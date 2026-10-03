package games.cascadia.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cascadia.CascadiaGameState;
import games.cascadia.GamePhase;
import games.cascadia.board.HexCoord;
import games.cascadia.board.PlacedHabitatTile;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.WildlifeToken;

public class PlaceWildlifeAction extends AbstractAction {

  private final HexCoord coord;
  private final WildlifeToken token;

  public PlaceWildlifeAction(HexCoord coord, WildlifeToken token) {
    this.coord = coord;
    this.token = token;
  }

  @Override
  public PlaceWildlifeAction copy() {
    return new PlaceWildlifeAction(coord, token);
  }

  @Override
  public String getString(core.AbstractGameState gameState) {
    if (coord != null) {
      return toString();
    } else {
      return "SkipWildlifePlacement";
    }
  }

  @Override
  public boolean execute(AbstractGameState gameState) {

    CascadiaGameState state = (CascadiaGameState) gameState;
    int playerId = state.getCurrentPlayer();
    
    PlacedHabitatTile placedTile;

    PlayerBoard board = state.getPlayerBoard(playerId);

    if (coord != null) {
      placedTile = state.getPlayerBoard(playerId).getTileAt(coord);
      board.placeWildlife(coord, token);
  
      // Award nature token if tile has mark
      if (placedTile.hasNatureMark()) {
        state.addNatureToken(playerId);
      }
    }

    // Advance player
    int nextPlayer = (playerId + 1) % state.getNPlayers();
    state.setTurnOwner(nextPlayer);
    state.setDraftedPair(null);

    // Reset phase
    state.setPhase(GamePhase.DRAFT);

    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PlaceWildlifeAction))
      return false;
    PlaceWildlifeAction other = (PlaceWildlifeAction) o;
    if (coord == null && other.coord == null)
      return true;
    if (coord == null || other.coord == null)
      return false;
    return coord.equals(other.coord);
  }

  @Override
  public int hashCode() {
    return coord != null ? coord.hashCode() : 0;
  }

  @Override
  public String toString() {
    if (coord != null) {
      return "PlaceWildlife@" + coord.q + "," + coord.r;
    } else {
      return "SkipWildlifePlacement";
    }
  }
}