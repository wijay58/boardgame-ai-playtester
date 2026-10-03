package core.analytics.ger;

public class PlayerInteraction {

  // --- Elemental Framework (Social Signature) ---

  /**
   * Earth (Mutualism): score correlation between players. Positive = mutual
   * growth.
   */
  public double earthSignature;

  /**
   * Water (Displacement): action contention how often branching factor drops
   * after opponent turns.
   */
  public double waterSignature;

  /**
   * Fire (Conflict): negative utility transfer when one player gains, another
   * loses.
   */
  public double fireSignature;

  /**
   * Wind (Chaos): turn-to-turn state variance unpredictable disruption to all
   * players.
   */
  public double windSignature;

  /**
   * Light (Altruism): fraction of turns where acting player's score doesn't
   * increase but others' do.
   */
  public double lightSignature;

  /** Dominant interaction element (highest signature). */
  public String dominantElement;

  // --- Strategic Interdependency ---

  /**
   * Response Sensitivity: % of turns where action distribution shifts
   * significantly after opponent play.
   */
  public double responseSensitivity;

  /**
   * Interaction Intensity: overall level of player-to-player influence (0 =
   * solitaire, 1 = constant).
   */
  public double interactionIntensity;
}
