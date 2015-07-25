/*
 * The Flee Steering Behaviour
 */
class Flee extends Steering {
  
  // Initialisation
  Flee(Agent a, Target t) {
      super(a,t);
  }
  
  PVector calculateRawForce() {
    // Calculate Flee Force
    PVector flee = PVector.sub(agent.position, target.position);
    flee.normalize();
    flee.mult(agent.maxSpeed);
    flee.sub(agent.velocity);
    return flee;
  }
}
