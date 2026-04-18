package view;

import model.GameModel;

import javax.swing.*;
import java.awt.*;

public class GameView extends JFrame {

    public final BoardPanel   boardPanel;
    public final StatusPanel  statusPanel;
    public final ControlPanel controlPanel;
    public final LogPanel     logPanel;

    public GameView(GameModel model) {
        super("Mini-Monopoly");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(4, 4));

        boardPanel   = new BoardPanel(model);
        statusPanel  = new StatusPanel(model);
        controlPanel = new ControlPanel();
        logPanel     = new LogPanel();

        add(boardPanel, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(340, 700));
        right.add(statusPanel, BorderLayout.CENTER);
        right.add(logPanel, BorderLayout.SOUTH);
        add(right, BorderLayout.EAST);

        add(controlPanel, BorderLayout.SOUTH);

        setSize(1150, 780);
        setLocationRelativeTo(null);
    }
}
