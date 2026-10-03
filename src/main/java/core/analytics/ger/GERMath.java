package core.analytics.ger;

import java.util.Arrays;

final class GERMath {

  private GERMath() {}

  static double mean(double[] x) {
    return Arrays.stream(x).average().orElse(0);
  }

  static double std(double[] x) {
    double m = mean(x);
    return Math.sqrt(Arrays.stream(x).map(v -> (v - m) * (v - m)).average().orElse(0));
  }

  static double[] flatten(double[][] x) {
    return Arrays.stream(x).flatMapToDouble(Arrays::stream).toArray();
  }

  static double gini(double[] values) {
    double[] sorted = values.clone();
    Arrays.sort(sorted);
    int n = sorted.length;
    double sum = 0;
    double weightedSum = 0;
    for (int i = 0; i < n; i++) {
      sum += sorted[i];
      weightedSum += (i + 1) * sorted[i];
    }
    if (sum == 0) return 0;
    return (2.0 * weightedSum) / (n * sum) - (n + 1.0) / n;
  }

  static double pearsonCorrelation(double[] x, double[] y) {
    int n = x.length;
    if (n < 2) return 0;

    double mx = mean(x);
    double my = mean(y);

    double sxy = 0, sxx = 0, syy = 0;
    for (int i = 0; i < n; i++) {
      double dx = x[i] - mx;
      double dy = y[i] - my;
      sxy += dx * dy;
      sxx += dx * dx;
      syy += dy * dy;
    }

    double denom = Math.sqrt(sxx * syy);
    return denom > 0 ? sxy / denom : 0;
  }
}
