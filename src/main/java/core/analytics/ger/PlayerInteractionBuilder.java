package core.analytics.ger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.analytics.GameTrace;

import static core.analytics.ger.GERMath.*;

class PlayerInteractionBuilder {

  private final List<GameTrace> traces;

  PlayerInteractionBuilder(List<GameTrace> traces) {
    this.traces = traces;
  }

  PlayerInteraction build() {

    PlayerInteraction pi = new PlayerInteraction();

    computeElementalSignatures(pi);

    // Determine dominant element among interaction signatures.
    // Wind (Chaos) is excluded from dominance when any directional interaction
    // signal (Fire, Water, Earth, Light) is significant, because Wind's coefficient
    // of variation measure saturates at 1.0 for most games and would always "win."
    double maxDirectional = Math.max(pi.earthSignature,
        Math.max(pi.waterSignature,
            Math.max(pi.fireSignature, pi.lightSignature)));

    double maxSig;
    if (maxDirectional >= 0.1) {
      maxSig = maxDirectional;
    } else {
      maxSig = Math.max(maxDirectional, pi.windSignature);
    }

    if (maxSig == pi.fireSignature) pi.dominantElement = "Fire (Conflict)";
    else if (maxSig == pi.waterSignature) pi.dominantElement = "Water (Displacement)";
    else if (maxSig == pi.earthSignature) pi.dominantElement = "Earth (Mutualism)";
    else if (maxSig == pi.windSignature) pi.dominantElement = "Wind (Chaos)";
    else pi.dominantElement = "Light (Altruism)";

    pi.responseSensitivity = computeResponseSensitivity();

    pi.interactionIntensity = (pi.fireSignature + pi.waterSignature + pi.responseSensitivity) / 3.0;

    return pi;
  }

  private List<double[]> interactionTimeline(GameTrace t) {
    return t.interactionScoreTimeline.isEmpty() ? t.scoreTimeline : t.interactionScoreTimeline;
  }

  private void computeElementalSignatures(PlayerInteraction pi) {

    int nPlayers = traces.get(0).nPlayers;

    // Earth (Mutualism): mean pairwise score correlation
    double totalCorr = 0;
    int corrPairs = 0;

    for (int i = 0; i < nPlayers; i++) {
      for (int j = i + 1; j < nPlayers; j++) {
        List<Double> deltasI = new ArrayList<>();
        List<Double> deltasJ = new ArrayList<>();

        for (GameTrace t : traces) {
          List<double[]> timeline = interactionTimeline(t);
          for (int turn = 1; turn < timeline.size(); turn++) {
            double di = timeline.get(turn)[i] - timeline.get(turn - 1)[i];
            double dj = timeline.get(turn)[j] - timeline.get(turn - 1)[j];
            deltasI.add(di);
            deltasJ.add(dj);
          }
        }

        if (deltasI.size() >= 3) {
          double corr = pearsonCorrelation(
              deltasI.stream().mapToDouble(d -> d).toArray(),
              deltasJ.stream().mapToDouble(d -> d).toArray());
          totalCorr += corr;
          corrPairs++;
        }
      }
    }

    double meanCorr = corrPairs > 0 ? totalCorr / corrPairs : 0;
    pi.earthSignature = Math.max(0, meanCorr);

    // Fire (Conflict): negative utility transfer caused by opponent actions
    // Only counts turns where the acting player gained while a non-acting player lost,
    // filtering out self-inflicted score drops from pattern recalculation.
    int conflictTurns = 0;
    int totalTurns = 0;

    for (GameTrace t : traces) {
      List<double[]> timeline = interactionTimeline(t);
      for (int turn = 1; turn < timeline.size(); turn++) {
        double[] prev = timeline.get(turn - 1);
        double[] curr = timeline.get(turn);
        totalTurns++;

        int actingPlayer = (turn < t.playerPerTurn.size()) ? t.playerPerTurn.get(turn) : -1;

        double scoreRange = 0;
        for (int p = 0; p < nPlayers; p++) scoreRange = Math.max(scoreRange, Math.abs(curr[p]));
        double minDelta = Math.max(1.0, scoreRange * 0.01);

        if (actingPlayer >= 0 && actingPlayer < nPlayers) {
          boolean actorGained = (curr[actingPlayer] - prev[actingPlayer]) >= minDelta;
          boolean otherLost = false;
          for (int p = 0; p < nPlayers; p++) {
            if (p != actingPlayer && (prev[p] - curr[p]) >= minDelta) {
              otherLost = true;
              break;
            }
          }
          if (actorGained && otherLost) conflictTurns++;
        } else {
          boolean someoneGained = false;
          boolean someoneLost = false;
          for (int p = 0; p < nPlayers; p++) {
            if ((curr[p] - prev[p]) >= minDelta) someoneGained = true;
            if ((prev[p] - curr[p]) >= minDelta) someoneLost = true;
          }
          if (someoneGained && someoneLost) conflictTurns++;
        }
      }
    }

    pi.fireSignature = totalTurns > 0 ? conflictTurns / (double) totalTurns : 0;
    pi.fireSignature = Math.max(pi.fireSignature, Math.max(0, -meanCorr));

    // Water (Displacement): branching factor drops after opponent turns
    int contentionEvents = 0;
    int eligibleTurns = 0;

    for (GameTrace t : traces) {
      for (int turn = 1; turn < t.branchingTimeline.size() && turn < t.playerPerTurn.size(); turn++) {
        int prevBf = t.branchingTimeline.get(turn - 1);
        int currBf = t.branchingTimeline.get(turn);

        if (turn < t.playerPerTurn.size() && turn - 1 < t.playerPerTurn.size()
            && !t.playerPerTurn.get(turn).equals(t.playerPerTurn.get(turn - 1))) {
          eligibleTurns++;
          if (prevBf > 0 && currBf < prevBf * 0.7) {
            contentionEvents++;
          }
        }
      }
    }

    pi.waterSignature = eligibleTurns > 0 ? contentionEvents / (double) eligibleTurns : 0;

    // Wind (Chaos): mean absolute entropy delta relative to overall entropy range.
    // A high value means the game state changes drastically between turns.
    List<Double> entropyDeltas = new ArrayList<>();
    double minEntropy = Double.MAX_VALUE;
    double maxEntropy = Double.MIN_VALUE;
    for (GameTrace t : traces) {
      for (int i = 0; i < t.stateEntropyTimeline.size(); i++) {
        double h = t.stateEntropyTimeline.get(i);
        minEntropy = Math.min(minEntropy, h);
        maxEntropy = Math.max(maxEntropy, h);
        if (i > 0) {
          entropyDeltas.add(Math.abs(h - t.stateEntropyTimeline.get(i - 1)));
        }
      }
    }

    if (!entropyDeltas.isEmpty()) {
      double[] deltas = entropyDeltas.stream().mapToDouble(d -> d).toArray();
      double meanDelta = mean(deltas);
      double entropyRange = maxEntropy - minEntropy;
      pi.windSignature = entropyRange > 0 ? Math.min(1.0, meanDelta / entropyRange) : 0;
    }

    // Light (Altruism): acting player doesn't gain but others do.
    // The score delta between timeline[turn-1] and timeline[turn] is caused by
    // playerPerTurn[turn], not playerPerTurn[turn-1], because each timeline entry
    // is a snapshot taken after the corresponding player's turn completes.
    int altruisticTurns = 0;
    int scoredTurns = 0;

    for (GameTrace t : traces) {
      List<double[]> iTimeline = interactionTimeline(t);
      for (int turn = 1; turn < iTimeline.size() && turn < t.playerPerTurn.size(); turn++) {
        double[] prev = iTimeline.get(turn - 1);
        double[] curr = iTimeline.get(turn);

        int actingPlayer = t.playerPerTurn.get(turn);
        if (actingPlayer < 0 || actingPlayer >= nPlayers) continue;

        double scoreRange = 0;
        for (int p = 0; p < nPlayers; p++) scoreRange = Math.max(scoreRange, Math.abs(curr[p]));
        double minDelta = Math.max(1.0, scoreRange * 0.01);

        boolean selfGained = (curr[actingPlayer] - prev[actingPlayer]) >= minDelta;
        boolean othersGained = false;
        for (int p = 0; p < nPlayers; p++) {
          if (p != actingPlayer && (curr[p] - prev[p]) >= minDelta) {
            othersGained = true;
            break;
          }
        }

        scoredTurns++;
        if (!selfGained && othersGained) altruisticTurns++;
      }
    }

    pi.lightSignature = scoredTurns > 0 ? altruisticTurns / (double) scoredTurns : 0;
  }

  private double computeResponseSensitivity() {
    // Measures whether players change action CATEGORY after opponents act.
    // Uses the first word of the action string as the category to avoid
    // false positives from unique action descriptions (e.g., different tile placements
    // are all the same category but different specific actions).
    int responsive = 0;
    int eligible = 0;

    for (GameTrace t : traces) {
      Map<Integer, String> lastCategory = new HashMap<>();

      for (int turn = 0; turn < t.playerPerTurn.size() && turn < t.actionSequence.size(); turn++) {
        int player = t.playerPerTurn.get(turn);

        int actionIdx = 0;
        for (int i = 0; i < turn && i < t.actionsPerTurn.size(); i++) {
          actionIdx += t.actionsPerTurn.get(i);
        }

        if (actionIdx >= t.actionSequence.size()) continue;
        String action = t.actionSequence.get(actionIdx);
        // Extract category: first word, or up to first space/digit
        String category = action.split("[\\s\\d]", 2)[0];

        if (lastCategory.containsKey(player)) {
          eligible++;
          if (!category.equals(lastCategory.get(player))) {
            responsive++;
          }
        }

        lastCategory.put(player, category);
      }
    }

    return eligible > 0 ? responsive / (double) eligible : 0;
  }

}
