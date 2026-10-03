package games.cascadia;

import java.util.ArrayList;
import java.util.List;

import core.AbstractPlayer;
import core.Game;
import core.analytics.GameTrace;
import core.analytics.TraceObserver;
import core.analytics.ger.GER;
import core.analytics.ger.GERBuilder;
import core.analytics.ger.GERExporter;
import players.PlayerConstants;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;
import players.heuristics.CascadiaGreedyAgent;
import players.simple.RandomPlayer;

public class RunCascadia {
  public static void main(String[] args) {
    // Create players for the game
    List<AbstractPlayer> players = new ArrayList<>();

    // Medium skill MCTS player 1 (moderate search)
    BasicMCTSParams mediumParams1 = new BasicMCTSParams();
    mediumParams1.K = 1.4;
    mediumParams1.rolloutLength = 5; 
    mediumParams1.maxTreeDepth = 7;
    mediumParams1.budget = 800;
    mediumParams1.budgetType = PlayerConstants.BUDGET_ITERATIONS;

    // Medium skill MCTS player 2 (moderate search)
    BasicMCTSParams mediumParams2 = new BasicMCTSParams();
    mediumParams2.K = 1.4;
    mediumParams2.rolloutLength = 5; 
    mediumParams2.maxTreeDepth = 7;
    mediumParams2.budget = 800;
    mediumParams2.budgetType = PlayerConstants.BUDGET_ITERATIONS;

    // High skill MCTS player (deeper search with same rollout strategy)
    BasicMCTSParams highParams = new BasicMCTSParams();
    highParams.K = 1.4;
    highParams.rolloutLength = 7; // Keep short - strong heuristic means less rollout needed
    highParams.maxTreeDepth = 10;
    highParams.budget = 1000; // Much more search iterations
    highParams.budgetType = PlayerConstants.BUDGET_ITERATIONS;

    CascadiaGreedyAgent greedyAgent = new CascadiaGreedyAgent();

    players.add(greedyAgent);
    players.add(new BasicMCTSPlayer(mediumParams2));
    players.add(new BasicMCTSPlayer(highParams));

    // Retrieve and print the game trace
    List<GameTrace> traces = new ArrayList<>();

    long masterSeed = 42L;
    for (int i = 0; i < 10; i++) {
      CascadiaParameters params = new CascadiaParameters();
      params.setRandomSeed(masterSeed + i);
      Game game = new CascadiaGame(3, params);
      TraceObserver obs = new TraceObserver(players);
      game.addObserver(obs);
      game.reset(players);

      // Update player names from actual player objects
      if (game instanceof CascadiaGame cascadiaGame) {
        cascadiaGame.updatePlayerNames();
      }

      game.run();
      traces.add(obs.getTrace());

      // Print game summary
      System.out.println("\n=== Game " + (i + 1) + " Complete ===");
      CascadiaGameState state = (CascadiaGameState) game.getGameState();
      for (int p = 0; p < state.getNPlayers(); p++) {
        System.out.println(state.getPlayerName(p) + " Score: " + state.playerScores.get(p));
      }
    }

    GER ger = new GERBuilder(traces).build();
    GERExporter.export(ger, "cascadia-ger.json");
  }
}