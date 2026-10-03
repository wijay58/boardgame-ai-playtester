package games.cascadia;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import games.cascadia.board.HexCoord;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.HabitatTile;
import games.cascadia.components.HabitatType;
import games.cascadia.components.ScoringCard;
import games.cascadia.components.WildlifeToken;
import games.cascadia.components.WildlifeType;
import games.cascadia.market.MarketPair;
import games.cascadia.scoring.HabitatCorridor;
import games.cascadia.scoring.bear.MatingPair;
import games.cascadia.scoring.elk.Lines;
import games.cascadia.scoring.fox.NearbyAnimals;
import games.cascadia.scoring.hawk.Solitary;
import games.cascadia.scoring.salmon.LongRun;

public class CascadiaGameState extends AbstractGameState {

  // Shared state
  public Deck<HabitatTile> habitatDeck;
  public Deck<WildlifeToken> wildlifeDeck;
  public List<MarketPair> market;
  public Map<WildlifeType, ScoringCard> scoringCards;
  public List<Integer> playerScores;
  public GamePhase phase;
  public int natureTokensLeft = 20;

  // Per-player state
  public List<PlayerBoard> playerBoards;
  public List<Integer> natureTokens;
  private List<String> playerNames;

  private MarketPair draftedPair;
  private HexCoord lastPlacedTileCoord;

  public CascadiaGameState(AbstractParameters gameParameters, int nPlayers) {
    super(gameParameters, nPlayers);
    this.playerScores = new ArrayList<>();
    this.playerNames = new ArrayList<>();
    for (int i = 0; i < nPlayers; i++) {
      this.playerScores.add(0);
      this.playerNames.add("Player " + i);
    }
  }

  public void setPlayerNames(List<String> names) {
    if (names != null && names.size() == getNPlayers()) {
      this.playerNames = new ArrayList<>(names);
    }
  }

  public void setScore(int playerId, int score) {
    this.playerScores.set(playerId, score);
  }

  public HexCoord getLastPlacedTileCoord() {
    return lastPlacedTileCoord;
  }

  public int getNatureTokensLeft() {
    return natureTokensLeft;
  }

  public void setNatureTokensLeft(int tokens) {
    this.natureTokensLeft = tokens;
  }

  public void setLastPlacedTileCoord(HexCoord coord) {
    this.lastPlacedTileCoord = coord;
  }

  public MarketPair getDraftedPair() {
    return draftedPair;
  }

  public void setDraftedPair(MarketPair draftedPair) {
    this.draftedPair = draftedPair;
  }

  public Deck<HabitatTile> getHabitatDeck() {
    return habitatDeck;
  }

  public Deck<WildlifeToken> getWildlifeDeck() {
    return wildlifeDeck;
  }

  public List<MarketPair> getMarket() {
    return market;
  }

  public Map<WildlifeType, ScoringCard> getScoringCards() {
    return scoringCards;
  }

  public GamePhase getPhase() {
    return phase;
  }

  public PlayerBoard getPlayerBoard(int playerId) {
    return playerBoards.get(playerId);
  }

  public int getNatureTokens(int playerId) {
    return natureTokens.get(playerId);
  }

  public void addNatureToken(int playerId) {
    natureTokens.set(playerId, natureTokens.get(playerId) + 1);
  }

  public void removeNatureToken(int playerId) {
    natureTokens.set(playerId, natureTokens.get(playerId) - 1);
  }

  public void setPhase(GamePhase phase) {
    this.phase = phase;
  }

  public String getPlayerName(int playerId) {
    if (playerId < 0 || playerId >= getNPlayers()) {
      return "Unknown Player";
    }

    // Try to get the actual player name from the game if available
    for (evaluation.listeners.IGameListener listener : listeners) {
      if (listener.getGame() != null && listener.getGame().getPlayers() != null
          && playerId < listener.getGame().getPlayers().size()) {
        return listener.getGame().getPlayers().get(playerId).toString();
      }
    }

    // Fallback to stored name
    return playerNames.get(playerId);
  }

  public List<String> getAllPlayerNames() {
    List<String> names = new ArrayList<>();
    for (int i = 0; i < getNPlayers(); i++) {
      names.add(getPlayerName(i));
    }
    return names;
  }

  // ---------- TAG Required Overrides ----------

  @Override
  protected games.GameType _getGameType() {
    return games.GameType.Cascadia;
  }

  @Override
  protected double _getHeuristicScore(int playerId) {
    // Use actual score computation for heuristic evaluation
    // This allows MCTS to distinguish between good and bad positions
    return computeScore(playerId);
  }

  @Override
  public double getGameScore(int playerId) {
    return computeScore(playerId);
  }

  @Override
  public double getInteractionScore(int playerId) {
    PlayerBoard board = getPlayerBoard(playerId);
    int score = 0;

    Solitary hawkScorer = new Solitary();
    LongRun salmonScorer = new LongRun();
    MatingPair bearScorer = new MatingPair();
    NearbyAnimals foxScorer = new NearbyAnimals();
    Lines elkScorer = new Lines();

    score += hawkScorer.scoreHawkSolitary(board);
    score += salmonScorer.scoreSalmonRuns(board);
    score += bearScorer.scoreBearPairs(board);
    score += foxScorer.scoreFoxAdjacency(board);
    score += elkScorer.scoreElkLines(board);

    score += scoreHabitats(board);
    score += getNatureTokens(playerId);

    return score;
  }

  public int computeScore(int playerId) {

    PlayerBoard board = getPlayerBoard(playerId);
    int score = 0;

    // Animal scoring
    Solitary hawkScorer = new Solitary();
    LongRun salmonScorer = new LongRun();
    MatingPair bearScorer = new MatingPair();
    NearbyAnimals foxScorer = new NearbyAnimals();
    Lines elkScorer = new Lines();

    score += hawkScorer.scoreHawkSolitary(board);
    score += salmonScorer.scoreSalmonRuns(board);
    score += bearScorer.scoreBearPairs(board);
    score += foxScorer.scoreFoxAdjacency(board);
    score += elkScorer.scoreElkLines(board);

    // Habitat scoring
    score += scoreHabitats(board);
    score += scoreHabitatsMajority(board);

    // Nature tokens
    score += getNatureTokens(playerId);

    return score;
  }

  private int scoreHabitats(PlayerBoard board) {
    int score = 0;
    HabitatCorridor corridorScorer = new HabitatCorridor();
    for (HabitatType h : HabitatType.values()) {
      score += corridorScorer.scoreHabitat(board, h);
    }
    return score;
  }

  private int scoreHabitatsMajority(PlayerBoard board) {
    int score = 0;
    HabitatCorridor corridorScorer = new HabitatCorridor();
    for (HabitatType h : HabitatType.values()) {
      int[] habitatBonus = corridorScorer.scoreCorridorMajority(h, this);
      for (int p = 0; p < getNPlayers(); p++) {
        if (board == getPlayerBoard(p)) {
          score += habitatBonus[p];
        }
      }
    }
    return score;
  }

  @Override
  protected List<Component> _getAllComponents() {
    List<Component> components = new ArrayList<>();

    components.add(habitatDeck);
    components.add(wildlifeDeck);

    for (PlayerBoard board : playerBoards) {
      components.add(board);
    }

    for (ScoringCard card : scoringCards.values()) {
      components.add(card);
    }

    return components;
  }

  @Override
  protected AbstractGameState _copy(int playerId) {
    CascadiaGameState copy = new CascadiaGameState(gameParameters.copy(), getNPlayers());

    copy.habitatDeck = habitatDeck.copy();
    copy.wildlifeDeck = wildlifeDeck.copy();

    copy.market = new ArrayList<>();
    for (MarketPair pair : market) {
      copy.market.add(new MarketPair(
          (HabitatTile) pair.getHabitatTile().copy(),
          (WildlifeToken) pair.getWildlifeToken().copy()));
    }

    copy.scoringCards = new EnumMap<>(WildlifeType.class);
    for (Map.Entry<WildlifeType, ScoringCard> entry : scoringCards.entrySet()) {
      copy.scoringCards.put(entry.getKey(),
          (ScoringCard) entry.getValue().copy());
    }

    copy.playerBoards = new ArrayList<>();
    for (PlayerBoard board : playerBoards) {
      copy.playerBoards.add((PlayerBoard) board.copy());
    }

    copy.natureTokens = new ArrayList<>(natureTokens);
    copy.playerScores = new ArrayList<>(playerScores);
    copy.playerNames = new ArrayList<>(playerNames);
    copy.phase = phase;

    copy.draftedPair = draftedPair == null ? null
        : new MarketPair(
            (HabitatTile) draftedPair.getHabitatTile().copy(),
            (WildlifeToken) draftedPair.getWildlifeToken().copy());

    copy.lastPlacedTileCoord = lastPlacedTileCoord;

    return copy;
  }

  @Override
  protected boolean _equals(Object o) {
    return o instanceof CascadiaGameState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        habitatDeck,
        wildlifeDeck,
        market,
        scoringCards,
        playerBoards,
        natureTokens,
        phase);
  }

  public List<AbstractAction> getAvailableActions() {
    return new CascadiaForwardModel()._computeAvailableActions(this);
  }

}