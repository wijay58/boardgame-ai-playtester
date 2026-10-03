package core.analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants.ComponentType;
import core.CoreConstants.GameResult;
import core.actions.AbstractAction;
import core.components.Component;
import core.observers.GameObserver;

public class TraceObserver implements GameObserver {

  private GameTrace trace = new GameTrace();
  private List<AbstractPlayer> players;
  private AbstractGameState prevState;
  private int actionsSinceLastTurn = 0;

  public TraceObserver() {
    this.players = null;
  }

  public TraceObserver(List<AbstractPlayer> players) {
    this.players = players;
  }

  @Override
  public void onGameStart(AbstractGameState state) {
    trace.nPlayers = state.getNPlayers();
    trace.gameName = state.getGameType().name();
    trace.seed = state.getGameParameters().getRandomSeed();

    trace.agents = new String[state.getNPlayers()];
    for (int p = 0; p < state.getNPlayers(); p++) {
      if (players != null && p < players.size()) {
        trace.agents[p] = players.get(p).toString();
      } else {
        trace.agents[p] = "Player" + p;
      }
    }
  }

  @Override
  public void onTurnEnd(AbstractGameState state, int player) {
    trace.turns++;

    double[] scores = new double[state.getNPlayers()];
    double[] interactionScores = new double[state.getNPlayers()];
    for (int p = 0; p < scores.length; p++) {
      scores[p] = state.getGameScore(p);
      interactionScores[p] = state.getInteractionScore(p);
    }

    trace.scoreTimeline.add(scores);
    trace.interactionScoreTimeline.add(interactionScores);

    trace.actionsPerTurn.add(actionsSinceLastTurn);
    actionsSinceLastTurn = 0;
    trace.playerPerTurn.add(player);

    double entropy = computeStateEntropy(state);
    trace.stateEntropyTimeline.add(entropy);

    trace.resourceStockTimeline.add(totalResourceCount(state));

    Map<String, Integer> snapshot = resourceSnapshot(state);
    for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
      trace.stockTimelineByType
          .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
          .add(entry.getValue());
    }
  }

  @Override
  public void onGameEnd(AbstractGameState state) {
    trace.finalScores = new double[state.getNPlayers()];
    for (int p = 0; p < trace.finalScores.length; p++)
      trace.finalScores[p] = state.getGameScore(p);

    trace.winner = -1;
    for (int p = 0; p < state.getNPlayers(); p++) {
      if (state.getPlayerResults()[p] == GameResult.WIN_GAME) {
        trace.winner = p;
        break;
      }
    }
    trace.terminalStateSignature = computeLayoutSignature(state);
    trace.terminalLayoutSignature = computeLayoutSignature(state);
    trace.terminalCountSignature = computeStateSignature(state);

    trace.resourceWasted = totalResourceCount(state);
    for (var e : resourceSnapshot(state).entrySet())
      trace.wastedByType.merge(e.getKey(), e.getValue(), Integer::sum);

    trace.softlocked = state.isGameOver() && trace.turns < 3;
  }

  private String computeStateSignature(AbstractGameState state) {
    Map<String, Integer> counts = new TreeMap<>();

    for (Component c : state.getAllComponents().getComponents()) {
      counts.merge(c.getType().toString(), 1, Integer::sum);
    }

    return counts.toString();
  }

  private double computeLayoutEntropy(AbstractGameState state) {

    // Measure spatial distribution entropy focusing on positioned elements
    Map<String, Integer> spatialDistribution = new HashMap<>();
    boolean hasSpatialComponents = false;

    for (Component c : state.getAllComponents().getComponents()) {
      // Capture GridBoard spatial distribution
      if (c instanceof core.components.GridBoard) {
        hasSpatialComponents = true;
        core.components.GridBoard board = (core.components.GridBoard) c;
        for (int y = 0; y < board.getHeight(); y++) {
          for (int x = 0; x < board.getWidth(); x++) {
            core.components.BoardNode node = board.getElement(x, y);
            if (node != null) {
              String key = "P" + c.getOwnerId() + "_" + x + "_" + y + "_" + node.getComponentName();
              spatialDistribution.merge(key, 1, Integer::sum);
            }
          }
        }
      }
      // Capture GraphBoard spatial distribution
      else if (c instanceof core.components.GraphBoard) {
        hasSpatialComponents = true;
        core.components.GraphBoard board = (core.components.GraphBoard) c;
        int nodeIdx = 0;
        for (core.components.BoardNode node : board.getBoardNodes()) {
          String key = "P" + c.getOwnerId() + "_N" + nodeIdx + "_" + node.getComponentName();
          spatialDistribution.merge(key, 1, Integer::sum);
          nodeIdx++;
        }
      }
      // Capture Area spatial distribution
      else if (c instanceof core.components.Area) {
        hasSpatialComponents = true;
        core.components.Area area = (core.components.Area) c;
        int idx = 0;
        for (Component comp : area.getComponents()) {
          String key = "P" + c.getOwnerId() + "_A" + idx + "_" + comp.getComponentName();
          spatialDistribution.merge(key, 1, Integer::sum);
          idx++;
        }
      }
      // Capture Deck spatial distribution (for card games)
      else if (c instanceof core.components.Deck) {
        hasSpatialComponents = true;
        core.components.Deck<?> deck = (core.components.Deck<?>) c;
        int idx = 0;
        for (Component comp : deck.getComponents()) {
          String key = "P" + c.getOwnerId() + "_D" + idx + "_" + comp.getComponentName();
          spatialDistribution.merge(key, 1, Integer::sum);
          idx++;
        }
      }
    }

    // Fallback: if no spatial components found, use component ownership
    // distribution
    if (!hasSpatialComponents || spatialDistribution.isEmpty()) {
      for (Component c : state.getAllComponents().getComponents()) {
        String key = "P" + c.getOwnerId() + "_" + c.getComponentName();
        spatialDistribution.merge(key, 1, Integer::sum);
      }
    }

    int total = spatialDistribution.values().stream().mapToInt(i -> i).sum();
    if (total == 0)
      return 0;

    double H = 0;
    for (int v : spatialDistribution.values()) {
      double p = v / (double) total;
      if (p > 0) {
        H -= p * Math.log(p);
      }
    }
    return H;
  }

  private double computeCountEntropy(AbstractGameState state) {
    // Measure component type distribution entropy
    Map<String, Integer> typeCounts = new HashMap<>();

    for (Component c : state.getAllComponents().getComponents()) {
      typeCounts.merge(c.getType().toString(), 1, Integer::sum);
    }

    int total = typeCounts.values().stream().mapToInt(i -> i).sum();
    if (total == 0)
      return 0;

    double H = 0;
    for (int v : typeCounts.values()) {
      double p = v / (double) total;
      if (p > 0) {
        H -= p * Math.log(p);
      }
    }
    return H;
  }

  private double computeStateEntropy(AbstractGameState state) {
    // Combine layout entropy (spatial distribution) and count entropy (type
    // distribution)
    double layoutEntropy = computeLayoutEntropy(state);
    double countEntropy = computeCountEntropy(state);
    return layoutEntropy + countEntropy;
  }

  private String computeLayoutSignature(AbstractGameState state) {
    // Capture actual spatial layout using TAG framework spatial components
    StringBuilder sb = new StringBuilder();

    for (Component c : state.getAllComponents().getComponents()) {
      // Capture GridBoard layouts
      if (c instanceof core.components.GridBoard board) {
        sb.append("GridBoard[").append(c.getOwnerId()).append("]:");
        for (int y = 0; y < board.getHeight(); y++) {
          for (int x = 0; x < board.getWidth(); x++) {
            core.components.BoardNode node = board.getElement(x, y);
            if (node != null) {
              sb.append("(").append(x).append(",").append(y).append(")=");
              sb.append(node.getComponentName()).append(":").append(node.getProperties());
              sb.append("|");
            }
          }
        }
      }
      // Capture GraphBoard layouts
      else if (c instanceof core.components.GraphBoard board) {
        sb.append("GraphBoard[").append(c.getOwnerId()).append("]:");
        for (core.components.BoardNode node : board.getBoardNodes()) {
          sb.append(node.getComponentName()).append(":").append(node.getProperties()).append("|");
        }
      }
      // Capture Area contents
      else if (c instanceof core.components.Area area) {
        sb.append("Area[").append(c.getOwnerId()).append("]:");
        for (Component comp : area.getComponents()) {
          sb.append(comp.getComponentName()).append(":").append(comp.getType()).append("|");
        }
      }
      // Capture Deck contents (for card games)
      else if (c instanceof core.components.Deck<?> deck) {
        sb.append("Deck[").append(c.getOwnerId()).append("]:");
        for (Component comp : deck.getComponents()) {
          sb.append(comp.getComponentName()).append(":").append(comp.getType()).append("|");
        }
      }
      // Capture Cascadia PlayerBoard hex grid layout
      else if (c instanceof games.cascadia.board.PlayerBoard board) {
        sb.append("CascadiaBoard[").append(c.getOwnerId()).append("]:");
        // Sort coordinates for consistent ordering
        java.util.List<games.cascadia.board.HexCoord> sortedCoords = new java.util.ArrayList<>(
            board.getTiles().keySet());
        sortedCoords.sort((a, b) -> {
          if (a.q != b.q)
            return Integer.compare(a.q, b.q);
          return Integer.compare(a.r, b.r);
        });
        for (games.cascadia.board.HexCoord coord : sortedCoords) {
          games.cascadia.board.PlacedHabitatTile tile = board.getTileAt(coord);
          games.cascadia.components.WildlifeToken wildlife = board.getWildlifeTokenAt(coord);
          sb.append("(").append(coord.q).append(",").append(coord.r).append(")=");
          sb.append("T:").append(tile.tile.getHabitats());
          if (wildlife != null) {
            sb.append("W:").append(wildlife.getWildlifeType());
          }
          sb.append("|");
        }
      }
    }

    // Fallback: if no spatial components found, use score-based signature
    if (sb.length() == 0) {
      sb.append("Scores:[");
      for (int p = 0; p < state.getNPlayers(); p++) {
        sb.append("P").append(p).append("=").append((int) state.getGameScore(p)).append(",");
      }
      sb.append("]");
    }

    return sb.toString();
  }

  @Override
  public void onActionApplied(AbstractGameState state, AbstractAction action) {
    String a = action.toString().replaceAll("(?i)player\\s*\\d+\\s*", "").trim();
    if (a.isEmpty()) a = action.getClass().getSimpleName();
    trace.actionCounts.merge(a, 1, Integer::sum);
    trace.totalActions++;
    actionsSinceLastTurn++;

    if (prevState != null) {
      int before = totalResourceCount(prevState);
      int after = totalResourceCount(state);

      if (after < before)
        trace.resourceSpent += (before - after);

      Map<String, Integer> beforeSnapshot = resourceSnapshot(prevState);
      Map<String, Integer> afterSnapshot = resourceSnapshot(state);

      for (Map.Entry<String, Integer> entry : beforeSnapshot.entrySet()) {
        int d = entry.getValue() - afterSnapshot.getOrDefault(entry.getKey(), 0);
        if (d > 0)
          trace.spentByType.merge(entry.getKey(), d, Integer::sum);
      }

      Map<String, Integer> prevUsage = prevState.getResourceUsage();
      Map<String, Integer> currUsage = state.getResourceUsage();

      for (Map.Entry<String, Integer> entry : currUsage.entrySet()) {
        int d = entry.getValue() - prevUsage.getOrDefault(entry.getKey(), 0);
        if (d > 0) {
          trace.resourceUsed += d;
          trace.usedByType.merge(entry.getKey(), d, Integer::sum);
        }
      }
    }

    trace.actionSequence.add(a);
    prevState = state.copy();
  }

  @Override
  public void onActionSetComputed(AbstractGameState state, int bf) {
    trace.branchingTimeline.add(bf);
    if (bf <= 1)
      trace.forcedTurns++;
  }

  private int totalResourceCount(AbstractGameState state) {
    int custom = state.getTotalResourceCount();
    if (custom >= 0) return custom;

    int sum = 0;
    for (Component c : state.getAllComponents().getComponents()) {
      if (c.getType() == ComponentType.COUNTER ||
          c.getType() == ComponentType.TOKEN) {
        sum++;
      }
    }
    return sum;
  }

  private Map<String, Integer> resourceSnapshot(AbstractGameState state) {
    Map<String, Integer> custom = state.getResourceSnapshot();
    if (!custom.isEmpty()) return custom;

    Map<String, Integer> snapshot = new HashMap<>();
    for (Component c : state.getAllComponents().getComponents()) {
      if (c.getType() == ComponentType.COUNTER ||
          c.getType() == ComponentType.TOKEN) {
        snapshot.merge(c.getComponentName(), 1, Integer::sum);
      }
    }
    return snapshot;
  }

  public GameTrace getTrace() {
    return trace;
  }
}