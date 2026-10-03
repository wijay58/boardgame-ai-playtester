package games.cascadia.components;

import core.CoreConstants.ComponentType;
import core.components.Component;

public class WildlifeToken extends Component {

  private final WildlifeType wildlifeType;

  public WildlifeToken(int componentID, WildlifeType wildlifeType) {
    super(ComponentType.TOKEN, "Cascadia Wildlife Token", componentID);
    this.wildlifeType = wildlifeType;
  }

  public WildlifeType getWildlifeType() {
    return wildlifeType;
  }

  @Override
  public Component copy() {
    return new WildlifeToken(componentID, wildlifeType);
  }
}