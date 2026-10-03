package core.analytics.ger;

public class PlayerAgency {

  // --- Agency Density (after redundancy pruning) ---

  /** Fraction of turns that are bookkeeping (bf <= 1, no real choice). */
  public double bookkeepingRedundancy;

  /**
   * Fraction of turns that are single-player determinism (consecutive same-player
   * turns).
   */
  public double singlePlayerDeterminism;

  /**
   * Fraction of turns where branching factor is high but score delta is zero
   * (flavor choices).
   */
  public double decisionMatrixRedundancy;

  /**
   * Agency Density: fraction of session spent making non-redundant, impactful
   * choices.
   */
  public double agencyDensity;

  // --- Skill-Luck Index ---

  /**
   * Skill Leverage (K): increase in win probability from optimal vs random play.
   */
  public double skillLeverage;

  /** Luck Leverage (L): variance in win probability caused by chance. */
  public double luckLeverage;

  /** Skill-Luck Index S = (K - L) / (K + L), range [-1, 1]. */
  public double skillLuckIndex;

  // --- Elo-based Skill Depth ---

  /**
   * Standard deviation of Elo ratings across agents wider = more skill-dependent.
   */
  public double eloStdDev;

}
