package core.analytics.ger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import core.analytics.GameTrace;

import static core.analytics.ger.GERMath.*;

class StrategicDepthBuilder {

  private final List<GameTrace> traces;

  StrategicDepthBuilder(List<GameTrace> traces) {
    this.traces = traces;
  }

  StrategicDepth build() {

    StrategicDepth sd = new StrategicDepth();

    // Mean Branching Factor
    List<Integer> allBranches = new ArrayList<>();
    for (GameTrace t : traces) {
      allBranches.addAll(t.branchingTimeline);
    }
    double[] branchFactors = allBranches.stream().mapToDouble(i -> i).toArray();
    sd.meanBranchingFactor = mean(branchFactors);

    // Action Length (fiddliness)
    List<Double> allActionLengths = new ArrayList<>();
    for (GameTrace t : traces) {
      for (int apt : t.actionsPerTurn) {
        allActionLengths.add((double) apt);
      }
    }
    sd.meanActionLength = allActionLengths.isEmpty() ? 0
        : mean(allActionLengths.stream().mapToDouble(d -> d).toArray());

    // Administrative Action Ratio
    int totalActions = 0;
    int adminActions = 0;
    for (GameTrace t : traces) {
      for (int i = 0; i < t.actionsPerTurn.size() && i < t.branchingTimeline.size(); i++) {
        int actionsThisTurn = t.actionsPerTurn.get(i);
        int bf = t.branchingTimeline.get(i);
        totalActions += actionsThisTurn;
        if (bf <= 1) {
          adminActions += actionsThisTurn;
        }
      }
    }
    sd.administrativeActionRatio = totalActions > 0 ? adminActions / (double) totalActions : 0;

    // Dead Action Rate
    Set<String> allKnownActions = new HashSet<>();
    Map<String, Integer> globalActionCounts = new HashMap<>();
    for (GameTrace t : traces) {
      allKnownActions.addAll(t.actionCounts.keySet());
      for (var e : t.actionCounts.entrySet()) {
        globalActionCounts.merge(e.getKey(), e.getValue(), Integer::sum);
      }
    }
    long deadActions = globalActionCounts.values().stream().filter(v -> v == 0).count();
    sd.deadActionRate = allKnownActions.isEmpty() ? 0 : deadActions / (double) allKnownActions.size();

    // Opacity
    sd.opacityIndex = computeOpacityIndex();
    sd.meanRewardDelay = computeMeanRewardDelay();

    // Consequence Sensitivity
    sd.consequenceSensitivity = computeConsequenceSensitivity();

    // Strategic Breadth
    sd.strategicBreadth = computeStrategicBreadth();

    // Move Diversity
    sd.moveDiversity = computeMoveDiversity();

    return sd;
  }

  private double computeOpacityIndex() {
    int maxLag = 10;
    double[] correlations = new double[maxLag];
    int validLags = 0;

    for (int lag = 1; lag <= maxLag; lag++) {
      List<Double> actionSignals = new ArrayList<>();
      List<Double> scoreDeltas = new ArrayList<>();

      for (GameTrace t : traces) {
        for (int turn = 0; turn < t.branchingTimeline.size() - lag && turn < t.scoreTimeline.size() - lag; turn++) {
          double bf = t.branchingTimeline.get(turn);
          double[] scoresNow = t.scoreTimeline.get(turn);
          double[] scoresLater = t.scoreTimeline.get(turn + lag);

          double scoreDelta = 0;
          for (int p = 0; p < scoresNow.length; p++) {
            scoreDelta += Math.abs(scoresLater[p] - scoresNow[p]);
          }

          actionSignals.add(bf);
          scoreDeltas.add(scoreDelta);
        }
      }

      if (actionSignals.size() >= 3) {
        correlations[lag - 1] = Math.abs(pearsonCorrelation(
            actionSignals.stream().mapToDouble(d -> d).toArray(),
            scoreDeltas.stream().mapToDouble(d -> d).toArray()));
        validLags++;
      }
    }

    if (validLags < 2) return 0;

    double earlyCorr = correlations[0];
    if (earlyCorr < 0.01) return 0;

    double lateCorr = 0;
    int lateCount = 0;
    for (int i = 2; i < validLags; i++) {
      lateCorr += correlations[i];
      lateCount++;
    }

    return lateCount > 0 ? (lateCorr / lateCount) / earlyCorr : 0;
  }

  private double computeMeanRewardDelay() {
    int maxLag = 10;
    double peakCorr = 0;
    int peakLag = 1;

    for (int lag = 1; lag <= maxLag; lag++) {
      List<Double> actionSignals = new ArrayList<>();
      List<Double> scoreDeltas = new ArrayList<>();

      for (GameTrace t : traces) {
        for (int turn = 0; turn < t.branchingTimeline.size() - lag && turn < t.scoreTimeline.size() - lag; turn++) {
          double bf = t.branchingTimeline.get(turn);
          double[] scoresNow = t.scoreTimeline.get(turn);
          double[] scoresLater = t.scoreTimeline.get(turn + lag);

          double scoreDelta = 0;
          for (int p = 0; p < scoresNow.length; p++) {
            scoreDelta += Math.abs(scoresLater[p] - scoresNow[p]);
          }

          actionSignals.add(bf);
          scoreDeltas.add(scoreDelta);
        }
      }

      if (actionSignals.size() >= 3) {
        double corr = Math.abs(pearsonCorrelation(
            actionSignals.stream().mapToDouble(d -> d).toArray(),
            scoreDeltas.stream().mapToDouble(d -> d).toArray()));
        if (corr > peakCorr) {
          peakCorr = corr;
          peakLag = lag;
        }
      }
    }

    return peakLag;
  }

  private double computeConsequenceSensitivity() {
    Set<Integer> winners = new HashSet<>();
    for (GameTrace t : traces) winners.add(t.winner);

    if (winners.size() <= 1) return 0;

    Map<Integer, Map<String, Double>> distByWinner = new HashMap<>();
    Map<Integer, Integer> totalByWinner = new HashMap<>();

    for (GameTrace t : traces) {
      distByWinner.computeIfAbsent(t.winner, k -> new HashMap<>());
      for (var e : t.actionCounts.entrySet()) {
        distByWinner.get(t.winner).merge(e.getKey(), (double) e.getValue(), Double::sum);
      }
      totalByWinner.merge(t.winner, t.totalActions, Integer::sum);
    }

    Set<String> allActions = new HashSet<>();
    for (var d : distByWinner.values()) allActions.addAll(d.keySet());

    List<Integer> winnerList = new ArrayList<>(winners);
    double totalJSD = 0;
    int pairs = 0;

    for (int i = 0; i < winnerList.size(); i++) {
      for (int j = i + 1; j < winnerList.size(); j++) {
        Map<String, Double> dA = distByWinner.get(winnerList.get(i));
        Map<String, Double> dB = distByWinner.get(winnerList.get(j));
        int tA = totalByWinner.get(winnerList.get(i));
        int tB = totalByWinner.get(winnerList.get(j));

        double jsd = 0;
        for (String action : allActions) {
          double pA = tA > 0 ? dA.getOrDefault(action, 0.0) / tA : 0;
          double pB = tB > 0 ? dB.getOrDefault(action, 0.0) / tB : 0;
          double m = (pA + pB) / 2.0;
          if (m > 0) {
            if (pA > 0) jsd += pA * Math.log(pA / m);
            if (pB > 0) jsd += pB * Math.log(pB / m);
          }
        }
        totalJSD += jsd / 2.0;
        pairs++;
      }
    }

    return pairs > 0 ? totalJSD / pairs : 0;
  }

  private double computeStrategicBreadth() {
    Set<String> allActions = new HashSet<>();
    Set<String> winnerActions = new HashSet<>();

    for (GameTrace t : traces) {
      allActions.addAll(t.actionCounts.keySet());

      if (t.winner >= 0) {
        for (var e : t.actionCounts.entrySet()) {
          if (e.getValue() > 0) {
            winnerActions.add(e.getKey());
          }
        }
      }
    }

    return allActions.isEmpty() ? 0 : winnerActions.size() / (double) allActions.size();
  }

  private double computeMoveDiversity() {
    int nGames = traces.size();
    if (nGames < 2) return 0;

    Set<String> allActions = new HashSet<>();
    for (GameTrace t : traces) allActions.addAll(t.actionCounts.keySet());

    double totalEntropy = 0;
    int validActions = 0;

    for (String action : allActions) {
      double[] proportions = new double[nGames];
      double actionTotal = 0;

      for (int i = 0; i < nGames; i++) {
        proportions[i] = traces.get(i).actionCounts.getOrDefault(action, 0);
        actionTotal += proportions[i];
      }

      if (actionTotal == 0) continue;

      double h = 0;
      for (double count : proportions) {
        double p = count / actionTotal;
        if (p > 0) h -= p * Math.log(p);
      }

      totalEntropy += h;
      validActions++;
    }

    double maxH = Math.log(nGames);
    return (validActions > 0 && maxH > 0) ? (totalEntropy / validActions) / maxH : 0;
  }
}
