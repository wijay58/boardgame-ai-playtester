package games.monopolydeal;

import java.util.ArrayList;
import java.util.List;

import core.AbstractPlayer;
import core.Game;
import core.analytics.GameTrace;
import core.analytics.TraceObserver;
import core.analytics.ger.GER;
import core.analytics.ger.GERBuilder;
import core.analytics.ger.GERExporter;
import games.GameType;
import players.PlayerConstants;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;
import players.simple.RandomPlayer;

public class RunMonopolyDeal {
  public static void main(String[] args) {
    // Create players for the game
    List<AbstractPlayer> players = new ArrayList<>();
    BasicMCTSParams highParams = new BasicMCTSParams();
    highParams.K = 1.4;
    highParams.rolloutLength = 7; // Keep short - strong heuristic means less rollout needed
    highParams.maxTreeDepth = 10;
    highParams.budget = 1000; // Much more search iterations
    highParams.budgetType = PlayerConstants.BUDGET_ITERATIONS;

    // Medium skill MCTS player 2 (moderate search)
    BasicMCTSParams mediumParams2 = new BasicMCTSParams();
    mediumParams2.K = 1.4;
    mediumParams2.rolloutLength = 5; 
    mediumParams2.maxTreeDepth = 7;
    mediumParams2.budget = 800;
    mediumParams2.budgetType = PlayerConstants.BUDGET_ITERATIONS;

    players.add(new RandomPlayer());
    players.add(new BasicMCTSPlayer(mediumParams2));
    players.add(new BasicMCTSPlayer(highParams));

    // Retrieve and print the game trace
    List<GameTrace> traces = new ArrayList<>();

    long masterSeed = 42L;
    for (int i = 0; i < 10; i++) {
      long gameSeed = masterSeed + i;
      Game game = GameType.MonopolyDeal.createGameInstance(3, gameSeed);
      TraceObserver obs = new TraceObserver(players);
      game.addObserver(obs);
      game.reset(players);
      game.run();
      traces.add(obs.getTrace());
    }

    GER ger = new GERBuilder(traces).build();
    GERExporter.export(ger, "monopolydeal-ger.json");
  }
}
