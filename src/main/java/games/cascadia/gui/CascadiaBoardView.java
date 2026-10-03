package games.cascadia.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import core.actions.AbstractAction;
import games.cascadia.CascadiaGameState;
import games.cascadia.actions.PlaceWildlifeAction;
import games.cascadia.board.HexCoord;
import games.cascadia.board.PlacedHabitatTile;
import games.cascadia.board.PlayerBoard;
import games.cascadia.components.HabitatType;
import games.cascadia.components.WildlifeType;
import games.cascadia.market.MarketPair;
import gui.IScreenHighlight;

public class CascadiaBoardView extends JComponent implements IScreenHighlight {
  CascadiaGameState gs;

  private int hexRadius = 30;
  private int hexWidth;
  private int hexHeight;
  private int marketPairWidth = 120;
  private int marketPairHeight = 80;

  // Color scheme for habitat types
  Map<HabitatType, Color> habitatColors = new HashMap<HabitatType, Color>() {
    {
      put(HabitatType.MOUNTAIN, new Color(139, 137, 137));
      put(HabitatType.FOREST, new Color(34, 139, 34));
      put(HabitatType.PRAIRIE, new Color(238, 203, 173));
      put(HabitatType.WETLANDS, new Color(64, 164, 223));
      put(HabitatType.RIVER, new Color(100, 149, 237));
    }
  };

  // Wildlife colors for tokens
  Map<WildlifeType, Color> wildlifeColors = new HashMap<WildlifeType, Color>() {
    {
      put(WildlifeType.BEAR, new Color(101, 67, 33));
      put(WildlifeType.ELK, new Color(160, 82, 45));
      put(WildlifeType.SALMON, new Color(250, 128, 114));
      put(WildlifeType.HAWK, new Color(105, 105, 105));
      put(WildlifeType.FOX, new Color(255, 140, 0));
    }
  };

  private Set<HexCoord> highlightedHexes = new HashSet<>();

  public CascadiaBoardView(CascadiaGameState gs) {
    this.gs = gs;
    calculateHexDimensions();
    int width = 1000;
    int height = 600;
    this.setPreferredSize(new Dimension(width, height));
    this.setMinimumSize(new Dimension(width, height));
  }

  private void calculateHexDimensions() {
    hexWidth = (int) (Math.sqrt(3) * hexRadius);
    hexHeight = hexRadius * 2;
  }

  public void updateGameState(CascadiaGameState gameState) {
    this.gs = gameState;
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Clear background
    g2d.setColor(new Color(245, 245, 245));
    g2d.fillRect(0, 0, getWidth(), getHeight());

    // Draw market at the top
    drawMarket(g2d);

    // Draw current player's board in the center
    if (gs != null && gs.getCurrentPlayer() >= 0 && gs.getCurrentPlayer() < gs.getNPlayers()) {
      drawPlayerBoard(g2d, gs.getCurrentPlayer());
    }
  }

  private void drawMarket(Graphics2D g2d) {
    if (gs == null || gs.getMarket() == null)
      return;

    g2d.setColor(Color.BLACK);
    g2d.drawString("Market", 20, 30);

    List<MarketPair> market = gs.getMarket();
    int x = 20;
    int y = 40;

    for (int i = 0; i < market.size(); i++) {
      MarketPair pair = market.get(i);
      drawMarketPair(g2d, pair, x, y);
      x += marketPairWidth + 10;
    }
  }

  private void drawMarketPair(Graphics2D g2d, MarketPair pair, int x, int y) {
    if (pair == null)
      return;

    // Draw tile as hexagon
    int tileRadius = 25;
    int tileCenterX = x + tileRadius;
    int tileCenterY = y + tileRadius + 10;

    Set<HabitatType> habitats = pair.getHabitatTile().getHabitats();
    if (habitats != null && !habitats.isEmpty()) {
      Polygon hex = createHexagon(tileCenterX, tileCenterY, tileRadius);

      if (habitats.size() == 1) {
        // Single habitat - fill entire hex
        HabitatType habitat = habitats.iterator().next();
        Color color = habitatColors.getOrDefault(habitat, Color.LIGHT_GRAY);
        g2d.setColor(color);
        g2d.fillPolygon(hex);
      } else if (habitats.size() == 2) {
        // Dual habitat - split hex vertically
        HabitatType[] habitatArray = habitats.toArray(new HabitatType[0]);

        // Calculate hex vertices
        int[] xPoints = new int[6];
        int[] yPoints = new int[6];
        for (int i = 0; i < 6; i++) {
          double angle = Math.PI / 3 * i - Math.PI / 6;
          xPoints[i] = (int) (tileCenterX + tileRadius * Math.cos(angle));
          yPoints[i] = (int) (tileCenterY + tileRadius * Math.sin(angle));
        }

        // Left half
        Polygon leftHalf = new Polygon();
        leftHalf.addPoint(xPoints[5], yPoints[5]);
        leftHalf.addPoint(xPoints[4], yPoints[4]);
        leftHalf.addPoint(xPoints[3], yPoints[3]);
        leftHalf.addPoint(xPoints[2], yPoints[2]);
        leftHalf.addPoint(tileCenterX, yPoints[2]);
        leftHalf.addPoint(tileCenterX, yPoints[5]);

        Color color1 = habitatColors.getOrDefault(habitatArray[0], Color.LIGHT_GRAY);
        g2d.setColor(color1);
        g2d.fillPolygon(leftHalf);

        // Right half
        Polygon rightHalf = new Polygon();
        rightHalf.addPoint(tileCenterX, yPoints[5]);
        rightHalf.addPoint(tileCenterX, yPoints[2]);
        rightHalf.addPoint(xPoints[2], yPoints[2]);
        rightHalf.addPoint(xPoints[1], yPoints[1]);
        rightHalf.addPoint(xPoints[0], yPoints[0]);
        rightHalf.addPoint(xPoints[5], yPoints[5]);

        Color color2 = habitatColors.getOrDefault(habitatArray[1], Color.LIGHT_GRAY);
        g2d.setColor(color2);
        g2d.fillPolygon(rightHalf);

        // Draw dividing line
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(tileCenterX, yPoints[5], tileCenterX, yPoints[2]);
      }

      // Draw border
      g2d.setColor(Color.BLACK);
      g2d.setStroke(new BasicStroke(2));
      g2d.drawPolygon(hex);

      // Draw allowed wildlife icons on the tile
      Set<WildlifeType> allowedWildlife = pair.getHabitatTile().getAllowedWildlife();
      if (allowedWildlife != null && !allowedWildlife.isEmpty()) {
        int iconSize = 8;
        int iconSpacing = 10;
        int startX = tileCenterX - ((allowedWildlife.size() - 1) * iconSpacing) / 2;
        int iconY = tileCenterY + tileRadius - 8;

        int index = 0;
        for (WildlifeType wt : allowedWildlife) {
          Color wtColor = wildlifeColors.getOrDefault(wt, Color.GRAY);
          g2d.setColor(wtColor);
          g2d.fillOval(startX + (index * iconSpacing) - iconSize / 2, iconY - iconSize / 2, iconSize, iconSize);
          g2d.setColor(Color.BLACK);
          g2d.drawOval(startX + (index * iconSpacing) - iconSize / 2, iconY - iconSize / 2, iconSize, iconSize);
          index++;
        }
      }
    }

    // Draw token
    int tokenCenterX = x + 70;
    int tokenCenterY = y + 30;
    WildlifeType wildlife = pair.getWildlifeToken().getWildlifeType();
    Color color = wildlifeColors.getOrDefault(wildlife, Color.GRAY);
    g2d.setColor(color);
    g2d.fillOval(tokenCenterX - 15, tokenCenterY - 15, 30, 30);
    g2d.setColor(Color.BLACK);
    g2d.drawOval(tokenCenterX - 15, tokenCenterY - 15, 30, 30);

    // Draw wildlife initial
    g2d.setFont(new Font("Arial", Font.BOLD, 12));
    g2d.setColor(Color.WHITE);
    String initial = wildlife.toString().substring(0, 1);
    FontMetrics fm = g2d.getFontMetrics();
    int textWidth = fm.stringWidth(initial);
    g2d.drawString(initial, tokenCenterX - textWidth / 2, tokenCenterY + 4);
  }

  private void drawPlayerBoard(Graphics2D g2d, int playerId) {
    PlayerBoard board = gs.getPlayerBoard(playerId);
    if (board == null)
      return;

    g2d.setColor(Color.BLACK);
    g2d.drawString("Current Player's Board", 20, 140);

    Map<HexCoord, PlacedHabitatTile> tiles = board.getTiles();
    if (tiles == null || tiles.isEmpty()) {
      g2d.drawString("No tiles placed yet", 100, 200);
      return;
    }

    // Find bounds of the board
    int minQ = Integer.MAX_VALUE, maxQ = Integer.MIN_VALUE;
    int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE;

    for (HexCoord coord : tiles.keySet()) {
      minQ = Math.min(minQ, coord.q);
      maxQ = Math.max(maxQ, coord.q);
      minR = Math.min(minR, coord.r);
      maxR = Math.max(maxR, coord.r);
    }

    // Center the board view horizontally and position below market
    int offsetX = getWidth() / 2 - ((maxQ + minQ) * hexWidth / 2);
    // Calculate dynamic Y offset to keep board below market
    int boardHeight = (maxR - minR) * hexHeight;
    int offsetY = Math.max(250, 150 + boardHeight / 4);

    // Draw each tile
    for (Map.Entry<HexCoord, PlacedHabitatTile> entry : tiles.entrySet()) {
      HexCoord coord = entry.getKey();
      PlacedHabitatTile tile = entry.getValue();

      Point center = hexToPixel(coord, offsetX, offsetY);
      drawHexTile(g2d, tile, center.x, center.y, highlightedHexes.contains(coord));
    }
  }

  private Point hexToPixel(HexCoord coord, int offsetX, int offsetY) {
    int x = (int) (hexWidth * (coord.q + coord.r / 2.0)) + offsetX;
    int y = (int) (hexHeight * 0.75 * coord.r) + offsetY;
    return new Point(x, y);
  }

  private void drawHexTile(Graphics2D g2d, PlacedHabitatTile placedTile, int centerX, int centerY,
      boolean highlighted) {
    Polygon hex = createHexagon(centerX, centerY, hexRadius);

    // Draw habitat(s)
    if (placedTile != null && placedTile.tile != null) {
      Set<HabitatType> habitats = placedTile.tile.getHabitats();
      if (habitats != null && !habitats.isEmpty()) {
        if (habitats.size() == 1) {
          // Single habitat - fill entire hex
          HabitatType habitat = habitats.iterator().next();
          Color color = habitatColors.getOrDefault(habitat, Color.LIGHT_GRAY);
          g2d.setColor(color);
          g2d.fillPolygon(hex);
        } else if (habitats.size() == 2) {
          // Dual habitat - split hex vertically using same angle calculation as
          // createHexagon
          HabitatType[] habitatArray = habitats.toArray(new HabitatType[0]);

          // Calculate hex vertices (same as createHexagon)
          // i=0: -30° (right upper)
          // i=1: 30° (right lower)
          // i=2: 90° (bottom)
          // i=3: 150° (left lower)
          // i=4: 210° (left upper)
          // i=5: 270° (top)

          int[] xPoints = new int[6];
          int[] yPoints = new int[6];
          for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i - Math.PI / 6;
            xPoints[i] = (int) (centerX + hexRadius * Math.cos(angle));
            yPoints[i] = (int) (centerY + hexRadius * Math.sin(angle));
          }

          // Left half: includes vertices at 150°, 210°, 270° (indices 3, 4, 5)
          Polygon leftHalf = new Polygon();
          leftHalf.addPoint(xPoints[5], yPoints[5]); // top
          leftHalf.addPoint(xPoints[4], yPoints[4]); // left upper
          leftHalf.addPoint(xPoints[3], yPoints[3]); // left lower
          leftHalf.addPoint(xPoints[2], yPoints[2]); // bottom
          leftHalf.addPoint(centerX, yPoints[2]); // bottom center
          leftHalf.addPoint(centerX, yPoints[5]); // top center

          Color color1 = habitatColors.getOrDefault(habitatArray[0], Color.LIGHT_GRAY);
          g2d.setColor(color1);
          g2d.fillPolygon(leftHalf);

          // Right half: includes vertices at -30°, 30°, 90° (indices 0, 1, 2)
          Polygon rightHalf = new Polygon();
          rightHalf.addPoint(centerX, yPoints[5]); // top center
          rightHalf.addPoint(centerX, yPoints[2]); // bottom center
          rightHalf.addPoint(xPoints[2], yPoints[2]); // bottom
          rightHalf.addPoint(xPoints[1], yPoints[1]); // right lower
          rightHalf.addPoint(xPoints[0], yPoints[0]); // right upper
          rightHalf.addPoint(xPoints[5], yPoints[5]); // top

          Color color2 = habitatColors.getOrDefault(habitatArray[1], Color.LIGHT_GRAY);
          g2d.setColor(color2);
          g2d.fillPolygon(rightHalf);

          // Draw dividing line
          g2d.setColor(Color.BLACK);
          g2d.setStroke(new BasicStroke(1));
          g2d.drawLine(centerX, yPoints[5], centerX, yPoints[2]);
        }
      }
    }

    // Draw border (highlighted tiles show green border for valid placement)
    if (highlighted) {
      // Draw outer glow for highlighted tiles
      g2d.setColor(new Color(50, 255, 50, 100));
      g2d.setStroke(new BasicStroke(6));
      g2d.drawPolygon(hex);

      g2d.setColor(new Color(0, 200, 0));
      g2d.setStroke(new BasicStroke(3));
      g2d.drawPolygon(hex);
    } else {
      g2d.setColor(Color.BLACK);
      g2d.setStroke(new BasicStroke(1));
      g2d.drawPolygon(hex);
    }

    // Draw allowed wildlife icons on the tile (if no wildlife placed yet)
    if (placedTile != null && placedTile.tile != null && !placedTile.tile.hasWildlife()) {
      Set<WildlifeType> allowedWildlife = placedTile.tile.getAllowedWildlife();
      if (allowedWildlife != null && !allowedWildlife.isEmpty()) {
        int iconSize = 6;
        int iconSpacing = 8;
        int startX = centerX - ((allowedWildlife.size() - 1) * iconSpacing) / 2;
        int iconY = centerY + 10;

        int index = 0;
        for (WildlifeType wt : allowedWildlife) {
          Color wtColor = wildlifeColors.getOrDefault(wt, Color.GRAY);
          g2d.setColor(wtColor);
          g2d.fillOval(startX + (index * iconSpacing) - iconSize / 2, iconY - iconSize / 2, iconSize, iconSize);
          g2d.setColor(Color.BLACK);
          g2d.drawOval(startX + (index * iconSpacing) - iconSize / 2, iconY - iconSize / 2, iconSize, iconSize);
          index++;
        }
      }
    }

    // Draw wildlife token if present - LARGE and PROMINENT
    if (placedTile != null && placedTile.tile != null && placedTile.tile.hasWildlife()) {
      WildlifeType wildlife = placedTile.tile.getWildlifeToken().getWildlifeType();

      int tokenSize = 18; // Much larger token

      // Draw shadow for depth
      g2d.setColor(new Color(0, 0, 0, 60));
      g2d.fillOval(centerX - tokenSize + 1, centerY - tokenSize + 1, tokenSize * 2, tokenSize * 2);

      // Draw white background ring
      g2d.setColor(Color.WHITE);
      g2d.fillOval(centerX - tokenSize, centerY - tokenSize, tokenSize * 2, tokenSize * 2);

      // Draw colored wildlife token
      Color color = wildlifeColors.getOrDefault(wildlife, Color.GRAY);
      g2d.setColor(color);
      g2d.fillOval(centerX - tokenSize + 3, centerY - tokenSize + 3, tokenSize * 2 - 6, tokenSize * 2 - 6);

      // Draw thick black border
      g2d.setColor(Color.BLACK);
      g2d.setStroke(new BasicStroke(3));
      g2d.drawOval(centerX - tokenSize, centerY - tokenSize, tokenSize * 2, tokenSize * 2);

      // Draw wildlife initial - LARGE
      g2d.setFont(new Font("Arial", Font.BOLD, 16));
      g2d.setColor(Color.WHITE);
      String initial = wildlife.toString().substring(0, 1);
      FontMetrics fm = g2d.getFontMetrics();
      int textWidth = fm.stringWidth(initial);
      int textHeight = fm.getAscent();
      g2d.drawString(initial, centerX - textWidth / 2, centerY + textHeight / 2 - 2);
    }
  }

  private Polygon createHexagon(int centerX, int centerY, int radius) {
    Polygon hex = new Polygon();
    for (int i = 0; i < 6; i++) {
      double angle = Math.PI / 3 * i - Math.PI / 6;
      int x = (int) (centerX + radius * Math.cos(angle));
      int y = (int) (centerY + radius * Math.sin(angle));
      hex.addPoint(x, y);
    }
    return hex;
  }

  public void highlight(AbstractAction action) {
    clearHighlights();

    // Highlight tiles compatible with PlaceWildlifeAction
    if (action instanceof PlaceWildlifeAction) {
      PlaceWildlifeAction placeAction = (PlaceWildlifeAction) action;
      // Extract the coordinate from the action's toString or use reflection
      String actionStr = placeAction.toString();
      if (actionStr.startsWith("PlaceWildlife@")) {
        String[] parts = actionStr.substring("PlaceWildlife@".length()).split(",");
        if (parts.length == 2) {
          try {
            int q = Integer.parseInt(parts[0]);
            int r = Integer.parseInt(parts[1]);
            highlightedHexes.add(new HexCoord(q, r));
          } catch (NumberFormatException e) {
            // Ignore parse errors
          }
        }
      }
    }

    repaint();
  }

  @Override
  public void clearHighlights() {
    highlightedHexes.clear();
    repaint();
  }
}
