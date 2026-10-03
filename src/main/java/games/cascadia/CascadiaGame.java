package games.cascadia;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractParameters;
import core.Game;
import games.GameType;

public class CascadiaGame extends Game {

  public CascadiaGame() {
    this(2);
  }

  public CascadiaGame(int nPlayers) {
    this(nPlayers, new CascadiaParameters());
  }

  public CascadiaGame(int nPlayers, AbstractParameters params) {
    super(GameType.Cascadia,
        GameType.Cascadia.createForwardModel(params, nPlayers),
        GameType.Cascadia.createGameState(params, nPlayers));
  }

  public CascadiaGame(AbstractForwardModel model, AbstractGameState gameState) {
    super(GameType.Cascadia, model, gameState);
  }

  /**
   * Update player names in the game state from the actual player objects.
   * Call this after reset() to sync the names.
   */
  public void updatePlayerNames() {
    if (getPlayers() != null && getGameState() instanceof CascadiaGameState cascadiaState) {
      java.util.List<String> names = getPlayers().stream()
          .map(core.AbstractPlayer::toString)
          .collect(java.util.stream.Collectors.toList());
      if (getGameState().getCoreGameParameters().verbose) {
        System.out.println("CascadiaGame: Updating player names to: " + names);
      }
      cascadiaState.setPlayerNames(names);
    }
  }
}