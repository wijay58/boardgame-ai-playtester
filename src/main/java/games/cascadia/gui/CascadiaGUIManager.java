package games.cascadia.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.cascadia.CascadiaGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

public class CascadiaGUIManager extends AbstractGUIManager implements IScreenHighlight {
  CascadiaGameState gs;
  CascadiaBoardView boardView;
  CascadiaPlayerPanel[] playerPanels;

  JPanel gameInfo;
  JLabel phaseLabel;
  JLabel marketLabel;
  JLabel natureTokensLabel;

  public CascadiaGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
    super(parent, game, ac, humanId);
    if (game == null)
      return;
    this.gs = (CascadiaGameState) game.getGameState();

    boardView = new CascadiaBoardView(gs);

    // Bottom area will show actions available
    JComponent actionPanel = createActionPanel(new IScreenHighlight[] { boardView, this }, 800,
        defaultActionPanelHeight, false, false, null, null, null);

    JPanel wrapper = new JPanel();
    wrapper.setBackground(Color.white);
    parent.setLayout(new FlowLayout());
    parent.add(wrapper);

    wrapper.setLayout(new BorderLayout());
    wrapper.setBackground(Color.white);
    wrapper.add(createGameStateInfoPanel(gs), BorderLayout.NORTH);

    JPanel mainPanel = new JPanel();
    mainPanel.setOpaque(false);
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

    // Create player panels
    playerPanels = new CascadiaPlayerPanel[gs.getNPlayers()];
    for (int i = 0; i < gs.getNPlayers(); i++) {
      playerPanels[i] = new CascadiaPlayerPanel(this, i, game.getPlayers().get(i).toString());
      playerPanels[i].setOpaque(false);
    }

    mainPanel.add(Box.createRigidArea(new Dimension(5, 0)));

    // Layout player panels based on number of players
    if (gs.getNPlayers() == 2) {
      JPanel leftPanel = new JPanel();
      leftPanel.setOpaque(false);
      leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
      JScrollPane scrollPane0 = new JScrollPane(playerPanels[0]);
      scrollPane0.setOpaque(false);
      scrollPane0.getViewport().setOpaque(false);
      scrollPane0.setPreferredSize(new Dimension(350, 300));
      leftPanel.add(scrollPane0);
      mainPanel.add(leftPanel);

      mainPanel.add(boardView);

      JPanel rightPanel = new JPanel();
      rightPanel.setOpaque(false);
      rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
      JScrollPane scrollPane1 = new JScrollPane(playerPanels[1]);
      scrollPane1.setOpaque(false);
      scrollPane1.getViewport().setOpaque(false);
      scrollPane1.setPreferredSize(new Dimension(350, 300));
      rightPanel.add(scrollPane1);
      mainPanel.add(rightPanel);
    } else if (gs.getNPlayers() == 3 || gs.getNPlayers() == 4) {
      JPanel leftPanel = new JPanel();
      leftPanel.setOpaque(false);
      leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
      JScrollPane scrollPane0 = new JScrollPane(playerPanels[0]);
      scrollPane0.setOpaque(false);
      scrollPane0.getViewport().setOpaque(false);
      scrollPane0.setPreferredSize(new Dimension(350, 200));
      leftPanel.add(scrollPane0);
      JScrollPane scrollPane1 = new JScrollPane(playerPanels[1]);
      scrollPane1.setOpaque(false);
      scrollPane1.getViewport().setOpaque(false);
      scrollPane1.setPreferredSize(new Dimension(350, 200));
      leftPanel.add(scrollPane1);
      mainPanel.add(leftPanel);

      mainPanel.add(boardView);

      JPanel rightPanel = new JPanel();
      rightPanel.setOpaque(false);
      rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
      JScrollPane scrollPane2 = new JScrollPane(playerPanels[2]);
      scrollPane2.setOpaque(false);
      scrollPane2.getViewport().setOpaque(false);
      scrollPane2.setPreferredSize(new Dimension(350, 200));
      rightPanel.add(scrollPane2);
      if (gs.getNPlayers() == 4) {
        JScrollPane scrollPane3 = new JScrollPane(playerPanels[3]);
        scrollPane3.setOpaque(false);
        scrollPane3.getViewport().setOpaque(false);
        scrollPane3.setPreferredSize(new Dimension(350, 200));
        rightPanel.add(scrollPane3);
      }
      mainPanel.add(rightPanel);
    }

    mainPanel.add(Box.createRigidArea(new Dimension(5, 0)));

    wrapper.add(mainPanel, BorderLayout.CENTER);
    wrapper.add(actionPanel, BorderLayout.SOUTH);

    wrapper.revalidate();
    wrapper.repaint();
  }

  @Override
  public int getMaxActionSpace() {
    return 500;
  }

  @Override
  protected void _update(AbstractPlayer player, AbstractGameState gameState) {
    this.gs = (CascadiaGameState) gameState;

    phaseLabel.setText("Phase: " + gs.getPhase());
    marketLabel.setText("Market: " + (gs.getMarket() != null ? gs.getMarket().size() + " pairs available" : "empty"));
    natureTokensLabel.setText("Nature Tokens in Pool: " + gs.getNatureTokensLeft());

    for (int i = 0; i < gameState.getNPlayers(); i++) {
      playerPanels[i]._update(gs);
    }

    boardView.updateGameState(gs);
    parent.repaint();
  }

  protected JPanel createGameStateInfoPanel(AbstractGameState gameState) {
    gameInfo = new JPanel();
    gameInfo.setOpaque(false);
    gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
    gameInfo.add(new JLabel("<html><h1>Cascadia</h1></html>"));

    updateGameStateInfo(gameState);

    phaseLabel = new JLabel("Phase: " + gs.getPhase());
    marketLabel = new JLabel(
        "Market: " + (gs.getMarket() != null ? gs.getMarket().size() + " pairs available" : "empty"));
    natureTokensLabel = new JLabel("Nature Tokens in Pool: " + gs.getNatureTokensLeft());

    gameInfo.add(gameStatus);
    gameInfo.add(gamePhase);
    gameInfo.add(turn);
    gameInfo.add(currentPlayer);
    gameInfo.add(phaseLabel);
    gameInfo.add(marketLabel);
    gameInfo.add(natureTokensLabel);

    gameInfo.setPreferredSize(new Dimension(900, 170));

    JPanel wrapper = new JPanel();
    wrapper.setOpaque(false);
    wrapper.add(gameInfo, BorderLayout.WEST);
    return wrapper;
  }

  @Override
  public void clearHighlights() {
    // Clear any highlights from board or panels
    boardView.clearHighlights();
  }
}
