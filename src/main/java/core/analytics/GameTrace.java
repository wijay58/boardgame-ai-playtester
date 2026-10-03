package core.analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameTrace {

  public int nPlayers;
  public int turns;

  public double[] finalScores;
  public int winner;

  public List<double[]> scoreTimeline = new ArrayList<>();
  public List<double[]> interactionScoreTimeline = new ArrayList<>();
  public List<Integer> branchingTimeline = new ArrayList<>();

  public String gameName;
  public String[] agents;
  public long seed;

  public List<Double> stateEntropyTimeline = new ArrayList<>();
  public String terminalStateSignature;
  public String terminalLayoutSignature;
  public String terminalCountSignature;

  public Map<String, Integer> actionCounts = new HashMap<>();
  public int totalActions = 0;

  public int resourceSpent = 0;
  public int resourceUsed = 0;
  public int resourceWasted = 0;
  public List<Integer> resourceStockTimeline = new ArrayList<>();
  public Map<String, Integer> spentByType = new HashMap<>();
  public Map<String, Integer> usedByType = new HashMap<>();
  public Map<String, Integer> wastedByType = new HashMap<>();
  public Map<String, List<Integer>> stockTimelineByType = new HashMap<>();

  public int forcedTurns = 0;
  public List<String> actionSequence = new ArrayList<>();
  public boolean softlocked = false;

  /** Number of sub-actions taken within each turn (for Action Length / fiddliness). */
  public List<Integer> actionsPerTurn = new ArrayList<>();

  /** Which player acted on each turn (for single-player determinism detection). */
  public List<Integer> playerPerTurn = new ArrayList<>();
}