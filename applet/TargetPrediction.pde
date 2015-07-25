class TargetPrediction extends Target {
  
  Agent agent1; // he will flee/seek the target
  Agent agent2; // he is seeken/fleed from

  // Initialisation
  TargetPrediction(float r, color c, Agent a1, Agent a2) {
    super(r,c);
    agent1 = a1;
    agent2 = a2;
    
    position = get_predicted_position();

  } 
 
  void update() {
    position = get_predicted_position();

  }
  
  PVector get_predicted_position() {
    float t_pred = min(get_time_to_target(),agent1.t_lim);
    return PVector.add( agent2.position, PVector.mult(agent2.velocity,t_pred));
  }
  
  float get_time_to_target() {
    PVector pp = PVector.sub(agent1.position, agent2.position);
    return agent1.k_pred * pp.mag() / agent1.velocity.mag();
  }
}
