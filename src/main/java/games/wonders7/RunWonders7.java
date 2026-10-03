package games.wonders7;

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

public class RunWonders7 {
  public static void main(String[] args) {
    List<AbstractPlayer> players = new ArrayList<>();

    // Random player
    players.add(new RandomPlayer());

    // Medium MCTS
    BasicMCTSParams medParams = new BasicMCTSParams();
    medParams.K = 1.4;
    medParams.rolloutLength = 5;
    medParams.maxTreeDepth = 7;
    medParams.budget = 800;
    medParams.budgetType = PlayerConstants.BUDGET_ITERATIONS;
    // players.add(new RandomPlayer());

    // High MCTS
    BasicMCTSParams highParams = new BasicMCTSParams();
    highParams.K = 1.4;
    highParams.rolloutLength = 7;
    highParams.maxTreeDepth = 10;
    highParams.budget = 1000;
    highParams.budgetType = PlayerConstants.BUDGET_ITERATIONS;
    players.add(new BasicMCTSPlayer(medParams));
    players.add(new BasicMCTSPlayer(highParams));

    List<GameTrace> traces = new ArrayList<>();

    long masterSeed = 42L;
    for (int i = 0; i < 10; i++) {
      long gameSeed = masterSeed + i;
      Game game = GameType.Wonders7.createGameInstance(3, gameSeed);
      TraceObserver obs = new TraceObserver(players);
      game.addObserver(obs);
      game.reset(players);
      game.run();
      traces.add(obs.getTrace());
    }

    GER ger = new GERBuilder(traces).build();
    GERExporter.export(ger, "wonders7-ger.json");
  }
}
