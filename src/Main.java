import controller.GameController;
import model.Board;
import model.DataLoader;
import model.GameModel;
import model.Slot;
import view.GameView;

import javax.swing.*;
import java.util.List;

public class Main {

    private static final int STARTING_BALANCE = 2000;
    private static final int GO_BONUS         = 2000;
    private static final String DATA_PATH     = "data/slots.csv";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        List<Slot> slots;
        try {
            slots = DataLoader.loadSlots(DATA_PATH);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Failed to load slot data from '" + DATA_PATH + "':\n" + e.getMessage(),
                "Mini-Monopoly", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (slots.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Slot data file is empty: " + DATA_PATH,
                "Mini-Monopoly", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Board board = new Board(slots);
        GameModel model = new GameModel(board, STARTING_BALANCE, GO_BONUS);
        GameView view = new GameView(model);
        new GameController(model, view);

        view.setVisible(true);
        view.logPanel.append("Welcome to Mini-Monopoly!");
        view.logPanel.append("Starting balance: $" + STARTING_BALANCE
            + ", GO bonus: $" + GO_BONUS + ".");
        view.logPanel.append("Press Ctrl+Shift+E for the hidden Game Editor.");
        view.logPanel.append("It is " + model.getCurrentPlayer().getName() + "'s turn.");
    }
}
