package games.cascadia.board;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import core.CoreConstants.ComponentType;
import core.components.Component;
import games.cascadia.components.WildlifeToken;

public class PlayerBoard extends Component {

  private final Map<HexCoord, PlacedHabitatTile> placedTiles;
  private final Map<HexCoord, WildlifeToken> wildlifeTokens;

  public PlayerBoard(int componentID) {
    super(ComponentType.BOARD, "Cascadia Player Board", componentID);
    this.placedTiles = new HashMap<>();
    this.wildlifeTokens = new HashMap<>();
  }

  private PlayerBoard(int componentID, Map<HexCoord, PlacedHabitatTile> placedTiles,
      Map<HexCoord, WildlifeToken> wildlifeTokens) {
    super(ComponentType.BOARD, "Cascadia Player Board", componentID);
    this.placedTiles = placedTiles;
    this.wildlifeTokens = wildlifeTokens;
  }

  public void placeWildlife(HexCoord coord, WildlifeToken token) {

    if (!placedTiles.containsKey(coord)) {
      throw new IllegalArgumentException(
          "Cannot place wildlife on empty hex: " + coord);
    }

    if (wildlifeTokens.containsKey(coord)) {
      throw new IllegalStateException(
          "Hex already has wildlife: " + coord);
    }

    wildlifeTokens.put(coord, token);
  }

  public Map<HexCoord, WildlifeToken> getWildlifeTokens() {
    return Collections.unmodifiableMap(wildlifeTokens);
  }

  public WildlifeToken getWildlifeTokenAt(HexCoord coord) {
    return wildlifeTokens.get(coord);
  }

  public boolean isEmpty() {
    return placedTiles.isEmpty();
  }

  public PlacedHabitatTile getTileAt(HexCoord coord) {
    return placedTiles.get(coord);
  }

  public void placeTile(HexCoord coord, PlacedHabitatTile tile) {
    placedTiles.put(coord, tile);
  }

  public Map<HexCoord, PlacedHabitatTile> getTiles() {
    return Collections.unmodifiableMap(placedTiles);
  }

  @Override
  public Component copy() {
    Map<HexCoord, PlacedHabitatTile> copyTiles = new HashMap<>();
    Map<HexCoord, WildlifeToken> copyWildlife = new HashMap<>();

    for (Map.Entry<HexCoord, WildlifeToken> entry : wildlifeTokens.entrySet()) {
      copyWildlife.put(entry.getKey(),
          (WildlifeToken) entry.getValue().copy());
    }

    for (Map.Entry<HexCoord, PlacedHabitatTile> entry : placedTiles.entrySet()) {
      copyTiles.put(entry.getKey(),
          (PlacedHabitatTile) entry.getValue().copy());
    }
    return new PlayerBoard(componentID, copyTiles, copyWildlife);
  }
}