package core.analytics.ger;

import java.util.Arrays;
import java.util.List;

import core.analytics.GameTrace;

import static core.analytics.ger.GERMath.*;

class PlayerAgencyBuilder {

  private final List<GameTrace> traces;

  PlayerAgencyBuilder(List<GameTrace> traces) {
    this.traces = traces;
  }

  PlayerAgency build(double[] elo) {

    PlayerAgency pa = new PlayerAgency();

    pa.eloStdDev = std(elo);

    // Redundancy Analysis for Agency Density
    int totalTurns = 0;
    int bookkeepingTurns = 0;
    int singlePlayerDetTurns = 0;
    int flavorChoiceTurns = 0;

    for (GameTrace t : traces) {
      totalTurns += t.turns;

      for (int bf : t.branchingTimeline) {
        if (bf <= 1) bookkeepingTurns++;
      }

      for (int i = 1; i < t.playerPerTurn.size(); i++) {
        if (t.playerPerTurn.get(i).equals(t.playerPerTurn.get(i - 1))) {
          singlePlayerDetTurns++;
        }
      }

      for (int i = 0; i < t.branchingTimeline.size() && i < t.scoreTimeline.size(); i++) {
        int bf = t.branchingTimeline.get(i);
        if (bf <= 1) continue;

        if (i > 0 && i < t.scoreTimeline.size()) {
          double[] prev = t.scoreTimeline.get(i - 1);
          double[] curr = t.scoreTimeline.get(i);
          boolean identical = true;
          for (int p = 0; p < prev.length && p < curr.length; p++) {
            if (prev[p] != curr[p]) { identical = false; break; }
          }
          if (identical) flavorChoiceTurns++;
        }
      }
    }

    pa.bookkeepingRedundancy = totalTurns > 0 ? bookkeepingTurns / (double) totalTurns : 0;
    pa.singlePlayerDeterminism = totalTurns > 0 ? singlePlayerDetTurns / (double) totalTurns : 0;
    pa.decisionMatrixRedundancy = totalTurns > 0 ? flavorChoiceTurns / (double) totalTurns : 0;

    int redundantTurns = bookkeepingTurns + singlePlayerDetTurns + flavorChoiceTurns;
    pa.agencyDensity = totalTurns > 0 ? 1.0 - (redundantTurns / (double) totalTurns) : 0;
    pa.agencyDensity = Math.max(0, pa.agencyDensity);

    computeSkillLuckIndex(pa, elo);
    return pa;
  }

  private void computeSkillLuckIndex(PlayerAgency pa, double[] elo) {
    double maxElo = Arrays.stream(elo).max().orElse(1000);
    double minElo = Arrays.stream(elo).min().orElse(1000);
    double eloRange = maxElo - minElo;

    pa.skillLeverage = 1.0 - 1.0 / (1.0 + eloRange / 400.0);

    // Luck leverage: use per-game score variance normalized by score range.
    // Also compute outcome unpredictability: how often the weaker agent
    // (by ELO) beats the stronger one. High upset rate = high luck.
    double totalScoreVariance = 0;
    int validTraces = 0;

    for (GameTrace t : traces) {
      double scoreMean = 0;
      for (double s : t.finalScores) scoreMean += s;
      scoreMean /= t.finalScores.length;

      double variance = 0;
      for (double s : t.finalScores) {
        variance += (s - scoreMean) * (s - scoreMean);
      }
      variance /= t.finalScores.length;
      totalScoreVariance += variance;
      validTraces++;
    }

    double avgVariance = validTraces > 0 ? totalScoreVariance / validTraces : 0;
    double maxScore = 0;
    for (GameTrace t : traces) {
      for (double s : t.finalScores) maxScore = Math.max(maxScore, s);
    }
    double varianceLuck = maxScore > 0 ? Math.sqrt(avgVariance) / maxScore : 0;

    // Upset rate: how often a lower-ELO player wins against a higher-ELO one.
    // In a pure-skill game this is 0; in a pure-luck game this approaches 0.5.
    int bestPlayer = 0;
    for (int i = 1; i < elo.length; i++) {
      if (elo[i] > elo[bestPlayer]) bestPlayer = i;
    }
    int upsets = 0;
    for (GameTrace t : traces) {
      if (t.winner >= 0 && t.winner != bestPlayer) upsets++;
    }
    double upsetRate = traces.size() > 0 ? upsets / (double) traces.size() : 0;

    // Combine variance-based and upset-based luck signals.
    // Upset rate is normalized to [0,1] where 0.5+ means pure luck.
    pa.luckLeverage = Math.max(varianceLuck, upsetRate * 2.0);
    pa.luckLeverage = Math.min(1.0, pa.luckLeverage);

    double sum = pa.skillLeverage + pa.luckLeverage;
    pa.skillLuckIndex = sum > 0 ? (pa.skillLeverage - pa.luckLeverage) / sum : 0;
  }

}
