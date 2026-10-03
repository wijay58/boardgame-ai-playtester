package core.analytics.ger;

public class StrategicDepth {

  // --- Complexity / Weight ---

  /** Mean number of legal moves available per decision point. */
  public double meanBranchingFactor;

  /**
   * Mean number of sub-actions per turn (Action Length). High values indicate
   * fiddliness.
   */
  public double meanActionLength;

  /** Fraction of actions that are administrative (forced, no real choice). */
  public double administrativeActionRatio;

  /** Fraction of known action types never used by any agent (Dead Actions). */
  public double deadActionRate;

  // --- Opacity ---

  /**
   * Correlation decay: how quickly an action's influence on score fades over
   * time.
   */
  public double opacityIndex;

  /** Mean temporal lag (in turns) before an action's score impact is realized. */
  public double meanRewardDelay;

  // --- Consequence ---

  /** Win-rate sensitivity: how much move selection affects outcomes. */
  public double consequenceSensitivity;

  // --- Breadth ---

  /**
   * Ratio of action types that meaningfully impact outcomes to total available
   * action types.
   */
  public double strategicBreadth;

  /**
   * Entropy of action choices across simulations how diverse top-agent play is.
   */
  public double moveDiversity;
}
