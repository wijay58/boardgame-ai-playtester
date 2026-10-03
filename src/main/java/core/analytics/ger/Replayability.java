package core.analytics.ger;

public class Replayability {

  /** Shannon Diversity Index H = -Σ p_i * ln(p_i) over outcome categories. */
  public double shannonDiversityIndex;

  /** Number of unique outcome categories (richness R). */
  public int outcomeRichness;

  /** Evenness J = H / ln(R), normalized to [0,1]. 1 = perfectly even outcomes. */
  public double outcomeEvenness;

  /** Zahl's bias-corrected estimator: exp(H) with small-sample correction. */
  public double zahlEstimator;

  /**
   * Mean pairwise Hamming distance between action sequences across simulations.
   */
  public double meanHammingDistance;

  /** Normalized Hamming distance [0,1] fraction of positions that differ. */
  public double normalizedHammingDistance;

  /**
   * Entropy collapse rate: how quickly decision entropy drops during a session.
   */
  public double entropyCollapseRate;

  /** Fitted parameter a in R = -a * exp(H) + b (entropy-performance decay). */
  public double entropyPerformanceA;

  /** Fitted parameter b in R = -a * exp(H) + b (theoretical max reward). */
  public double entropyPerformanceB;
}
