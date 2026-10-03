package core.analytics.ger;

public class RandomnessManagement {

  // --- Input Randomness ---

  /**
   * Mean branching factor after input randomness how many viable actions remain
   * post-draw.
   */
  public double inputAdaptationBreadth;

  /**
   * Std dev of branching factor consistency of decision space across random
   * inputs.
   */
  public double inputBreadthVariance;

  /**
   * Fraction of turns where branching factor remains above the median despite
   * randomness.
   */
  public double adaptationPotential;

  // --- Output Randomness ---

  /** Mean absolute score delta per turn baseline volatility. */
  public double meanScoreDelta;

  /** Max single-turn score swing observed worst-case output randomness impact. */
  public double maxScoreSwing;

  /**
   * Realization Variance: std dev of per-turn score deltas how "swingy" outcomes
   * are.
   */
  public double realizationVariance;

  /** Win probability stability: how often the leader changes turn-to-turn. */
  public double leaderChangeRate;

  /**
   * Brittleness Index: fraction of games where a single large swing decided the
   * winner.
   */
  public double brittlenessIndex;
}
