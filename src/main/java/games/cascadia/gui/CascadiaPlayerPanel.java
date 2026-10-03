package games.cascadia.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import games.cascadia.CascadiaGameState;
import games.cascadia.components.HabitatType;

class CascadiaPlayerPanel extends JPanel {
  final int playerID;
  JLabel playerLabel;
  JLabel scoreLabel;
  JLabel boardSizeLabel;
  JLabel natureTokensLabel;
  JLabel largestHabitatLabel;

  Border highlightBorder = BorderFactory.createLineBorder(Color.blue, 3);
  Border normalBorder = BorderFactory.createLineBorder(Color.GRAY, 1);

  // Colors for player identification
  private static final Color[] PLAYER_COLORS = {
      new Color(220, 20, 60), // Crimson
      new Color(30, 144, 255), // Dodger Blue
      new Color(50, 205, 50), // Lime Green
      new Color(255, 140, 0) // Dark Orange
  };

  CascadiaPlayerPanel(CascadiaGUIManager gui, int playerID, String playerName) {
    this.playerID = playerID;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(normalBorder);
    setBackground(new Color(250, 250, 250));

    // Player header
    playerLabel = new JLabel("Player " + playerID + ": " + playerName);
    playerLabel.setFont(new Font("Arial", Font.BOLD, 14));
    playerLabel.setForeground(PLAYER_COLORS[playerID % PLAYER_COLORS.length]);

    // Score information
    scoreLabel = new JLabel("Score: 0");
    scoreLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    // Board size
    boardSizeLabel = new JLabel("Board Tiles: 0");
    boardSizeLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    // Nature tokens
    natureTokensLabel = new JLabel("Nature Tokens: 0");
    natureTokensLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    // Largest habitat
    largestHabitatLabel = new JLabel("Largest Habitat: -");
    largestHabitatLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    // Add components with spacing
    add(Box.createRigidArea(new Dimension(0, 5)));
    add(playerLabel);
    add(Box.createRigidArea(new Dimension(0, 10)));
    add(scoreLabel);
    add(Box.createRigidArea(new Dimension(0, 5)));
    add(boardSizeLabel);
    add(Box.createRigidArea(new Dimension(0, 5)));
    add(natureTokensLabel);
    add(Box.createRigidArea(new Dimension(0, 5)));
    add(largestHabitatLabel);
    add(Box.createRigidArea(new Dimension(0, 10)));

    setPreferredSize(new Dimension(300, 150));
  }

  void _update(CascadiaGameState gs) {
    // Update player name if it changed
    String playerName = gs.getPlayerName(playerID);
    playerLabel.setText("Player " + playerID + ": " + playerName);

    // Update score
    int score = gs.computeScore(playerID);
    scoreLabel.setText("Score: " + score);

    // Update board size
    if (gs.getPlayerBoard(playerID) != null) {
      int boardSize = gs.getPlayerBoard(playerID).getTiles().size();
      boardSizeLabel.setText("Board Tiles: " + boardSize);

      // Find largest habitat
      String largestHabitat = findLargestHabitat(gs, playerID);
      largestHabitatLabel.setText("Largest Habitat: " + largestHabitat);
    } else {
      boardSizeLabel.setText("Board Tiles: 0");
      largestHabitatLabel.setText("Largest Habitat: -");
    }

    // Update nature tokens
    natureTokensLabel.setText("Nature Tokens: " + gs.getNatureTokens(playerID));

    // Highlight current player
    if (gs.getCurrentPlayer() == playerID) {
      setBorder(highlightBorder);
      setBackground(new Color(230, 255, 230));
    } else {
      setBorder(normalBorder);
      setBackground(new Color(250, 250, 250));
    }
  }

  private String findLargestHabitat(CascadiaGameState gs, int playerID) {
    if (gs.getPlayerBoard(playerID) == null) {
      return "-";
    }

    Map<HabitatType, Integer> habitatCounts = new HashMap<>();
    gs.getPlayerBoard(playerID).getTiles().forEach((coord, tile) -> {
      if (tile != null && tile.tile.getAllowedWildlife() != null) {
        for (HabitatType habitat : tile.tile.getHabitats()) {
          habitatCounts.put(habitat, habitatCounts.getOrDefault(habitat, 0) + 1);
        }
      }
    });

    if (habitatCounts.isEmpty()) {
      return "-";
    }

    HabitatType largest = null;
    int maxCount = 0;
    for (Map.Entry<HabitatType, Integer> entry : habitatCounts.entrySet()) {
      if (entry.getValue() > maxCount) {
        maxCount = entry.getValue();
        largest = entry.getKey();
      }
    }

    return largest != null ? largest.toString() + " (" + maxCount + ")" : "-";
  }
}
