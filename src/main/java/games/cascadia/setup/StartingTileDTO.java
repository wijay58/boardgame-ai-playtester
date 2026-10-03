package games.cascadia.setup;

import java.util.List;

import games.cascadia.components.HabitatType;

public class StartingTileDTO {
  public int q;
  public int r;
  public List<String> habitats;
  public List<String> allowedWildlife;
  public boolean natureMark;
  public List<HabitatType> baseEdgeHabitats;
}