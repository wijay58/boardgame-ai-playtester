package core.analytics.ger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.analytics.GameTrace;

import static core.analytics.ger.GERMath.*;

public class GERBuilder {

  private final List<GameTrace> traces;

  public GERBuilder(List<GameTrace> traces) {
    this.traces = traces;
  }

  public GER build() {
    GER ger = new GER();
    double[][] winMatrix = buildWinMatrix();
    double[] elo = computeELO(winMatrix);

    ger.meta = buildMeta();
    ger.outcome = buildOutcome();
    ger.progression = buildProgression();

    ger.resources = buildResources();

    ger.replayability = new ReplayabilityBuilder(traces).build();
    ger.strategicDepth = new StrategicDepthBuilder(traces).build();

    ger.outcome.elo = elo;
    ger.playerAgency = new PlayerAgencyBuilder(traces).build(elo);
    ger.catchUpMechanics = new CatchUpMechanicsBuilder(traces).build();
    ger.playerInteraction = new PlayerInteractionBuilder(traces).build();
    ger.randomness = new RandomnessBuilder(traces).build();
    ger.annotations = AnnotationEngine.analyze(ger);

    return ger;
  }

  // --- Elo computation ---

  private double[] computeELO(double[][] winRate) {
    int n = winRate.length;
    double[] elo = new double[n];
    Arrays.fill(elo, 1000);

    double convergenceThreshold = 0.01;
    int maxIterations = 1000;

    for (int it = 0; it < maxIterations; it++) {
      double[] oldElo = elo.clone();

      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          if (i == j) continue;

          double expected = 1.0 / (1 + Math.pow(10, (elo[j] - elo[i]) / 400));
          double score = winRate[i][j];
          elo[i] += 16 * (score - expected);
        }
      }

      double maxChange = 0;
      for (int i = 0; i < n; i++) {
        maxChange = Math.max(maxChange, Math.abs(elo[i] - oldElo[i]));
      }
      if (maxChange < convergenceThreshold) break;
    }
    return elo;
  }

  private double[][] buildWinMatrix() {
    int n = traces.get(0).finalScores.length;
    double[][] wins = new double[n][n];
    double[][] games = new double[n][n];

    for (GameTrace t : traces) {
      for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
          double si = t.finalScores[i];
          double sj = t.finalScores[j];

          if (si == sj) continue;

          if (si > sj) wins[i][j]++;
          else wins[j][i]++;

          games[i][j]++;
          games[j][i]++;
        }
      }
    }

    for (int i = 0; i < n; i++)
      for (int j = 0; j < n; j++)
        if (games[i][j] > 0)
          wins[i][j] /= games[i][j];

    return wins;
  }

  // --- Core GER sections ---

  private Meta buildMeta() {
    Meta m = new Meta();
    GameTrace first = traces.get(0);
    m.game = first.gameName;
    m.agents = first.agents;
    m.players = first.nPlayers;
    m.games = traces.size();
    m.seed = first.seed;
    return m;
  }

  private Outcome buildOutcome() {
    int n = traces.get(0).finalScores.length;
    int g = traces.size();

    Outcome o = new Outcome();
    o.winRate = new double[n];
    o.scoreMean = new double[n];
    o.scoreStd = new double[n];

    double[][] scores = new double[n][g];
    int[] wins = new int[n];
    int ties = 0;

    for (int i = 0; i < g; i++) {
      GameTrace t = traces.get(i);
      for (int p = 0; p < n; p++)
        scores[p][i] = t.finalScores[p];

      double max = Arrays.stream(t.finalScores).max().getAsDouble();
      int cnt = 0;
      for (int p = 0; p < n; p++) {
        if (t.finalScores[p] == max) {
          wins[p]++;
          cnt++;
        }
      }
      if (cnt > 1) ties++;
    }

    for (int p = 0; p < n; p++) {
      o.winRate[p] = wins[p] / (double) g;
      o.scoreMean[p] = mean(scores[p]);
      o.scoreStd[p] = std(scores[p]);
    }

    o.tieRate = ties / (double) g;

    return o;
  }

  private Progression buildProgression() {
    Progression p = new Progression();
    double[] turns = traces.stream().mapToDouble(t -> t.turns).toArray();
    p.meanTurns = mean(turns);
    p.turnStd = std(turns);
    p.earlyLeadConversion = computeEarlyLeadConversion();
    return p;
  }

  private Resources buildResources() {
    Resources global = new Resources();

    int spent = 0;
    int used = 0;
    int wasted = 0;
    List<Integer> stock = new ArrayList<>();
    Set<String> types = new HashSet<>();

    for (GameTrace t : traces) {
      spent += t.resourceSpent;
      used += t.resourceUsed;
      wasted += t.resourceWasted;
      stock.addAll(t.resourceStockTimeline);
      types.addAll(t.spentByType.keySet());
      types.addAll(t.usedByType.keySet());
      types.addAll(t.wastedByType.keySet());
      types.addAll(t.stockTimelineByType.keySet());
    }

    double total = spent + used + wasted + 1e-9;
    global.spendRate = spent / total;
    global.usedRate = used / total;
    global.wasteRate = wasted / total;
    global.lateGameValueDecay = computeDecay(stock);

    for (String type : types) {
      global.byType.put(type, computeForType(type));
    }

    return global;
  }

  private Resources computeForType(String type) {
    Resources r = new Resources();

    int spent = 0;
    int used = 0;
    int wasted = 0;
    List<Integer> stock = new ArrayList<>();

    for (GameTrace t : traces) {
      spent += t.spentByType.getOrDefault(type, 0);
      used += t.usedByType.getOrDefault(type, 0);
      wasted += t.wastedByType.getOrDefault(type, 0);
      stock.addAll(t.stockTimelineByType.getOrDefault(type, List.of()));
    }

    double total = spent + used + wasted + 1e-9;
    r.spendRate = spent / total;
    r.usedRate = used / total;
    r.wasteRate = wasted / total;
    r.lateGameValueDecay = computeDecay(stock);
    r.byType = null;

    return r;
  }

  // --- Helpers ---

  private double computeDecay(List<Integer> stock) {
    int mid = stock.size() / 2;
    double early = mean(stock.subList(0, mid).stream().mapToDouble(i -> i).toArray());
    double late = mean(stock.subList(mid, stock.size()).stream().mapToDouble(i -> i).toArray());
    return early == 0 ? 0 : (late / early);
  }

  private double computeEarlyLeadConversion() {
    int earlyLeadWins = 0;
    int validGames = 0;

    for (GameTrace t : traces) {
      if (t.scoreTimeline.isEmpty()) continue;

      int earlyTurn = Math.max(0, Math.min(t.scoreTimeline.size() - 1, t.turns / 4));
      double[] earlyScores = t.scoreTimeline.get(earlyTurn);

      int earlyLeader = 0;
      double maxEarlyScore = earlyScores[0];
      for (int p = 1; p < earlyScores.length; p++) {
        if (earlyScores[p] > maxEarlyScore) {
          maxEarlyScore = earlyScores[p];
          earlyLeader = p;
        }
      }

      if (t.winner == earlyLeader) earlyLeadWins++;
      validGames++;
    }

    return validGames > 0 ? earlyLeadWins / (double) validGames : 0;
  }
}
