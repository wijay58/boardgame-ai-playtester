package core.analytics.ger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.analytics.GameTrace;

import static core.analytics.ger.GERMath.*;

class RandomnessBuilder {

  private final List<GameTrace> traces;

  RandomnessBuilder(List<GameTrace> traces) {
    this.traces = traces;
  }

  RandomnessManagement build() {

    RandomnessManagement rm = new RandomnessManagement();

    computeInputRandomness(rm);
    computeOutputRandomness(rm);

    return rm;
  }

  private void computeInputRandomness(RandomnessManagement rm) {
    // Decision Space Breadth: how many viable options remain after input randomness
    // Use branching factor as proxy high bf after random draws = good adaptation
    // potential
    List<Double> allBf = new ArrayList<>();

    for (GameTrace t : traces) {
      for (int bf : t.branchingTimeline) {
        allBf.add((double) bf);
      }
    }

    if (allBf.isEmpty()) {
      rm.inputAdaptationBreadth = 0;
      rm.inputBreadthVariance = 0;
      rm.adaptationPotential = 0;
      return;
    }

    double[] bfArray = allBf.stream().mapToDouble(d -> d).toArray();
    rm.inputAdaptationBreadth = mean(bfArray);
    rm.inputBreadthVariance = std(bfArray);

    // Adaptation potential: fraction of turns where bf stays above median
    double[] sorted = bfArray.clone();
    Arrays.sort(sorted);
    double median = sorted[sorted.length / 2];

    long aboveMedian = 0;
    for (double bf : bfArray) {
      if (bf >= median)
        aboveMedian++;
    }
    rm.adaptationPotential = aboveMedian / (double) bfArray.length;
  }

  private void computeOutputRandomness(RandomnessManagement rm) {
    int nPlayers = traces.get(0).nPlayers;

    List<Double> allDeltas = new ArrayList<>();
    double maxSwing = 0;
    int leaderChanges = 0;
    int leaderComparisons = 0;

    for (GameTrace t : traces) {
      for (int turn = 1; turn < t.scoreTimeline.size(); turn++) {
        double[] prev = t.scoreTimeline.get(turn - 1);
        double[] curr = t.scoreTimeline.get(turn);

        // Total absolute score delta across all players this turn
        double turnDelta = 0;
        for (int p = 0; p < nPlayers && p < prev.length && p < curr.length; p++) {
          turnDelta += Math.abs(curr[p] - prev[p]);
        }
        allDeltas.add(turnDelta);
        maxSwing = Math.max(maxSwing, turnDelta);

        // Leader change detection
        int prevLeader = argmax(prev, nPlayers);
        int currLeader = argmax(curr, nPlayers);
        if (prevLeader != currLeader)
          leaderChanges++;
        leaderComparisons++;
      }
    }

    if (!allDeltas.isEmpty()) {
      double[] deltaArray = allDeltas.stream().mapToDouble(d -> d).toArray();
      rm.meanScoreDelta = mean(deltaArray);
      rm.realizationVariance = std(deltaArray);
    }

    rm.maxScoreSwing = maxSwing;
    rm.leaderChangeRate = leaderComparisons > 0 ? leaderChanges / (double) leaderComparisons : 0;

    // Brittleness Index: games where the largest single-turn swing decided the
    // winner
    rm.brittlenessIndex = computeBrittleness();
  }

  private double computeBrittleness() {
    int brittleGames = 0;
    int validGames = 0;

    for (GameTrace t : traces) {
      if (t.scoreTimeline.size() < 2 || t.winner < 0)
        continue;
      validGames++;

      int nPlayers = t.nPlayers;

      // Find the largest single-turn swing for the winner
      double maxWinnerSwing = 0;
      double totalWinnerGain = 0;

      for (int turn = 1; turn < t.scoreTimeline.size(); turn++) {
        double prev = t.scoreTimeline.get(turn - 1)[t.winner];
        double curr = t.scoreTimeline.get(turn)[t.winner];
        double delta = curr - prev;

        if (delta > 0) {
          totalWinnerGain += delta;
          maxWinnerSwing = Math.max(maxWinnerSwing, delta);
        }
      }

      // If the largest single swing accounts for > 40% of total gains, game is
      // "brittle"
      if (totalWinnerGain > 0 && maxWinnerSwing / totalWinnerGain > 0.4) {
        brittleGames++;
      }
    }

    return validGames > 0 ? brittleGames / (double) validGames : 0;
  }

  private int argmax(double[] arr, int n) {
    int best = 0;
    for (int i = 1; i < n && i < arr.length; i++) {
      if (arr[i] > arr[best])
        best = i;
    }
    return best;
  }
}
