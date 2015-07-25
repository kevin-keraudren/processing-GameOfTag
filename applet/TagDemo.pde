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
void setup() {
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
PVector randomPoint() {
  return new PVector(random(width), random(height));
}
color randomColor() {
  return color(random(256), random(256), random(256)); 
}

// The draw loop
void draw() {
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
void drawInfoPanel() {
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
void keyPressed() {
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
  
void annotateAgents() {
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.annotate = ! a.annotate;
    }
}

void toggleTargets(){
  showTargets = ! showTargets;
  if (showTargets) showTargets();
  else hideTargets();
}

void showTargets() {
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.showTargets = true;
    }
}
void hideTargets() {
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.showTargets = false;
    }
}
void spread(Boolean spread_value) {
    for (int i = 0; i < agents.size(); i++) {
      Agent a = (Agent)agents.get(i);
      a.spreadable = spread_value;
   }
}

void toggleSpread(){
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


