package games.cascadia.market;

import games.cascadia.components.HabitatTile;
import games.cascadia.components.WildlifeToken;

public class MarketPair {

  private final HabitatTile habitatTile;
  private final WildlifeToken wildlifeToken;

  public MarketPair(HabitatTile habitatTile, WildlifeToken wildlifeToken) {
    if (habitatTile == null || wildlifeToken == null) {
      throw new IllegalArgumentException("MarketPair cannot contain null");
    }
    this.habitatTile = habitatTile;
    this.wildlifeToken = wildlifeToken;
  }

  public HabitatTile getHabitatTile() {
    return habitatTile;
  }

  public WildlifeToken getWildlifeToken() {
    return wildlifeToken;
  }
}