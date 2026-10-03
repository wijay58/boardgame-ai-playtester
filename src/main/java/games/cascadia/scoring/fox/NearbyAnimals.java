package games.cascadia.scoring.fox;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import games.cascadia.board.HexCoord;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.WildlifeToken;
import games.cascadia.components.WildlifeType;

public class NearbyAnimals {
  public int scoreFoxAdjacency(PlayerBoard board) {

    int score = 0;

    for (Map.Entry<HexCoord, WildlifeToken> e : board.getWildlifeTokens().entrySet()) {

      if (e.getValue().getWildlifeType() == WildlifeType.FOX) {
        score += scoreSingleFox(e.getKey(), board);
      }
    }

    return score;
  }

  private int scoreSingleFox(HexCoord foxCoord, PlayerBoard board) {

    Set<WildlifeType> adjacentTypes = new HashSet<>();

    for (HexCoord n : foxCoord.neighbors()) {

      WildlifeToken token = board.getWildlifeTokenAt(n);
      if (token == null)
        continue;

      adjacentTypes.add(token.getWildlifeType());
    }

    int unique = adjacentTypes.size();
    return Math.min(unique, 5);
  }
}
