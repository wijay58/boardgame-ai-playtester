package games.cascadia.scoring.salmon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.cascadia.board.HexCoord;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.WildlifeToken;
import games.cascadia.components.WildlifeType;

public class LongRun {
  private static final int[] SALMON_SCORES = { 2, 5, 8, 12, 16, 20, 25 };

  public int scoreSalmonRuns(PlayerBoard board) {

    Set<HexCoord> salmon = getSalmonCoords(board);

    int score = 0;

    for (List<HexCoord> run : findSalmonRuns(salmon)) {

      boolean invalid = false;

      for (HexCoord c : run) {
        if (isBranchingSalmon(c, salmon)) {
          invalid = true;
          break;
        }
      }

      if (invalid)
        continue;

      int len = run.size();
      score += (len > 6) ? 25 : SALMON_SCORES[len-1];
    }

    return score;
  }

  private Set<HexCoord> getSalmonCoords(PlayerBoard board) {

    Set<HexCoord> result = new HashSet<>();

    for (Map.Entry<HexCoord, WildlifeToken> e : board.getWildlifeTokens().entrySet()) {

      if (e.getValue().getWildlifeType() == WildlifeType.SALMON) {
        result.add(e.getKey());
      }
    }

    return result;
  }

  private boolean isBranchingSalmon(
      HexCoord coord,
      Set<HexCoord> salmonSet) {

    int neighbors = 0;

    for (HexCoord n : coord.neighbors()) {
      if (salmonSet.contains(n)) {
        neighbors++;
      }
    }

    return neighbors > 2;
  }

  private List<List<HexCoord>> findSalmonRuns(Set<HexCoord> salmonSet) {

    Set<HexCoord> visited = new HashSet<>();
    List<List<HexCoord>> runs = new ArrayList<>();

    for (HexCoord start : salmonSet) {

      if (visited.contains(start))
        continue;

      for (HexCoord dir : HexCoord.STRAIGHT_DIRECTIONS) {

        List<HexCoord> run = new ArrayList<>();
        HexCoord current = start;

        while (salmonSet.contains(current)) {
          run.add(current);
          visited.add(current);
          current = current.add(dir);
        }

        if (!run.isEmpty()) {
          runs.add(run);
        }
      }
    }

    return runs;
  }
}
