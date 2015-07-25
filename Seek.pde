/*
 * The Seek Steering Behaviour
 */
class Seek extends Steering {
  
  // Initialisation
  Seek( Agent a, Target t) {
      super(a, t);
  }
  
  PVector calculateRawForce() {
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
