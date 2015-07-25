/*
 * A Steered Agent
 */
class Agent {
  
  // Body  
  float mass;
  float radius;
  color fashion;

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
  Agent(float m, float r, PVector p, color c) {
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
  void update() {
    
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
  void draw() {
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
  void toggleAnnotate() {
    annotate = ! annotate;
  }
  
  /*
   * Change parameters
   */
  // Vary maximum speed
  void incMaxSpeed() {    
    maxSpeed++;
  }
  void decMaxSpeed() {    
    maxSpeed--;
    if (maxSpeed < 1) maxSpeed = 1;
  }

  // Vary maximum force  
  void incMaxForce() {    
    maxForce++;
  }
  void decMaxForce() {    
    maxForce--;
    if (maxForce < 1) maxForce = 1;
  }

  // Vary mass
  void incMass() {    
    mass++;
  }
  void decMass() {    
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
  PVector toLocalSpace(PVector vec) {
    PVector trans = PVector.sub(vec, position);
    PVector local =  new PVector(trans.dot(forward), trans.dot(side));
    return local;
  }
  
  PVector toGlobalSpace(PVector vec) {
    PVector global = PVector.mult(forward, vec.x);
    global.add(PVector.mult(side, vec.y)); 
    global.add(position);    
    
    return global;
  }
 
  Target toTarget() {
    return toTarget;
  }
 
  TargetPrediction predict(Agent a) {
    TargetPrediction pred = new TargetPrediction(a.radius, a.fashion, this, a);
    return pred;
  }
   
  void playTag() {
    
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
      if (distance <= 2.5*radius ) {
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
      avoid.weight = 1.5;
      behaviours.add(avoid);
      
      // if we are far from theTagged, we can wander a bit
      // and seek the center even more
      if (distanceTag > safeDistance) {
        seekCenter.weight = 2;
        Seek wander = new Seek(this, wander_target);
        wander.weight = 0.5;
        behaviours.add(wander);
        if (showTargets) wander_target.draw();
      }
    }    
  }
  
  void avoid_walls(float weight) {
    
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
  
  Agent get_nearest_agent() {
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
  
  Agent whos_tagged() {
    Agent a = this;
    for (int i=0; i < agents.size(); i++) {
      a = (Agent)agents.get(i);
      if (a == this) continue;
      if (a.isTagged) break;
    }
    if (a == this) die("**ERROR**: Did not find theTagged\n"); 
    return a;
  }  
  
  void get_tired() {
    if (random(-1,10) < 0) {
      velocity.x--;
      velocity.y--;      
    }
  }
  
  void reflect_velocity(PVector a) {
    PVector axe1 = a.get(); // get a deep copy
    axe1.normalize();
    PVector axe2 = new PVector(-axe1.y,axe1.x); // othonormal
    PVector v = velocity.get();
    velocity = new PVector(velocity.dot(axe1), -velocity.dot(axe2));   
  }
  
}
  
  
 
