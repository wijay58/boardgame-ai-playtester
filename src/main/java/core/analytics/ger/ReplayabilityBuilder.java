package core.analytics.ger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.analytics.GameTrace;

import static core.analytics.ger.GERMath.*;

class ReplayabilityBuilder {

  private final List<GameTrace> traces;

  ReplayabilityBuilder(List<GameTrace> traces) {
    this.traces = traces;
  }

  Replayability build() {

    Replayability r = new Replayability();

    // Compute Shannon diversity over full ordinal position vectors (e.g. "2,1,3")
    // rather than just winner identity. This captures outcome diversity even when
    // one agent dominates the 2nd/3rd place ordering still varies.
    Map<String, Integer> outcomeCounts = new HashMap<>();
    for (GameTrace t : traces) {
      int n = t.finalScores.length;
      int[] ordinals = new int[n];
      for (int p = 0; p < n; p++) {
        int rank = 1;
        for (int q = 0; q < n; q++) {
          if (t.finalScores[q] > t.finalScores[p])
            rank++;
        }
        ordinals[p] = rank;
      }
      outcomeCounts.merge(java.util.Arrays.toString(ordinals), 1, Integer::sum);
    }

    int N = traces.size();
    int R = outcomeCounts.size();
    r.outcomeRichness = R;

    double H = 0;
    for (int count : outcomeCounts.values()) {
      double p = count / (double) N;
      if (p > 0) {
        H -= p * Math.log(p);
      }
    }
    r.shannonDiversityIndex = H;

    r.outcomeEvenness = R > 1 ? H / Math.log(R) : 0.0;

    double hCorrected = H + (R - 1) / (2.0 * N);
    r.zahlEstimator = Math.exp(hCorrected);

    r.meanHammingDistance = computeMeanHammingDistance();
    r.normalizedHammingDistance = computeNormalizedHammingDistance();
    r.entropyCollapseRate = computeEntropyCollapseRate();

    fitEntropyPerformanceModel(r);

    return r;
  }

  private double computeMeanHammingDistance() {
    int pairs = 0;
    double totalDistance = 0;

    for (int i = 0; i < traces.size(); i++) {
      for (int j = i + 1; j < traces.size(); j++) {
        List<String> seqA = traces.get(i).actionSequence;
        List<String> seqB = traces.get(j).actionSequence;

        int len = Math.min(seqA.size(), seqB.size());
        int diff = Math.abs(seqA.size() - seqB.size());

        for (int k = 0; k < len; k++) {
          if (!seqA.get(k).equals(seqB.get(k))) {
            diff++;
          }
        }

        totalDistance += diff;
        pairs++;
      }
    }

    return pairs > 0 ? totalDistance / pairs : 0;
  }

  private double computeNormalizedHammingDistance() {
    int pairs = 0;
    double totalNormalized = 0;

    for (int i = 0; i < traces.size(); i++) {
      for (int j = i + 1; j < traces.size(); j++) {
        List<String> seqA = traces.get(i).actionSequence;
        List<String> seqB = traces.get(j).actionSequence;

        int maxLen = Math.max(seqA.size(), seqB.size());
        if (maxLen == 0)
          continue;

        int len = Math.min(seqA.size(), seqB.size());
        int diff = Math.abs(seqA.size() - seqB.size());

        for (int k = 0; k < len; k++) {
          if (!seqA.get(k).equals(seqB.get(k))) {
            diff++;
          }
        }

        totalNormalized += diff / (double) maxLen;
        pairs++;
      }
    }

    return pairs > 0 ? totalNormalized / pairs : 0;
  }

  private double computeEntropyCollapseRate() {
    double totalCollapseRate = 0;
    int validTraces = 0;

    for (GameTrace t : traces) {
      List<Double> timeline = t.stateEntropyTimeline;
      if (timeline.size() < 4)
        continue;

      int quarter = timeline.size() / 4;

      double earlyH = mean(timeline.subList(0, quarter).stream().mapToDouble(d -> d).toArray());
      double lateH = mean(timeline.subList(timeline.size() - quarter, timeline.size())
          .stream().mapToDouble(d -> d).toArray());

      if (earlyH > 0) {
        totalCollapseRate += (earlyH - lateH) / earlyH;
        validTraces++;
      }
    }

    return validTraces > 0 ? totalCollapseRate / validTraces : 0;
  }

  private void fitEntropyPerformanceModel(Replayability r) {
    List<double[]> points = new ArrayList<>();

    for (GameTrace t : traces) {
      if (t.stateEntropyTimeline.isEmpty())
        continue;

      double meanH = mean(t.stateEntropyTimeline.stream().mapToDouble(d -> d).toArray());
      double performance;
      if (t.winner >= 0 && t.winner < t.finalScores.length) {
        performance = t.finalScores[t.winner];
      } else {
        performance = Arrays.stream(t.finalScores).max().orElse(0.0);
      }

      points.add(new double[] { meanH, performance });
    }

    if (points.size() < 3) {
      r.entropyPerformanceA = 0;
      r.entropyPerformanceB = 0;
      return;
    }

    double[] X = new double[points.size()];
    double[] Y = new double[points.size()];

    for (int i = 0; i < points.size(); i++) {
      X[i] = Math.exp(points.get(i)[0]);
      Y[i] = points.get(i)[1];
    }

    double meanX = mean(X);
    double meanY = mean(Y);

    double sxy = 0, sxx = 0;
    for (int i = 0; i < X.length; i++) {
      sxy += (X[i] - meanX) * (Y[i] - meanY);
      sxx += (X[i] - meanX) * (X[i] - meanX);
    }

    double slope = sxx > 0 ? sxy / sxx : 0;
    double intercept = meanY - slope * meanX;

    r.entropyPerformanceA = -slope;
    r.entropyPerformanceB = intercept;
  }
}
