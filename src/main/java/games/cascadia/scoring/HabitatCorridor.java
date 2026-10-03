package games.cascadia.scoring;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import games.cascadia.CascadiaGameState;
import games.cascadia.board.HexCoord;
import games.cascadia.board.PlacedHabitatTile;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.HabitatType;

public class HabitatCorridor {

  public int scoreHabitat(PlayerBoard board, HabitatType habitat) {

    Set<HexCoord> visited = new HashSet<>();
    int max = 0;

    for (HexCoord start : board.getTiles().keySet()) {

      if (visited.contains(start))
        continue;

      PlacedHabitatTile tile = board.getTileAt(start);
      boolean hasHabitat = false;
      for (int e = 0; e < 6; e++) {
        if (tile.getHabitatOnWorldEdge(e) == habitat) {
          hasHabitat = true;
          break;
        }
      }
      if (!hasHabitat)
        continue;

      int size = 0;
      Deque<HexCoord> q = new ArrayDeque<>();
      q.add(start);
      visited.add(start);

      while (!q.isEmpty()) {
        HexCoord curr = q.poll();
        size++;

        for (int e = 0; e < 6; e++) {
          HexCoord next = curr.add(HexCoord.EDGE_TO_DIRECTION[e]);
          if (visited.contains(next))
            continue;

          if (connects(curr, next, habitat, board, e)) {
            visited.add(next);
            q.add(next);
          }
        }
      }

      max = Math.max(max, size);
    }

    return max;
  }

  private boolean connects(
      HexCoord a,
      HexCoord b,
      HabitatType habitat,
      PlayerBoard board,
      int edgeFromA) {

    PlacedHabitatTile tileA = board.getTileAt(a);
    PlacedHabitatTile tileB = board.getTileAt(b);
    if (tileB == null)
      return false;

    int opposite = HexCoord.oppositeEdge(edgeFromA);

    return tileA.getHabitatOnWorldEdge(edgeFromA) == habitat
        && tileB.getHabitatOnWorldEdge(opposite) == habitat;
  }

  private int largestCorridor(PlayerBoard board, HabitatType habitat) {
    return scoreHabitat(board, habitat);
  }

  public int[] scoreCorridorMajority(
      HabitatType habitat,
      CascadiaGameState state) {

    int n = state.getNPlayers();
    int[] sizes = new int[n];
    int[] bonus = new int[n];

    for (int p = 0; p < n; p++) {
      sizes[p] = largestCorridor(
          state.getPlayerBoard(p), habitat);
    }

    // SOLO
    if (n == 1) {
      if (sizes[0] >= 7)
        bonus[0] = 2;
      return bonus;
    }

    // TWO PLAYER
    if (n == 2) {
      if (sizes[0] > sizes[1])
        bonus[0] = 2;
      else if (sizes[1] > sizes[0])
        bonus[1] = 2;
      else {
        bonus[0] = 1;
        bonus[1] = 1;
      }
      return bonus;
    }

    // THREE / FOUR PLAYER
    int max = Arrays.stream(sizes).max().orElse(0);
    List<Integer> maxPlayers = new ArrayList<>();

    for (int p = 0; p < n; p++) {
      if (sizes[p] == max)
        maxPlayers.add(p);
    }

    // Tie for largest
    if (maxPlayers.size() >= 2) {
      int points = (maxPlayers.size() == 2) ? 2 : 1;
      for (int p : maxPlayers)
        bonus[p] = points;
      return bonus;
    }

    // Unique largest
    int winner = maxPlayers.get(0);
    bonus[winner] = 3;

    // Find second largest
    int second = -1;
    int secondCount = 0;
    int secondPlayer = -1;

    for (int p = 0; p < n; p++) {
      if (p == winner)
        continue;
      if (sizes[p] > second) {
        second = sizes[p];
        secondCount = 1;
        secondPlayer = p;
      } else if (sizes[p] == second) {
        secondCount++;
      }
    }

    // Award second place only if unique
    if (secondCount == 1 && second > 0) {
      bonus[secondPlayer] = 1;
    }

    return bonus;
  }
}