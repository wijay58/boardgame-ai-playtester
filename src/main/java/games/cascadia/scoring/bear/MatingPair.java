package games.cascadia.scoring.bear;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.cascadia.board.HexCoord;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.WildlifeToken;
import games.cascadia.components.WildlifeType;

public class MatingPair {
  private static final int[] BEAR_PAIR_SCORES = { 4, 11, 19, 27 };

  public int scoreBearPairs(PlayerBoard board) {

    int pairs = countBearPairs(board);

    if (pairs > 3) {
      return 27;
    }

    if (pairs == 0) {
      return 0;
    }

    return BEAR_PAIR_SCORES[pairs-1];
  }

  private Set<HexCoord> getBearCoords(PlayerBoard board) {

    Set<HexCoord> bears = new HashSet<>();

    for (Map.Entry<HexCoord, WildlifeToken> e : board.getWildlifeTokens().entrySet()) {

      if (e.getValue().getWildlifeType() == WildlifeType.BEAR) {
        bears.add(e.getKey());
      }
    }

    return bears;
  }

  private int countBearPairs(PlayerBoard board) {

    Set<HexCoord> bears = getBearCoords(board);
    Set<HexCoord> used = new HashSet<>();

    int pairs = 0;

    for (HexCoord bear : bears) {

      if (used.contains(bear))
        continue;

      List<HexCoord> adjacentBears = new ArrayList<>();

      for (HexCoord n : bear.neighbors()) {
        if (bears.contains(n)) {
          adjacentBears.add(n);
        }
      }

      // Must have exactly one adjacent bear
      if (adjacentBears.size() != 1)
        continue;

      HexCoord mate = adjacentBears.get(0);

      // Mate must also have exactly one adjacent bear (this one)
      List<HexCoord> mateAdj = new ArrayList<>();
      for (HexCoord n : mate.neighbors()) {
        if (bears.contains(n)) {
          mateAdj.add(n);
        }
      }

      if (mateAdj.size() != 1)
        continue;

      // Valid isolated pair
      pairs++;
      used.add(bear);
      used.add(mate);
    }

    return pairs;
  }
}
