package games.cascadia.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cascadia.CascadiaGameState;
import games.cascadia.GamePhase;
import games.cascadia.board.HexCoord;
import games.cascadia.board.PlacedHabitatTile;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.HabitatTile;
import games.cascadia.market.MarketPair;

public class PlaceTileAction extends AbstractAction {

  private final HexCoord coord;
  private final int rotation;

  public PlaceTileAction(HexCoord coord, int rotation) {
    this.coord = coord;
    this.rotation = rotation;
  }

  @Override
  public PlaceTileAction copy() {
    return new PlaceTileAction(coord, rotation);
  }

  @Override
  public String getString(core.AbstractGameState gameState) {
    return toString();
  }

  public int getRotation() {
    return rotation;
  }

  @Override
  public boolean execute(AbstractGameState gameState) {

    CascadiaGameState state = (CascadiaGameState) gameState;
    PlayerBoard board = state.getPlayerBoard(state.getCurrentPlayer());
    int playerId = state.getCurrentPlayer();

    MarketPair pair = state.getDraftedPair();
    HabitatTile tileDef = pair.getHabitatTile();

    if (board.getTileAt(coord) != null) {
      throw new IllegalStateException("Tile already exists at " + coord);
    }

    PlacedHabitatTile placed = new PlacedHabitatTile(tileDef, rotation);

    state.getPlayerBoard(playerId).placeTile(coord, placed);

    state.setLastPlacedTileCoord(coord);

    state.setPhase(GamePhase.PLACE_WILDLIFE);

    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PlaceTileAction))
      return false;
    PlaceTileAction other = (PlaceTileAction) o;
    return coord.equals(other.coord);
  }

  @Override
  public int hashCode() {
    return coord.hashCode();
  }

  @Override
  public String toString() {
    return "PlaceTile@" + coord.q + "," + coord.r;
  }
}