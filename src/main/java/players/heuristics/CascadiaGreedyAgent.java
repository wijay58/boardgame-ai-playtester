package players.heuristics;

import java.util.List;
import java.util.Map;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.cascadia.CascadiaGameState;
import games.cascadia.GamePhase;
import games.cascadia.board.HexCoord;
import games.cascadia.board.PlacedHabitatTile;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.HabitatType;
import games.cascadia.components.WildlifeToken;
import games.cascadia.components.WildlifeType;

public class CascadiaGreedyAgent extends AbstractPlayer {

  public CascadiaGreedyAgent() {
    super(null, "CascadiaGreedyAgent");
  }

  @Override
  public AbstractAction _getAction(
      AbstractGameState gameState,
      List<AbstractAction> actions) {

    CascadiaGameState state = (CascadiaGameState) gameState;
    int playerId = state.getCurrentPlayer();
    boolean isDraftPhase = (state.getPhase() == GamePhase.DRAFT);

    AbstractAction bestAction = null;
    double bestValue = Double.NEGATIVE_INFINITY;

    double baseValue = heuristic(state, playerId);

    for (AbstractAction action : actions) {

      CascadiaGameState copy = (CascadiaGameState) state.copy();

      action.execute(copy);

      if (isDraftPhase) {
        simulateOneTurn(copy, playerId);
      }

      double value = heuristic(copy, playerId) - baseValue;

      if (value > bestValue ||
          (value == bestValue && rnd.nextBoolean())) {
        bestValue = value;
        bestAction = action;
      }
    }

    return bestAction;
  }

  private double heuristic(CascadiaGameState state, int playerId) {
    int baseScore = state.computeScore(playerId);
    int corridorBonus = computeCorridorPotential(state, playerId);
    int wildlifeLosePenalty = wildlifePlacementPotential(state, playerId);

    return baseScore + (0.2 * corridorBonus) + wildlifeLosePenalty;
  }

  private void simulateOneTurn(CascadiaGameState state, int playerId) {

    // PLACE_TILE
    if (state.getPhase() == GamePhase.PLACE_TILE) {
      List<AbstractAction> tileActions = state.getAvailableActions(); // thin wrapper

      AbstractAction bestTile = chooseBest(tileActions, state, playerId);

      if (bestTile != null) {
        bestTile.execute(state);
      }
    }

    // PLACE_WILDLIFE
    if (state.getPhase() == GamePhase.PLACE_WILDLIFE) {
      List<AbstractAction> wildlifeActions = state.getAvailableActions();

      AbstractAction bestWildlife = chooseBest(wildlifeActions, state, playerId);

      if (bestWildlife != null) {
        bestWildlife.execute(state);
      }
    }
  }

  private AbstractAction chooseBest(
      List<AbstractAction> actions,
      CascadiaGameState state,
      int playerId) {

    AbstractAction best = null;
    double bestValue = Double.NEGATIVE_INFINITY;

    for (AbstractAction a : actions) {
      CascadiaGameState copy = (CascadiaGameState) state.copy();

      a.execute(copy);
      double v = heuristic(copy, playerId);

      if (v > bestValue) {
        bestValue = v;
        best = a;
      }
    }

    return best;
  }

  private int computeCorridorPotential(
      CascadiaGameState state,
      int playerId) {

    HexCoord last = state.getLastPlacedTileCoord();
    if (last == null)
      return 0;

    PlayerBoard board = state.getPlayerBoard(playerId);
    PlacedHabitatTile tile = board.getTileAt(last);
    if (tile == null)
      return 0;

    int bonus = 0;

    for (HabitatType habitat : HabitatType.values()) {
      bonus += habitatEdgeConnectivity(board, last, tile, habitat);
    }

    return bonus;
  }

  private int habitatEdgeConnectivity(
      PlayerBoard board,
      HexCoord coord,
      PlacedHabitatTile tile,
      HabitatType habitat) {

    int connections = 0;

    for (int edge = 0; edge < 6; edge++) {

      if (tile.getHabitatOnWorldEdge(edge) == habitat) {
        HexCoord neighbor = coord.add(HexCoord.EDGE_TO_DIRECTION[edge]);

        PlacedHabitatTile other = board.getTileAt(neighbor);
        if (other != null) {
          int opp = HexCoord.oppositeEdge(edge);

          if (other.getHabitatOnWorldEdge(opp) == habitat) {
            connections++;
          }
        }
      }
    }

    if (connections == 0)
      return 0;
    else if (connections == 1)
      return 2;
    else if (connections == 2)
      return 3;
    else if (connections == 3)
      return 4;
    else
      return 1;
  }

  private int wildlifePlacementPotential(
      CascadiaGameState state,
      int playerId) {

    int value = 0;

    // Check if a pair has been drafted yet
    if (state.getDraftedPair() == null) {
      return 0; // No penalty if no pair drafted yet
    }

    WildlifeToken token = state.getDraftedPair().getWildlifeToken();

    PlayerBoard board = state.getPlayerBoard(playerId);

    if (!canPlaceWildlife(board, token)) {
      value = -15; // strong penalty
    }

    return value;
  }

  private boolean canPlaceWildlife(
      PlayerBoard board,
      WildlifeToken token) {

    WildlifeType type = token.getWildlifeType();

    for (Map.Entry<HexCoord, PlacedHabitatTile> entry : board.getTiles().entrySet()) {

      HexCoord coord = entry.getKey();
      PlacedHabitatTile tile = entry.getValue();

      if (board.getWildlifeTokenAt(coord) != null)
        continue;

      if (tile.getAllowedWildlife().contains(type)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public AbstractPlayer copy() {
    return new CascadiaGreedyAgent();
  }
}