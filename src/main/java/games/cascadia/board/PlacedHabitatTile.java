package games.cascadia.board;

import java.util.Set;

import games.cascadia.components.HabitatTile;
import games.cascadia.components.HabitatType;
import games.cascadia.components.WildlifeType;

public class PlacedHabitatTile {

  public final HabitatTile tile;
  private final int rotation; // 0–5

  public PlacedHabitatTile(HabitatTile tile, int rotation) {
    this.tile = tile;
    this.rotation = ((rotation % 6) + 6) % 6;
  }

  public HabitatType getHabitatOnWorldEdge(int worldEdge) {
    int baseEdge = (worldEdge - rotation + 6) % 6;
    return tile.getBaseHabitatOnEdge(baseEdge);
  }

  public boolean hasNatureMark() {
    return tile.hasNatureMark();
  }

  public PlacedHabitatTile copy() {
    return new PlacedHabitatTile((HabitatTile) tile.copy(), rotation);
  }

  public Set<WildlifeType> getAllowedWildlife() {
    return tile.getAllowedWildlife();
  }

}
