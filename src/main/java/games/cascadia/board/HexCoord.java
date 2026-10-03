package games.cascadia.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HexCoord {

  public final int q;
  public final int r;

  public HexCoord(int q, int r) {
    this.q = q;
    this.r = r;
  }

  public static final HexCoord[] EDGE_TO_DIRECTION = {
      new HexCoord(1, 0), // edge 0
      new HexCoord(1, -1), // edge 1
      new HexCoord(0, -1), // edge 2
      new HexCoord(-1, 0), // edge 3
      new HexCoord(-1, 1), // edge 4
      new HexCoord(0, 1) // edge 5
  };

  public static final List<HexCoord> STRAIGHT_DIRECTIONS = List.of(
      new HexCoord(1, 0),
      new HexCoord(0, 1),
      new HexCoord(1, -1));

  public static final List<HexCoord> NEIGHBOR_DIRECTIONS = List.of(
      new HexCoord(1, 0),
      new HexCoord(-1, 0),
      new HexCoord(0, 1),
      new HexCoord(0, -1),
      new HexCoord(1, -1),
      new HexCoord(-1, 1));

  public List<HexCoord> neighbors() {
    List<HexCoord> result = new ArrayList<>(6);
    for (HexCoord d : NEIGHBOR_DIRECTIONS) {
      result.add(add(d));
    }
    return result;
  }

  public HexCoord add(HexCoord other) {
    return new HexCoord(this.q + other.q, this.r + other.r);
  }

  public static int oppositeEdge(int edge) {
    return (edge + 3) % 6;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof HexCoord))
      return false;
    HexCoord hexCoord = (HexCoord) o;
    return q == hexCoord.q && r == hexCoord.r;
  }

  @Override
  public int hashCode() {
    return Objects.hash(q, r);
  }
}