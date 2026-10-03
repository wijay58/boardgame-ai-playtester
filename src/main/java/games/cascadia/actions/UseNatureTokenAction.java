package games.cascadia.actions;

import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cascadia.CascadiaGameState;
import games.cascadia.components.HabitatTile;
import games.cascadia.components.WildlifeToken;
import games.cascadia.market.MarketPair;

public class UseNatureTokenAction extends AbstractAction {

  private final int habitatIndex; // which tile in market
  private final int wildlifeIndex; // which token in market

  public UseNatureTokenAction(int habitatIndex, int wildlifeIndex) {
    this.habitatIndex = habitatIndex;
    this.wildlifeIndex = wildlifeIndex;
  }

  @Override
  public boolean execute(AbstractGameState gs) {
    CascadiaGameState state = (CascadiaGameState) gs;
    int player = state.getCurrentPlayer();

    // Validate indices are within bounds
    if (habitatIndex < 0 || habitatIndex >= state.getMarket().size() ||
        wildlifeIndex < 0 || wildlifeIndex >= state.getMarket().size()) {
      return false; // Invalid market indices
    }

    // Spend a nature token
    state.removeNatureToken(player);

    MarketPair habitatPair = state.getMarket().get(habitatIndex);
    MarketPair wildlifePair = state.getMarket().get(wildlifeIndex);

    HabitatTile habitat = habitatPair.getHabitatTile();
    WildlifeToken wildlife = wildlifePair.getWildlifeToken();

    // Create cross-market pair and set as drafted
    MarketPair draftedPair = new MarketPair(habitat, wildlife);
    state.setDraftedPair(draftedPair);

    // remove and refill the market
    refillTheMarket(state);

    // Transition to tile placement phase
    state.setPhase(games.cascadia.GamePhase.PLACE_TILE);

    return true;
  }

  private void refillTheMarket(CascadiaGameState state) {
    List<MarketPair> market = state.getMarket();

    // Remove higher index first to avoid shifting
    int first = Math.max(habitatIndex, wildlifeIndex);
    int second = Math.min(habitatIndex, wildlifeIndex);

    MarketPair p1 = market.remove(first);
    MarketPair p2 = market.remove(second);

    HabitatTile leftoverHabitat = (habitatIndex == first ? p2.getHabitatTile() : p1.getHabitatTile());

    WildlifeToken leftoverWildlife = (wildlifeIndex == first ? p2.getWildlifeToken() : p1.getWildlifeToken());

    HabitatTile newHabitat = state.getHabitatDeck().draw();
    WildlifeToken newWildlife = state.getWildlifeDeck().draw();

    market.add(new MarketPair(leftoverHabitat, newWildlife));
    market.add(new MarketPair(newHabitat, leftoverWildlife));
  }

  @Override
  public String getString(AbstractGameState gameState) {
    return "Use Nature Token: Habitat[" + habitatIndex + "] + Wildlife[" + wildlifeIndex + "]";
  }

  @Override
  public AbstractAction copy() {
    return new UseNatureTokenAction(habitatIndex, wildlifeIndex);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof UseNatureTokenAction))
      return false;
    UseNatureTokenAction other = (UseNatureTokenAction) obj;
    return habitatIndex == other.habitatIndex && wildlifeIndex == other.wildlifeIndex;
  }

  @Override
  public int hashCode() {
    return 31 * habitatIndex + wildlifeIndex;
  }
}
