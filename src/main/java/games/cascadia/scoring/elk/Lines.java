package games.cascadia.scoring.elk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.cascadia.board.HexCoord;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.WildlifeToken;
import games.cascadia.components.WildlifeType;

public class Lines {

  private static final int[] ELK_LINE_SCORES = { 0, 2, 5, 9, 13 };

  public int scoreElkLines(PlayerBoard board) {

    Set<HexCoord> elks = getElkCoords(board);
    List<List<HexCoord>> allLines = findElkLines(elks);
    List<List<HexCoord>> lines = selectElkLines(allLines);

    int score = 0;

    for (List<HexCoord> line : lines) {
      int len = Math.min(line.size(), 4);
      score += ELK_LINE_SCORES[len];
    }

    return score;
  }

  private Set<HexCoord> getElkCoords(PlayerBoard board) {

    Set<HexCoord> elks = new HashSet<>();

    for (Map.Entry<HexCoord, WildlifeToken> e : board.getWildlifeTokens().entrySet()) {

      if (e.getValue().getWildlifeType() == WildlifeType.ELK) {
        elks.add(e.getKey());
      }
    }

    return elks;
  }

  private List<List<HexCoord>> findElkLines(Set<HexCoord> elks) {

    List<List<HexCoord>> lines = new ArrayList<>();

    for (HexCoord start : elks) {
      for (HexCoord dir : HexCoord.STRAIGHT_DIRECTIONS) {

        // only start lines from minimal end
        HexCoord prev = start.add(new HexCoord(-dir.q, -dir.r));
        if (elks.contains(prev))
          continue;

        List<HexCoord> line = new ArrayList<>();
        HexCoord curr = start;

        while (elks.contains(curr)) {
          line.add(curr);
          curr = curr.add(dir);
        }

        if (!line.isEmpty()) {
          lines.add(line);
        }
      }
    }

    return lines;
  }

  private List<List<HexCoord>> selectElkLines(List<List<HexCoord>> allLines) {

    allLines.sort((a, b) -> Integer.compare(b.size(), a.size()));

    Set<HexCoord> used = new HashSet<>();
    List<List<HexCoord>> selected = new ArrayList<>();

    for (List<HexCoord> line : allLines) {

      boolean overlaps = false;
      for (HexCoord c : line) {
        if (used.contains(c)) {
          overlaps = true;
          break;
        }
      }

      if (!overlaps) {
        selected.add(line);
        used.addAll(line);
      }
    }

    return selected;
  }
}
