package core.analytics.ger;

import java.util.Arrays;
import java.util.List;

import core.analytics.GameTrace;

import static core.analytics.ger.GERMath.*;

class CatchUpMechanicsBuilder {

  private final List<GameTrace> traces;

  CatchUpMechanicsBuilder(List<GameTrace> traces) {
    this.traces = traces;
  }

  CatchUpMechanics build() {
    CatchUpMechanics cm = new CatchUpMechanics();
    computeGiniTimeline(cm);
    cm.winCurveResilience = computeWinCurveResilience();
    return cm;
  }

  private void computeGiniTimeline(CatchUpMechanics cm) {
    double peakGini = 0;
    double peakTiming = 0;
    double totalGini = 0;
    int giniCount = 0;

    for (GameTrace t : traces) {
      int totalTurns = t.scoreTimeline.size();
      if (totalTurns == 0) continue;

      for (int turn = 0; turn < totalTurns; turn++) {
        double[] scores = t.scoreTimeline.get(turn);

        double g = gini(scores);
        totalGini += g;
        giniCount++;

        double fraction = turn / (double) totalTurns;

        if (fraction < 0.75 && g > peakGini) {
          peakGini = g;
          peakTiming = fraction;
        }
      }
    }

    cm.peakGiniBefore75 = peakGini;
    cm.peakGiniTiming = peakTiming;
    cm.meanGini = giniCount > 0 ? totalGini / giniCount : 0;
  }

  private double computeWinCurveResilience() {
    int trailingAndWon = 0;
    int trailingTotal = 0;

    for (GameTrace t : traces) {
      if (t.scoreTimeline.isEmpty() || t.winner < 0) continue;

      int midTurn = t.scoreTimeline.size() / 2;
      if (midTurn >= t.scoreTimeline.size()) continue;

      double[] midScores = t.scoreTimeline.get(midTurn);
      double maxMidScore = Arrays.stream(midScores).max().orElse(0.0);

      for (int p = 0; p < midScores.length; p++) {
        if (maxMidScore > 0 && midScores[p] < maxMidScore * 0.75) {
          trailingTotal++;
          if (t.winner == p) trailingAndWon++;
        }
      }
    }

    return trailingTotal > 0 ? trailingAndWon / (double) trailingTotal : 0;
  }
}
