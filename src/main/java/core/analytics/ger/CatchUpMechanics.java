package core.analytics.ger;

public class CatchUpMechanics {

  /** Peak Gini coefficient of scores before 75% of game completion. */
  public double peakGiniBefore75;

  /** Turn (as fraction of total) at which peak Gini occurs. */
  public double peakGiniTiming;

  /** Mean Gini coefficient across all turns. */
  public double meanGini;

  /** Trailing player's win probability when at >= 25% resource deficit. */
  public double winCurveResilience;
}
