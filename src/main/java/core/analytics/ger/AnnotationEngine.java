package core.analytics.ger;

import java.util.ArrayList;
import java.util.List;

public class AnnotationEngine {

  public static List<DesignAnnotation> analyze(GER ger) {

    List<DesignAnnotation> out = new ArrayList<>();

    // Snowball risk
    if (ger.progression.earlyLeadConversion > 0.8) {
      out.add(warn(
          "Strategic Snowballing",
          "Early leads almost always convert into wins.",
          "Add rubber-banding, hidden objectives, or late-game swing opportunities."));
    }

    // Resource irrelevance
    if (ger.resources != null && ger.resources.spendRate < 0.35
        && ger.resources.usedRate < 0.1) {
      out.add(warn(
          "Low Resource Relevance",
          "Resources are frequently wasted or unused.",
          "Increase resource sinks, scaling costs, or late-game conversion."));
    }

    // Replayability: low outcome diversity
    if (ger.replayability != null) {
      if (ger.replayability.shannonDiversityIndex < 0.5 && ger.replayability.outcomeRichness <= 2) {
        out.add(critical(
            "Low Outcome Diversity",
            "Simulations converge on nearly identical outcomes (Shannon H < 0.5). The game may be solvable.",
            "Add asymmetric factions, modular setup, or input randomness to widen viable victory paths."));
      }

      if (ger.replayability.outcomeEvenness < 0.4 && ger.replayability.outcomeRichness > 1) {
        out.add(warn(
            "Uneven Outcome Distribution",
            "One outcome category dominates despite multiple categories existing.",
            "Rebalance dominant faction/strategy or add catch-up mechanics."));
      }

      if (ger.replayability.entropyCollapseRate > 0.7) {
        out.add(warn(
            "Rapid Entropy Collapse",
            "Decision entropy drops sharply during sessions  the game becomes predictable early.",
            "Introduce late-game branching, hidden information, or escalating complexity."));
      }

      if (ger.replayability.normalizedHammingDistance < 0.2) {
        out.add(warn(
            "Low Action Diversity",
            "Players take very similar action sequences across simulations.",
            "Add variable setup, diverse action cards, or contextual action availability."));
      }
    }

    // Strategic Depth / Weight
    if (ger.strategicDepth != null) {
      if (ger.strategicDepth.meanActionLength > 5.0) {
        out.add(warn(
            "High Fiddliness",
            String.format("Mean %.1f sub-actions per turn  excessive procedural overhead.",
                ger.strategicDepth.meanActionLength),
            "Consolidate administrative steps, batch upkeep phases, or automate bookkeeping."));
      }

      if (ger.strategicDepth.administrativeActionRatio > 0.8) {
        out.add(critical(
            "Excessive Administrative Overhead",
            "Over 80% of actions are forced (no real choice)  the game is complex without being deep.",
            "Reduce mandatory sub-steps, merge forced sequences, or give players opt-out choices."));
      }

      if (ger.strategicDepth.deadActionRate > 0.3) {
        out.add(warn(
            "High Dead Action Rate",
            String.format("%.0f%% of action types are never used  false complexity.",
                ger.strategicDepth.deadActionRate * 100),
            "Remove or rework unused actions; ensure each option has a viable strategic niche."));
      }

      if (ger.strategicDepth.opacityIndex > 1.0 && ger.strategicDepth.meanRewardDelay >= 4) {
        out.add(info(
            "High Opacity",
            String.format("Actions have long-horizon consequences  reward delay is %.0f turns with opacity %.2f.",
                ger.strategicDepth.meanRewardDelay, ger.strategicDepth.opacityIndex),
            "Consider adding intermediate feedback signals so players can evaluate their choices sooner."));
      }

      if (ger.strategicDepth.consequenceSensitivity < 0.01) {
        out.add(warn(
            "Low Consequence Sensitivity",
            "Move selection barely affects outcomes  the game may lack strategic depth.",
            "Increase the impact of key decisions, add asymmetric payoffs, or deepen action consequences."));
      }

      if (ger.strategicDepth.strategicBreadth < 0.5) {
        out.add(warn(
            "Narrow Strategic Breadth",
            "Less than half of available action types contribute to winning strategies.",
            "Rebalance underused actions or create synergies that reward diverse play."));
      }
    }

    // Player Agency
    if (ger.playerAgency != null) {
      if (ger.playerAgency.bookkeepingRedundancy > 0.6) {
        out.add(warn(
            "High Bookkeeping Redundancy",
            String.format("%.0f%% of turns have no real choice (forced moves).",
                ger.playerAgency.bookkeepingRedundancy * 100),
            "Increase market width, add wildcards, or increase board freedom."));
      }

      if (ger.playerAgency.decisionMatrixRedundancy > 0.4) {
        out.add(warn(
            "High Flavor Choice Rate",
            String.format("%.0f%% of turns present choices that lead to identical outcomes.",
                ger.playerAgency.decisionMatrixRedundancy * 100),
            "Differentiate action payoffs  ensure moves have meaningfully different consequences."));
      }

      if (ger.playerAgency.agencyDensity < 0.4
          && ger.playerAgency.bookkeepingRedundancy <= 0.6
          && ger.playerAgency.decisionMatrixRedundancy <= 0.4) {
        out.add(critical(
            "Low Agency Density",
            String.format("Only %.0f%% of turns involve non-redundant, impactful decisions.",
                ger.playerAgency.agencyDensity * 100),
            "Reduce bookkeeping turns, consolidate forced sequences, and ensure choices have distinct outcomes."));
      }

      if (ger.playerAgency.skillLuckIndex < -0.3) {
        out.add(warn(
            "Luck-Dominated Game",
            String.format("Skill-Luck Index is %.2f  chance outweighs skill.",
                ger.playerAgency.skillLuckIndex),
            "Reduce output randomness, add strategic mitigation of luck, or increase decision weight."));
      }

      if (ger.catchUpMechanics.peakGiniBefore75 > 0.5) {
        out.add(warn(
            "Early Resource Monopolization",
            String.format("Peak Gini coefficient reaches %.2f before 75%% completion  runaway leader risk.",
                ger.catchUpMechanics.peakGiniBefore75),
            "Implement catch-up mechanics, dynamic game balancing, or diminishing returns on leads."));
      }

      if (ger.catchUpMechanics.winCurveResilience < 0.05 && ger.catchUpMechanics.peakGiniBefore75 > 0.3) {
        out.add(critical(
            "Collapsed Win Curve",
            "Trailing players at the midpoint have near-zero chance of winning.",
            "Add rubber-banding, hidden objectives, or late-game swing opportunities."));
      }
    }

    // Player Interaction
    if (ger.playerInteraction != null) {
      if (ger.playerInteraction.interactionIntensity < 0.1) {
        out.add(warn(
            "Multiplayer Solitaire",
            String.format("Interaction intensity is only %.2f  players rarely affect each other.",
                ger.playerInteraction.interactionIntensity),
            "Add drafting, blocking, trading, or reactive phases to increase player-to-player influence."));
      }

      if (ger.playerInteraction.fireSignature > 0.5 && ger.playerInteraction.earthSignature < 0.1) {
        out.add(info(
            "High-Conflict Social Signature",
            String.format("Fire=%.2f, Earth=%.2f  the game is heavily zero-sum.",
                ger.playerInteraction.fireSignature, ger.playerInteraction.earthSignature),
            "Consider adding mutualistic elements (trading, shared scoring) to soften the conflict loop."));
      }

      if (ger.playerInteraction.windSignature > 0.7) {
        out.add(warn(
            "High Chaos Signature",
            "State variance between turns is very high  plans are frequently disrupted.",
            "Reduce random disruption events or give players ways to protect their positions."));
      }

      if (ger.playerInteraction.responseSensitivity < 0.1) {
        out.add(info(
            "Low Response Sensitivity",
            "Players rarely change strategy in response to opponents.",
            "Add more visible opponent state, reactive mechanics, or incentives to adapt."));
      }

    }

    // Randomness Management
    if (ger.randomness != null) {
      if (ger.randomness.brittlenessIndex > 0.5) {
        out.add(warn(
            "Brittle Outcome Resolution",
            String.format("%.0f%% of games were decided by a single large score swing.",
                ger.randomness.brittlenessIndex * 100),
            "Add buffers against bad luck  backup plans, insurance mechanics, or diminishing returns on swings."));
      }

      double avgScoreStd = ger.outcome.scoreStd != null
          ? java.util.Arrays.stream(ger.outcome.scoreStd).average().orElse(0)
          : 0;
      if (ger.randomness.realizationVariance > ger.randomness.meanScoreDelta * 2
          && avgScoreStd > 0
          && ger.randomness.realizationVariance > avgScoreStd) {
        out.add(warn(
            "High Output Randomness",
            "Score volatility (realization variance) far exceeds mean delta  outcomes are very swingy.",
            "Reduce output randomness magnitude, add risk mitigation options, or smooth scoring curves."));
      }

      if (ger.randomness.leaderChangeRate > 0.4) {
        out.add(info(
            "Volatile Leadership",
            String.format("The leader changes %.0f%% of turns  high uncertainty throughout.",
                ger.randomness.leaderChangeRate * 100),
            "This maintains tension but may frustrate strategic planning if too chaotic."));
      }

      if (ger.randomness.adaptationPotential < 0.3) {
        out.add(warn(
            "Low Adaptation Potential",
            "After random events, agents frequently have very few viable options.",
            "Ensure random inputs expand rather than constrict the decision space."));
      }

    }

    return out;
  }

  private static DesignAnnotation info(String t, String e, String s) {
    return new DesignAnnotation("INFO", t, e, s);
  }

  private static DesignAnnotation warn(String t, String e, String s) {
    return new DesignAnnotation("WARNING", t, e, s);
  }

  private static DesignAnnotation critical(String t, String e, String s) {
    return new DesignAnnotation("CRITICAL", t, e, s);
  }
}
