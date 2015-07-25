class Target {
  
  // Position/size of target
  PVector position;
  float radius;
  color fashion;
  
  float wander_radius;
  float wander_distance;
  float target_jitter;
  
  // Initialisation
  Target( float r, color c) {
      radius = r;
      fashion = c;
  } 
  
  void randomPosition() {
    position = randomPoint();
  }
  
  void update() {}
 
  // Draw the target
  void draw() {
     pushStyle();
     fill(fashion);
     ellipse(position.x, position.y, 2*radius, 2*radius);
     popStyle();
  } 
}
