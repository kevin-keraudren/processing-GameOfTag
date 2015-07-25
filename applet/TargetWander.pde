class TargetWander extends Target {
  
  Agent agent;
    
  PVector wander_center;
  PVector wander_target;

  // Initialisation
  TargetWander( float r, color c, float wr, float tj, float dw, Agent a) {
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
  
  void update() {
    wander_center = get_wander_center();
    
    PVector random_vector = new PVector(random(-1,1), random(-1,1));
    random_vector.mult(target_jitter);
    wander_target.add(random_vector);
    wander_target.normalize();
    wander_target.mult(wander_radius);
    
    position = wander_center.get();
    position.add(wander_target);
  }
  
  PVector get_wander_center() {
    PVector wc = new PVector();
    wc = agent.velocity.get();
    wc.normalize();
    wc.mult(wander_distance);
    wc.add(agent.position);
    return wc;
  }
    
  // Draw the target
  void draw() {
      pushStyle();
      noFill();
      ellipse(wander_center.x, wander_center.y, 2*wander_radius, 2*wander_radius);
      popStyle();
      super.draw();
  } 
}
