package games.cascadia.scoring.hawk;

import java.util.Map;

import games.cascadia.board.HexCoord;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.WildlifeToken;
import games.cascadia.components.WildlifeType;

public class Solitary {
  private static final int[] HAWK_SOLITARY_SCORES = { 2, 5, 8, 11, 14, 18, 22, 26 };

  public int scoreHawkSolitary(PlayerBoard board) {

    int solitaryHawks = countSolitaryHawks(board);

    if (solitaryHawks > 7) {
      return 26;
    }

    if (solitaryHawks == 0) {
      return 0;
    }

    return HAWK_SOLITARY_SCORES[solitaryHawks - 1];
  }

  private int countSolitaryHawks(PlayerBoard board) {

    int count = 0;

    for (Map.Entry<HexCoord, WildlifeToken> entry : board.getWildlifeTokens().entrySet()) {

      if (entry.getValue().getWildlifeType() != WildlifeType.HAWK) {
        continue;
      }

      HexCoord coord = entry.getKey();
      boolean adjacentHawk = false;

      for (HexCoord neighbor : coord.neighbors()) {
        WildlifeToken neighborToken = board.getWildlifeTokenAt(neighbor);

        if (neighborToken != null &&
            neighborToken.getWildlifeType() == WildlifeType.HAWK) {
          adjacentHawk = true;
          break;
        }
      }

      if (!adjacentHawk) {
        count++;
      }
    }

    return count;
  }
}
