package games.cascadia.components;

import core.CoreConstants.ComponentType;
import core.components.Component;

public class ScoringCard extends Component {
  private final WildlifeType wildlifeType;
  private final ScoringPattern pattern;

  public ScoringCard(int componentID, WildlifeType wildlifeType, ScoringPattern pattern) {
    super(ComponentType.CARD, "Cascadia Scoring Card", componentID);
    this.wildlifeType = wildlifeType;
    this.pattern = pattern;
  }

  public WildlifeType getWildlifeType() {
    return wildlifeType;
  }

  public ScoringPattern getPattern() {
    return pattern;
  }

  @Override
  public Component copy() {
    // Scoring cards are immutable, so safe to return a new instance with the same
    // ID
    return new ScoringCard(componentID, wildlifeType, pattern);
  }
}
