package core.observers;

import core.AbstractGameState;
import core.actions.AbstractAction;

public interface GameObserver {

  default void onGameStart(AbstractGameState state) {
  }

  default void onActionApplied(AbstractGameState state, AbstractAction action) {
  }

  default void onTurnEnd(AbstractGameState state, int playerId) {
  }

  default void onGameEnd(AbstractGameState terminalState) {
  }

  default void onActionSetComputed(AbstractGameState state, int branchingFactor) {
  }
}