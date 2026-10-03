package games.cascadia.components;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import core.CoreConstants.ComponentType;
import core.components.Component;

public class HabitatTile extends Component {

  // Terrain types (for habitat corridor scoring)
  private final Set<HabitatType> habitats;

  // Wildlife icons printed on the tile
  private final Set<WildlifeType> allowedWildlife;

  // Nature token bonus mark
  private final boolean hasNatureMark;

  // Placed wildlife (null if empty)
  private WildlifeToken wildlifeToken;

  private final List<HabitatType> baseEdgeHabitats;

  public HabitatTile(int componentID,
      Set<HabitatType> habitats,
      Set<WildlifeType> allowedWildlife,
      List<HabitatType> baseEdgeHabitats,
      boolean hasNatureMark) {

    super(ComponentType.BOARD_NODE, "Cascadia Habitat Tile", componentID);

    this.habitats = EnumSet.copyOf(habitats);
    this.allowedWildlife = EnumSet.copyOf(allowedWildlife);
    this.hasNatureMark = hasNatureMark;
    this.baseEdgeHabitats = baseEdgeHabitats;
    this.wildlifeToken = null;
  }

  // ---------- Getters ----------

  public HabitatType getBaseHabitatOnEdge(int edge) {
    return baseEdgeHabitats.get(edge);
  }

  public Set<HabitatType> getHabitats() {
    return habitats;
  }

  public Set<WildlifeType> getAllowedWildlife() {
    return allowedWildlife;
  }

  public boolean hasNatureMark() {
    return hasNatureMark;
  }

  public boolean hasWildlife() {
    return wildlifeToken != null;
  }

  public WildlifeToken getWildlifeToken() {
    return wildlifeToken;
  }

  // ---------- Mutators ----------

  public void placeWildlife(WildlifeToken token) {
    this.wildlifeToken = token;
  }

  // ---------- TAG copy ----------

  @Override
  public Component copy() {
    HabitatTile copy = new HabitatTile(
        componentID,
        habitats,
        allowedWildlife,
        baseEdgeHabitats,
        hasNatureMark);

    if (wildlifeToken != null) {
      copy.placeWildlife((WildlifeToken) wildlifeToken.copy());
    }

    return copy;
  }
}