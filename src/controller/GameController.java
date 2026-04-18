package controller;

import model.GameModel;
import model.Player;
import model.Slot;
import view.GameEditorDialog;
import view.GameView;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.stream.Collectors;

/**
 * Controller class orchestrating interactions between the game view (UI) and the game model.
 * Responsible for handling user inputs, dialogs, and updating the view when the model state changes.
 */
public class GameController implements GameModel.Listener {

    private final GameModel model;
    private final GameView view;
    private boolean gameOverDialogShown = false;

    public GameController(GameModel model, GameView view) {
        this.model = model;
        this.view = view;
        model.addListener(this);
        wireButtons();
        wireEditorShortcut();
        refreshView();
    }

    /**
     * Maps UI button actions to the corresponding game model operations.
     */
    private void wireButtons() {
        view.controlPanel.rollButton.addActionListener(e -> model.rollDice());
        view.controlPanel.buyButton.addActionListener(e -> model.buyCurrentLand());
        view.controlPanel.declineButton.addActionListener(e -> model.declineBuy());
        view.controlPanel.endTurnButton.addActionListener(e -> model.endTurn());
        view.controlPanel.tradeButton.addActionListener(e -> openTradeDialog());
        view.controlPanel.landsButton.addActionListener(e -> showLandOwnership());
        view.controlPanel.editorButton.addActionListener(e -> openEditor());
    }

    private void wireEditorShortcut() {
        JRootPane root = view.getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_E,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "openEditor");
        root.getActionMap().put("openEditor", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                openEditor();
            }
        });
    }

    private void openEditor() {
        new GameEditorDialog(view, model).setVisible(true);
    }

    /**
     * Initializes a trade transaction initiated by the current player.
     * Prompts for the trade type, counterparty, property being traded, and agreed price.
     */
    private void openTradeDialog() {
        Player buyer = model.getCurrentPlayer();
        String[] directions = {"Buy (I am the buyer)", "Sell (I am the seller)"};
        int dir = JOptionPane.showOptionDialog(view, "Trade type:",
            "Trade", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, directions, directions[0]);
        if (dir < 0) return;

        Player[] others = model.getPlayers().stream()
            .filter(p -> p != buyer && p.isActive())
            .toArray(Player[]::new);
        if (others.length == 0) {
            JOptionPane.showMessageDialog(view, "No other active players.");
            return;
        }
        Player counterparty = (Player) JOptionPane.showInputDialog(view,
            "Choose counterparty:", "Trade", JOptionPane.QUESTION_MESSAGE,
            null, others, others[0]);
        if (counterparty == null) return;

        Player seller = (dir == 0) ? counterparty : buyer;
        Player payer  = (dir == 0) ? buyer : counterparty;

        Slot[] owned = seller.getOwnedLands().toArray(new Slot[0]);
        if (owned.length == 0) {
            JOptionPane.showMessageDialog(view,
                seller.getName() + " owns no land to trade.");
            return;
        }
        Slot land = (Slot) JOptionPane.showInputDialog(view,
            "Choose land owned by " + seller.getName() + ":",
            "Trade", JOptionPane.QUESTION_MESSAGE, null, owned, owned[0]);
        if (land == null) return;

        String priceStr = JOptionPane.showInputDialog(view,
            "Agreed price (buyer " + payer.getName()
                + " pays seller " + seller.getName() + "):",
            String.valueOf(land.getPrice()));
        if (priceStr == null) return;
        int price;
        try { price = Integer.parseInt(priceStr.trim()); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(view, "Price must be an integer.");
            return;
        }
        model.trade(payer, seller, land, price);
    }

    private void showLandOwnership() {
        StringBuilder sb = new StringBuilder("<html><table border='0' cellpadding='3'>");
        sb.append("<tr><th>Slot</th><th>Name</th><th>Price</th><th>Owner</th></tr>");
        for (Slot s : model.getBoard().getSlots()) {
            if (!s.isLand()) continue;
            String owner = s.isOwned() ? s.getOwner().getName() : "—";
            sb.append("<tr><td>").append(s.getNumber())
              .append("</td><td>").append(s.getName())
              .append("</td><td>$").append(s.getPrice())
              .append("</td><td>").append(owner)
              .append("</td></tr>");
        }
        sb.append("</table></html>");
        JLabel label = new JLabel(sb.toString());
        JOptionPane.showMessageDialog(view, label, "Land Ownership",
            JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void onGameStateChanged(GameModel m) {
        // Run on the Event Dispatch Thread to safely update the GUI
        SwingUtilities.invokeLater(this::refreshView);
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> view.logPanel.append(message));
    }

    /**
     * Refreshes the UI components to reflect the current state of the game model.
     * Displays an alert if the game phase denotes the game is over.
     */
    private void refreshView() {
        view.boardPanel.refresh();
        view.statusPanel.refresh();
        view.controlPanel.updateForPhase(
            model.getPhase().name(),
            model.getPhase() == GameModel.Phase.GAME_OVER);

        if (model.getPhase() == GameModel.Phase.GAME_OVER
                && model.getWinner() != null
                && !gameOverDialogShown) {
            gameOverDialogShown = true;
            JOptionPane.showMessageDialog(view,
                "Game Over! " + model.getWinner().getName() + " wins!",
                "Mini-Monopoly", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @SuppressWarnings("unused")
    private String ownedLandsSummary(Player p) {
        return p.getOwnedLands().stream().map(Slot::getName)
            .collect(Collectors.joining(", "));
    }
}
