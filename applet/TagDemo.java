import processing.core.*; 
import processing.xml.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class TagDemo extends PApplet {

/*
 *
 * The TagDemo sketch
 *
 */

// Agents (we need this as a GLOBAL variable)
ArrayList agents = new ArrayList();
boolean showTargets = false;

int nb_agents = 5;
float mass = 10;
float radius = 12;
  
boolean spread = false;
boolean GAME_OVER = false;
  
// Are we paused?
boolean pause;
// Is this information panel being displayed?
boolean showInfo;

// Initialisation
public void setup() {
  size(1000,600); // Large display window
  pause = false;
  showInfo = true;
  

  for (int i = 0; i < nb_agents; i++) {
    agents.add( new Agent( mass, radius, randomPoint(), randomColor() ) );
  }
  // tag one of them
  Agent theTagged = (Agent)agents.get(0);
  theTagged.isTagged = true;
  theTagged.letFlee = theTagged.letFlee0;
  
  smooth(); // Anti-aliasing on
}

// Pick a random point in the display window
public PVector randomPoint() {
  return new PVector(random(width), random(height));
}
public int randomColor() {
  return color(random(256), random(256), random(256)); 
}

// The draw loop
public void draw() {
  // Clear the display
  background(255); 
  
    spread(spread);
  
  // Move forward one step in steering simulation
  if (!pause) {
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.update();
    }
  }
  
  // Draw the actors
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.draw();
    }

  
  // Draw the information panel
  if (showInfo) {
    drawInfoPanel();
  }
  
  if (GAME_OVER) {
    pushStyle();
    textAlign(CENTER);
    textSize(50);
    fill(255,0,0);
    text("GAME OVER\nPress 'r' to restart or 'q' to quit", width/2,height/2);
    popStyle();
  }
}
  
// Draw the information panel!
public void drawInfoPanel() {
  pushStyle(); // Push current drawing style onto stack
  fill(0);
  
  String buffer1 = "1 : toggle display\n" 
                 + "2 : toggle annotation\n"
                 + "3 : show/hide the targets\n"
                 + "'r' : change the rules\n"
                 + "'+' : add one agent\n"
                 + "'-' : remove one agent\n"
                 + "Space : play/pause\n"
                 + "'q' : exit\n";
                                  
  // Top left
  textSize(14);
  text(buffer1, 10, 20);
  
  // Bottom left
  textSize(30);
  text(nb_agents + " agents", 10, height - 20);
  
  popStyle(); // Retrieve previous drawing style
}

/*
 * Input handlers
 */

// Key pressed (either lower or upper case)
public void keyPressed() {
  String lkey = str(key).toLowerCase();
  switch (lkey.charAt(0)) {
    case ' ': pause = !pause; break;
    case '1': showInfo = !showInfo; break;
    case '2': annotateAgents(); break;
    case '3': toggleTargets(); break;
    case 'r': toggleSpread(); break;
    case 'q': exit(); break;
    case '+': 
      agents.add( new Agent( mass, radius, randomPoint(), randomColor() ) );
      nb_agents++;
      break;
    case '-':
      if (nb_agents == 2) break;
      for (int i=0; i < agents.size(); i++) {
        Agent a = (Agent)agents.get(i);
        if (a.isTagged) continue;
        agents.remove(i);
        break;
      }
      nb_agents--;
      break;    
    
  }
}
  
public void annotateAgents() {
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.annotate = ! a.annotate;
    }
}

public void toggleTargets(){
  showTargets = ! showTargets;
  if (showTargets) showTargets();
  else hideTargets();
}

public void showTargets() {
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.showTargets = true;
    }
}
public void hideTargets() {
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.showTargets = false;
    }
}
public void spread(Boolean spread_value) {
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.spreadable = spread_value;
   }
}

public void toggleSpread(){
  spread = ! spread;
   if (! spread) {
     GAME_OVER = false;
  for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.isTagged = false;
   }
     // tag one of them
  Agent theTagged = (Agent)agents.get(0);
  theTagged.isTagged = true;
  theTagged.letFlee = theTagged.letFlee0;
   }
}


/*
 * A Steered Agent
 */
class Agent {
  
  // Body  
  float mass;
  float radius;
  int fashion;

  // Physics
  PVector position;    
  PVector velocity;
  PVector acceleration;
  PVector force;
  
  // Limits
  float maxSpeed;
  float maxForce;
  
  // toTarget()
  Target toTarget;
  
  // TargetPrediction
  float k_pred;
  float t_lim;
  
   boolean isTagged;
   int letFlee;
   int letFlee0;
   TargetWander wander_target;
   float safeDistance;
   boolean spreadable;

  // Unit vector in direction agent is facing
  PVector forward; 
  // Unit vector orthogonal to forward, to the right of the agent
  PVector side;
  
  // A list of steering behaviours  
  ArrayList behaviours;
  int active_behaviour;
  
  // Should we draw steering annotations on agent?
  // e.g. draw the force vector
  boolean annotate;
  boolean showTargets;

  // Initialisation
  Agent(float m, float r, PVector p, int c) {
    mass = m;
    radius = r;
    position = p;
    fashion = c;
  
    // Agents starts at rest
    velocity = new PVector(0,0);
    acceleration = new PVector(0,0);
    force = new PVector(0,0);
    
    // Some arbitary initial limits
    maxSpeed = 5;
    maxForce = 10;
   
    // Because velocity is zero vector 
    forward = new PVector(0,0);
    side = new PVector(0,0);
    
    // Any empty list of steering behaviours
    behaviours = new ArrayList();
    active_behaviour = 0;
    
    // Don't draw annotations
    annotate = false;
    showTargets = false;
    
    // toTarget()
    toTarget = new Target(radius, fashion);
    toTarget.position = position;
    
    // TargetPrediction
    k_pred = 10;
    t_lim = 10;
    
    isTagged = false;
    letFlee = 0;
    letFlee0 = 100;
    safeDistance = 200;
    spreadable = false;
    
    // TargetWander(radius, color, wander_radius, target_jitter, wander_distance, agent)
    wander_target = new TargetWander(5, color(255,0,0), 60, 50, 50, this);

  }  
  
  // Agent simulation step
  public void update() {
    
    wander_target.update(); // so that it's ready when needed
    playTag();
    
    // Sum the list of steering forces
    PVector sf = new PVector(0,0);
    for (int i = 0; i < behaviours.size(); i++) {
       Steering sb = (Steering) behaviours.get(i);
       PVector sfi = sb.getForce();
       sf.add(sfi); 
    }
    // Trim the steering force
    if (maxForce > 0) sf.limit(maxForce);    
    force = sf;
    
    // Physics update
    acceleration = PVector.div(force, mass);
    velocity.add(acceleration);
    if (maxSpeed > 0) velocity.limit(maxSpeed);
    position.add(velocity);
    
    // Deal with vertical boundaries using reflection laws
    if (position.x <= 0) {
      position.x = 0;
      velocity.x = - velocity.x;
    } else if (position.x >= width) {
      position.x = width;
      velocity.x = - velocity.x;
    }

    // Deal with horizontal boundaries using reflection laws   
    if (position.y <= 0) {
      position.y = 0;
      velocity.y = - velocity.y;
    } else if (position.y >= height) {
      position.y = height;
      velocity.y = - velocity.y;
    }
    
    // no overlapping with other agents
    for (int i=0; i < agents.size(); i++) {
      Agent other = (Agent)agents.get(i);
        if (other == this) continue;
        if ( PVector.dist(position,other.position)< 2*radius) {
          PVector fix = PVector.sub(position,other.position);
          fix.div(2);
          position.add(fix);
          reflect_velocity(fix);
          other.position.sub(fix);
          other.reflect_velocity(PVector.mult(fix,-1));
        }
    }
        
    // Calculate forward and side vectors
    forward.x = velocity.x;
    forward.y = velocity.y;
    forward.normalize();
    side.x = -forward.y;
    side.y = forward.x;
    
    toTarget.position = position;
    if (isTagged) get_tired();
  }
  
  
  // Draw agent etc.
  public void draw() {
    pushStyle();
    noFill();

    // Draw the agent
    //   Draw circle
    stroke(fashion);
    float d = radius * 2;
    if (isTagged) {
      if (spreadable) letFlee = 0;
      fill(fashion);
      float d2 = d*(letFlee0-letFlee) / letFlee0;
      ellipse(position.x, position.y, d2, d2);
      noFill();
    }
    ellipse(position.x, position.y, d, d);
    //   Draw radial line in forward direction
    PVector heading = PVector.mult(forward, radius);
    heading.add(position);
    line(position.x, position.y, heading.x, heading.y);
    popStyle();
    
    // Draw force vector if required
    if (annotate) {
      pushStyle();
      stroke(204, 102, 0);
      // Scale the force vector x10 so it's visible
      float forceX = position.x + (10 * force.x);
      float forceY = position.y + (10 * force.y);
      line(position.x, position.y, forceX, forceY);
      popStyle();
    }
  }
  
  // Toggle annotations
  public void toggleAnnotate() {
    annotate = ! annotate;
  }
  
  /*
   * Change parameters
   */
  // Vary maximum speed
  public void incMaxSpeed() {    
    maxSpeed++;
  }
  public void decMaxSpeed() {    
    maxSpeed--;
    if (maxSpeed < 1) maxSpeed = 1;
  }

  // Vary maximum force  
  public void incMaxForce() {    
    maxForce++;
  }
  public void decMaxForce() {    
    maxForce--;
    if (maxForce < 1) maxForce = 1;
  }

  // Vary mass
  public void incMass() {    
    mass++;
  }
  public void decMass() {    
    mass--;
    if (mass < 1) mass = 1;
  }
  
  /*
   * Translate between global space and the agent's local space
   *
   * Global: Processing's default co-ordinate system.
   * Local: co-ordinate system with agent at the origin, facing along the
   * x-axis, with the y-axis extending to it's right.
   */ 
  public PVector toLocalSpace(PVector vec) {
    PVector trans = PVector.sub(vec, position);
    PVector local =  new PVector(trans.dot(forward), trans.dot(side));
    return local;
  }
  
  public PVector toGlobalSpace(PVector vec) {
    PVector global = PVector.mult(forward, vec.x);
    global.add(PVector.mult(side, vec.y)); 
    global.add(position);    
    
    return global;
  }
 
  public Target toTarget() {
    return toTarget;
  }
 
  public TargetPrediction predict(Agent a) {
    TargetPrediction pred = new TargetPrediction(a.radius, a.fashion, this, a);
    return pred;
  }
   
  public void playTag() {
    
    // Start from a new list of behaviours
    behaviours.clear();
    
    if (isTagged) {
      
      // who's the nearest ?
      Agent a = get_nearest_agent();   
      
      // I've just been tagged
      if (! spreadable){
      if (letFlee > 0) {
        letFlee--;
        
        // Let's wander for a while
        Seek wander = new Seek(this, wander_target);
        behaviours.add(wander);
        if (showTargets) wander_target.draw();
        
        // But do not step on the old theTagged
        Flee avoid = new Flee(this,a.toTarget());      
        behaviours.add(avoid);
        
        // And do not get stuck in a wall
        avoid_walls(1);
        
        return;
      }
      }    
      
      // try to give tag
      float distance = PVector.dist(this.position,a.position);
      if (distance <= 2.5f*radius ) {
        if (!spreadable) isTagged = false;
        velocity = new PVector(0,0); // prevents overlaping
        a.isTagged = true;
        //if (spreadable) a.letFlee = 0;
        //else a.letFlee = a.letFlee0;
        a.letFlee = a.letFlee0;
        a.velocity = new PVector(0,0); // prevents overlaping
        return;
      }

      TargetPrediction prediction_a = predict(a);
      Seek seek = new Seek(this, prediction_a);
      behaviours.add(seek);
      //if (showTargets) prediction_a.draw();
      
    } 
    else {
      
      // walls could trap us, so we avoid them
      avoid_walls(1);
      
      // who's theTagged ?
      Agent theTagged = whos_tagged(); 
      float distanceTag = PVector.dist(this.position,theTagged.position); 

      // flee from theTagged
      TargetPrediction prediction_theTagged = predict(theTagged);
      Flee flee = new Flee(this,prediction_theTagged);
      flee.weight = safeDistance / distanceTag;
      behaviours.add(flee);
      //if (showTargets) prediction_theTagged.draw();
      
      // Always seek the center : safer than the corners
      // unless theTagged is near the center
      // give a big radius to the center, as we stop seeking it
      // when we are on it
      Target center = new Target( 100, color(255,0,0));
      center.position = new PVector( width / 2, height / 2);
      Seek seekCenter = new Seek(this,center);
      if (showTargets) center.draw();
      float TagToCenter = PVector.dist( theTagged.position, center.position);
      if (TagToCenter > safeDistance) {
        seekCenter.weight = 1;
        behaviours.add(seekCenter);
      }
      
      // who's the nearest ?
      Agent nearest = get_nearest_agent(); 
      
      // it's dangerous to stay in a herd,
      // so flee from the nearest agent
      Flee avoid = new Flee(this,nearest.toTarget());
      avoid.weight = 1.5f;
      behaviours.add(avoid);
      
      // if we are far from theTagged, we can wander a bit
      // and seek the center even more
      if (distanceTag > safeDistance) {
        seekCenter.weight = 2;
        Seek wander = new Seek(this, wander_target);
        wander.weight = 0.5f;
        behaviours.add(wander);
        if (showTargets) wander_target.draw();
      }
    }    
  }
  
  public void avoid_walls(float weight) {
    
    float d_wall = 100;
    float wall_radius = 10;
    
    // Deal with vertical boundaries
    if (position.x <= d_wall) {
      Target wall = new Target(wall_radius,color(255,0,0));
      wall.position = new PVector(0,position.y);
      Flee avoid_wall = new Flee(this,wall);
      avoid_wall.weight = weight * (width - position.x) / width;  
      behaviours.add(avoid_wall);
      if (showTargets) wall.draw();
    } else if (position.x >= width - d_wall) {
      Target wall = new Target(wall_radius,color(255,0,0));
      wall.position = new PVector(width,position.y);
      Flee avoid_wall = new Flee(this,wall);
      avoid_wall.weight = weight * position.x / width;  
      behaviours.add(avoid_wall);
      if (showTargets) wall.draw();
    }

    // Deal with horizontal boundaries   
    if (position.y <= d_wall) {
      Target wall = new Target(wall_radius,color(255,0,0));
      wall.position = new PVector(position.x,0);
      Flee avoid_wall = new Flee(this,wall);
      avoid_wall.weight = weight * (height - position.y) / height;  
      behaviours.add(avoid_wall);
      if (showTargets) wall.draw();
    } else if (position.y >= height - d_wall) {
      Target wall = new Target(wall_radius,color(255,0,0));
      wall.position = new PVector(position.x,height);
      Flee avoid_wall = new Flee(this,wall);
      avoid_wall.weight = weight * position.y / height;  
      behaviours.add(avoid_wall);
      if (showTargets) wall.draw();
    }

  }
  
  public Agent get_nearest_agent() {
    float min_d = Float.POSITIVE_INFINITY;
    int nearest_agent = -1;
    for (int i=0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      if (a == this) continue;
      if (spreadable && isTagged && a.isTagged) continue; 
      float d = PVector.dist(position, a.position);
      if (d<min_d) {
        min_d = d;
        nearest_agent = i;
      }
    }
    if (nearest_agent == -1) {
      GAME_OVER = true;
      return this;
    }
    return (Agent)agents.get(nearest_agent);
  }
  
  public Agent whos_tagged() {
    Agent a = this;
    for (int i=0; i < agents.size(); i++) {
      a = (Agent)agents.get(i);
      if (a == this) continue;
      if (a.isTagged) break;
    }
    if (a == this) die("**ERROR**: Did not find theTagged\n"); 
    return a;
  }  
  
  public void get_tired() {
    if (random(-1,10) < 0) {
      velocity.x--;
      velocity.y--;      
    }
  }
  
  public void reflect_velocity(PVector a) {
    PVector axe1 = a.get(); // get a deep copy
    axe1.normalize();
    PVector axe2 = new PVector(-axe1.y,axe1.x); // othonormal
    PVector v = velocity.get();
    velocity = new PVector(velocity.dot(axe1), -velocity.dot(axe2));   
  }
  
}
  
  
 
/*
 * The Flee Steering Behaviour
 */
class Flee extends Steering {
  
  // Initialisation
  Flee(Agent a, Target t) {
      super(a,t);
  }
  
  public PVector calculateRawForce() {
    // Calculate Flee Force
    PVector flee = PVector.sub(agent.position, target.position);
    flee.normalize();
    flee.mult(agent.maxSpeed);
    flee.sub(agent.velocity);
    return flee;
  }
}
/*
 * The Seek Steering Behaviour
 */
class Seek extends Steering {
  
  // Initialisation
  Seek( Agent a, Target t) {
      super(a, t);
  }
  
  public PVector calculateRawForce() {
      // Check that agent's centre is not over target
      if (PVector.dist(target.position, agent.position) > target.radius) {
        // Calculate Seek Force
        PVector seek = PVector.sub(target.position, agent.position);
        seek.normalize();
        seek.mult(agent.maxSpeed);
        seek.sub(agent.velocity);
        return seek;

      } else  {
        // If agent's centre is over target stop seeking
        return new PVector(0,0); 
      }   
  }
}
/*
 * A Steering Behaviour
 */
abstract class Steering {
  String title;
  // The steered agent
  Agent agent;
  // Relative weight in sum of all steering forces
  float weight;
  // Is the behaviour switched on?
  boolean active;
  
  Target target;
  
  // Initialisation
  Steering( Agent a, Target t) {
   agent = a;
   target = t;
   weight = 1;
   active = true;
  }
  
  // Get the current steering force for this behaviour
  public PVector getForce() {
    if (active) {
       // Actual force is calculated in subclass
       PVector f = calculateRawForce();
       f.mult(weight); // Weight the result
       return f;
    } else {
       // No force if this behaviour is not active
       return new PVector(0,0); 
    }
  }

  // The actual force calculation
  public abstract PVector calculateRawForce();
}
class Target {
  
  // Position/size of target
  PVector position;
  float radius;
  int fashion;
  
  float wander_radius;
  float wander_distance;
  float target_jitter;
  
  // Initialisation
  Target( float r, int c) {
      radius = r;
      fashion = c;
  } 
  
  public void randomPosition() {
    position = randomPoint();
  }
  
  public void update() {}
 
  // Draw the target
  public void draw() {
     pushStyle();
     fill(fashion);
     ellipse(position.x, position.y, 2*radius, 2*radius);
     popStyle();
  } 
}
class TargetPrediction extends Target {
  
  Agent agent1; // he will flee/seek the target
  Agent agent2; // he is seeken/fleed from

  // Initialisation
  TargetPrediction(float r, int c, Agent a1, Agent a2) {
    super(r,c);
    agent1 = a1;
    agent2 = a2;
    
    position = get_predicted_position();

  } 
 
  public void update() {
    position = get_predicted_position();

  }
  
  public PVector get_predicted_position() {
    float t_pred = min(get_time_to_target(),agent1.t_lim);
    return PVector.add( agent2.position, PVector.mult(agent2.velocity,t_pred));
  }
  
  public float get_time_to_target() {
    PVector pp = PVector.sub(agent1.position, agent2.position);
    return agent1.k_pred * pp.mag() / agent1.velocity.mag();
  }
}
class TargetWander extends Target {
  
  Agent agent;
    
  PVector wander_center;
  PVector wander_target;

  // Initialisation
  TargetWander( float r, int c, float wr, float tj, float dw, Agent a) {
    super(r,c);
    wander_radius = wr;
    target_jitter = tj;
    wander_distance = dw;
    agent = a;

    wander_center = get_wander_center();
    wander_target = new PVector(wander_radius,0);
      
    position = wander_center.get();
    position.add(wander_target);  
  } 
  
  public void update() {
    wander_center = get_wander_center();
    
    PVector random_vector = new PVector(random(-1,1), random(-1,1));
    random_vector.mult(target_jitter);
    wander_target.add(random_vector);
    wander_target.normalize();
    wander_target.mult(wander_radius);
    
    position = wander_center.get();
    position.add(wander_target);
  }
  
  public PVector get_wander_center() {
    PVector wc = new PVector();
    wc = agent.velocity.get();
    wc.normalize();
    wc.mult(wander_distance);
    wc.add(agent.position);
    return wc;
  }
    
  // Draw the target
  public void draw() {
      pushStyle();
      noFill();
      ellipse(wander_center.x, wander_center.y, 2*wander_radius, 2*wander_radius);
      popStyle();
      super.draw();
  } 
}
  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#DFDFDF", "TagDemo" });
  }
}
