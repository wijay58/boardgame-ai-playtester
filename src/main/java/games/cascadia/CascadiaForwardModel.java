package games.cascadia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.cascadia.actions.DraftMarketPairAction;
import games.cascadia.actions.PlaceTileAction;
import games.cascadia.actions.PlaceWildlifeAction;
import games.cascadia.actions.UseNatureTokenAction;
import games.cascadia.board.HexCoord;
import games.cascadia.board.PlacedHabitatTile;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.HabitatTile;
import games.cascadia.components.HabitatType;
import games.cascadia.components.ScoringCard;
import games.cascadia.components.ScoringPattern;
import games.cascadia.components.WildlifeToken;
import games.cascadia.components.WildlifeType;
import games.cascadia.market.MarketPair;
import games.cascadia.setup.HabitatTileConfig;
import games.cascadia.setup.HabitatTileDTO;
import games.cascadia.setup.HabitatTileLoader;
import games.cascadia.setup.StartingGroupDTO;
import games.cascadia.setup.StartingHexConfig;
import games.cascadia.setup.StartingHexLoader;
import games.cascadia.setup.StartingTileDTO;

public class CascadiaForwardModel extends AbstractForwardModel {

  @Override
  protected void _setup(AbstractGameState firstState) {

    CascadiaGameState state = (CascadiaGameState) firstState;
    int nPlayers = state.getNPlayers();
    int habitatTilesNeeded = nPlayers * 20 + 3;

    String startHexConfig = "data/cascadia/starting_hexes.json";
    StartingHexConfig hexConfig = StartingHexLoader.load(startHexConfig);

    String habitatTileConfig = "data/cascadia/habitat_tiles.json";
    HabitatTileConfig tileConfig = HabitatTileLoader.load(habitatTileConfig);

    // Shuffle starting groups for random board assignment
    List<StartingGroupDTO> availableGroups = new ArrayList<>(hexConfig.startingGroups);
    Collections.shuffle(availableGroups, new Random());

    // ---------- Create decks ----------
    Deck<HabitatTile> habitatDeck = new Deck<>("Habitat Deck", core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
    Deck<WildlifeToken> wildlifeDeck = new Deck<>("Wildlife Deck", core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

    int componentId = 10;

    // Load habitat tiles from configuration
    for (HabitatTileDTO tileDTO : tileConfig.habitatTiles) {
      Set<HabitatType> habitats = new HashSet<>(tileDTO.habitats);
      Set<WildlifeType> allowedWildlife = new HashSet<>(tileDTO.allowedWildlife);

      habitatDeck.add(new HabitatTile(
          componentId++,
          habitats,
          allowedWildlife,
          tileDTO.baseEdgeHabitats,
          tileDTO.hasNatureMark));
    }

    habitatDeck.shuffle(new Random());

    while (habitatDeck.getSize() > habitatTilesNeeded) {
      habitatDeck.remove(habitatDeck.getSize() - 1);
    }

    if (state.getCoreGameParameters().verbose) {
      System.out.println("Habitat deck size for " + nPlayers + " players: " + habitatDeck.getSize());
    }

    // Create 20 tokens for each wildlife type (100 total)
    for (WildlifeType type : WildlifeType.values()) {
      for (int i = 0; i < 20; i++) {
        wildlifeDeck.add(new WildlifeToken(
            componentId++,
            type));
      }
    }

    wildlifeDeck.shuffle(new Random());

    // ---------- Scoring cards ----------
    Map<WildlifeType, ScoringCard> scoringCards = new EnumMap<>(WildlifeType.class);

    for (WildlifeType type : WildlifeType.values()) {
      scoringCards.put(type,
          new ScoringCard(componentId++, type, ScoringPattern.HAWK_SOLITARY));
    }

    // ---------- Player boards ----------
    List<PlayerBoard> boards = new ArrayList<>();
    List<Integer> natureTokens = new ArrayList<>();

    for (int p = 0; p < nPlayers; p++) {

      PlayerBoard board = new PlayerBoard(componentId++);
      natureTokens.add(0);

      // Assign a unique random board to this player
      StartingGroupDTO playerGroup = availableGroups.get(p % availableGroups.size());

      for (StartingTileDTO tileDTO : playerGroup.tiles) {

        Set<HabitatType> habitats = tileDTO.habitats.stream()
            .map(HabitatType::valueOf)
            .collect(Collectors.toSet());

        Set<WildlifeType> allowedWildlife = tileDTO.allowedWildlife.stream()
            .map(WildlifeType::valueOf)
            .collect(Collectors.toSet());

        HabitatTile tile = new HabitatTile(
            componentId++,
            habitats,
            allowedWildlife,
            tileDTO.baseEdgeHabitats,
            tileDTO.natureMark);

        PlacedHabitatTile placedTile = new PlacedHabitatTile(
            tile,
            0);

        board.placeTile(
            new HexCoord(tileDTO.q, tileDTO.r),
            placedTile);
      }

      boards.add(board);
    }

    // ---------- Market ----------
    List<MarketPair> market = new ArrayList<>();

    // Standard Cascadia uses 4 market pairs
    for (int i = 0; i < 4; i++) {
      HabitatTile tile = habitatDeck.draw();
      WildlifeToken token = wildlifeDeck.draw();

      if (tile == null || token == null) {
        throw new IllegalStateException("Deck exhausted during market setup");
      }

      market.add(new MarketPair(tile, token));
    }

    // ---------- Assign to state ----------
    state.setPhase(GamePhase.DRAFT);
    state.setFirstPlayer(0);

    state.habitatDeck = habitatDeck;
    state.wildlifeDeck = wildlifeDeck;
    state.market = market;
    state.scoringCards = scoringCards;
    state.playerBoards = boards;
    state.natureTokens = natureTokens;
  }

  @Override
  protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {

    CascadiaGameState state = (CascadiaGameState) gameState;

    if (state.getPhase() == GamePhase.PLACE_TILE) {

      PlayerBoard board = state.getPlayerBoard(state.getCurrentPlayer());

      Set<HexCoord> candidatePositions = new HashSet<>();

      // For every existing tile, add its empty neighbors
      for (HexCoord coord : board.getTiles().keySet()) {
        for (HexCoord neighbor : coord.neighbors()) {
          if (board.getTileAt(neighbor) == null) {
            candidatePositions.add(neighbor);
          }
        }
      }

      List<AbstractAction> actions = new ArrayList<>();
      for (HexCoord coord : candidatePositions) {
        for (int r = 0; r < 6; r++) {
          actions.add(new PlaceTileAction(coord, r));
        }
      }

      return actions;
    }

    if (state.getPhase() == GamePhase.PLACE_WILDLIFE) {

      PlayerBoard board = state.getPlayerBoard(state.getCurrentPlayer());

      WildlifeToken token = state.getDraftedPair().getWildlifeToken();

      List<AbstractAction> actions = new ArrayList<>();

      for (Map.Entry<HexCoord, PlacedHabitatTile> entry : board.getTiles().entrySet()) {

        HexCoord coord = entry.getKey();
        PlacedHabitatTile tile = entry.getValue();

        // Must be empty (check board's wildlife tokens map) and must have allowed
        // wildlife
        if (board.getWildlifeTokenAt(coord) != null || !tile.getAllowedWildlife().contains(token.getWildlifeType()))
          continue;

        actions.add(
            new PlaceWildlifeAction(coord, token));
      }

      // if token cannot be placed, must skip turn and lose token
      if (actions.isEmpty()) {
        actions.add(new PlaceWildlifeAction(null, token));
      }

      return actions;
    }

    if (state.getPhase() != GamePhase.DRAFT) {
      return List.of();
    }

    List<AbstractAction> actions = new ArrayList<>();
    // Normal draft actions
    for (int i = 0; i < state.getMarket().size(); i++) {
      actions.add(new DraftMarketPairAction(i));
    }

    // Nature token cross-pair actions
    if (state.getNatureTokens(state.getCurrentPlayer()) > 0) {
      for (int i = 0; i < state.getMarket().size(); i++) {
        for (int j = 0; j < state.getMarket().size(); j++) {
          if (i == j)
            continue;
          actions.add(new UseNatureTokenAction(i, j));
        }
      }
    }

    return actions;
  }

  @Override
  protected void endGame(AbstractGameState gameState) {
    CascadiaGameState state = (CascadiaGameState) gameState;

    // Compute scores for all players
    for (int p = 0; p < state.getNPlayers(); p++) {
      int score = state.computeScore(p);
      state.setScore(p, score);
      // Only print for real games, not MCTS simulations
      if (state.getCoreGameParameters().verbose) {
        System.out.println("Player " + state.getPlayerName(p) + " Score: " + score);
      }
    }

    // Call parent to set game status and determine winner based on scores
    super.endGame(gameState);
  }

  @Override
  protected void _next(AbstractGameState gameState, AbstractAction action) {
    CascadiaGameState state = (CascadiaGameState) gameState;
    action.execute(state);

    // Check for game over after action execution
    if (state.getHabitatDeck().getSize() == 0) {
      if (state.getCoreGameParameters().verbose) {
        System.out.println("Deck(s) empty - ending game");
      }
      endGame(state);
    }
  }

  @Override
  protected void endPlayerTurn(AbstractGameState gameState) {
    // End player turn logic here
  }
}