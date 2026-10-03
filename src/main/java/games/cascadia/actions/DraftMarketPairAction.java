package games.cascadia.actions;

import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cascadia.CascadiaGameState;
import games.cascadia.GamePhase;
import games.cascadia.components.HabitatTile;
import games.cascadia.components.WildlifeToken;
import games.cascadia.market.MarketPair;

public class DraftMarketPairAction extends AbstractAction {

  private final int marketIndex;

  public DraftMarketPairAction(int marketIndex) {
    this.marketIndex = marketIndex;
  }

  @Override
  public boolean execute(AbstractGameState gameState) {
    CascadiaGameState state = (CascadiaGameState) gameState;

    MarketPair pair = state.getMarket().remove(marketIndex);
    state.setDraftedPair(pair);

    // Refill market
    HabitatTile newTile = state.getHabitatDeck().draw();
    WildlifeToken newToken = state.getWildlifeDeck().draw();

    if (newTile != null) {
      state.getMarket().add(new MarketPair(newTile, newToken));
    }

    // check if all 4 wildlife tokens are the same
    List<MarketPair> marketArray = state.getMarket();
    if (marketArray.size() == 4) {
      WildlifeToken firstToken = marketArray.get(0).getWildlifeToken();
      boolean allSame = marketArray.stream().allMatch(mp -> mp.getWildlifeToken().equals(firstToken));

      // If all wildlife tokens are the same, replace them with new ones
      while (allSame) {
        for (int i = 0; i < marketArray.size(); i++) {
          MarketPair oldPair = marketArray.get(i);
          WildlifeToken newWildlifeToken = state.getWildlifeDeck().draw();
          if (newWildlifeToken != null) {
            marketArray.set(i, new MarketPair(oldPair.getHabitatTile(), newWildlifeToken));
          }
        }

        // Check again if all are the same
        WildlifeToken checkToken = marketArray.get(0).getWildlifeToken();
        allSame = marketArray.stream().allMatch(mp -> mp.getWildlifeToken().equals(checkToken));
      }
    }

    state.setPhase(GamePhase.PLACE_TILE);
    return true;
  }

  @Override
  public DraftMarketPairAction copy() {
    return new DraftMarketPairAction(marketIndex);
  }

  @Override
  public String getString(core.AbstractGameState gameState) {
    return toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DraftMarketPairAction))
      return false;
    return marketIndex == ((DraftMarketPairAction) o).marketIndex;
  }

  @Override
  public int hashCode() {
    return marketIndex;
  }

  @Override
  public String toString() {
    return "DraftMarketPair(" + marketIndex + ")";
  }
}