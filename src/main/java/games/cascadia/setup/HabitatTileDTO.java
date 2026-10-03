package games.cascadia.setup;

import java.util.List;

import games.cascadia.components.HabitatType;
import games.cascadia.components.WildlifeType;

public class HabitatTileDTO {
    public String id;
    public List<HabitatType> habitats;
    public List<WildlifeType> allowedWildlife;
    public boolean hasNatureMark;
    public List<HabitatType> baseEdgeHabitats;
}