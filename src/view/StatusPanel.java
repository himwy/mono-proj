package view;

import model.GameModel;
import model.Player;
import model.Slot;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class StatusPanel extends JPanel {

    private final GameModel model;
    private final PlayerCard[] cards;
    private final JLabel turnLabel;

    public StatusPanel(GameModel model) {
        this.model = model;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 235));

        turnLabel = new JLabel(" ", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 14));
        turnLabel.setOpaque(true);
        turnLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(turnLabel, BorderLayout.NORTH);

        JPanel cardsHost = new JPanel();
        cardsHost.setLayout(new GridLayout(4, 1, 4, 4));
        cardsHost.setOpaque(false);
        cards = new PlayerCard[model.getPlayers().size()];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = new PlayerCard(model.getPlayers().get(i));
            cardsHost.add(cards[i]);
        }
        add(cardsHost, BorderLayout.CENTER);
        refresh();
    }

    public void refresh() {
        Player cur = model.getCurrentPlayer();
        turnLabel.setText("Current turn: " + cur.getName()
            + "   |   Phase: " + model.getPhase());
        turnLabel.setBackground(cur.getColor());
        turnLabel.setForeground(Color.WHITE);
        for (PlayerCard c : cards) c.refresh();
        revalidate();
        repaint();
    }

    private class PlayerCard extends JPanel {
        private final Player player;
        private final JLabel title;
        private final JLabel balance;
        private final JLabel position;
        private final JLabel status;
        private final JTextArea lands;

        PlayerCard(Player player) {
            this.player = player;
            setLayout(new BorderLayout(4, 4));
            Border outer = BorderFactory.createLineBorder(player.getColor(), 2);
            Border inner = BorderFactory.createEmptyBorder(4, 6, 4, 6);
            setBorder(BorderFactory.createCompoundBorder(outer, inner));
            setBackground(Color.WHITE);

            title = new JLabel(player.getName());
            title.setFont(new Font("Arial", Font.BOLD, 13));
            title.setForeground(player.getColor());
            add(title, BorderLayout.NORTH);

            JPanel info = new JPanel(new GridLayout(3, 1));
            info.setOpaque(false);
            balance  = new JLabel();
            position = new JLabel();
            status   = new JLabel();
            info.add(balance);
            info.add(position);
            info.add(status);
            add(info, BorderLayout.CENTER);

            lands = new JTextArea(2, 20);
            lands.setEditable(false);
            lands.setLineWrap(true);
            lands.setWrapStyleWord(true);
            lands.setFont(new Font("Arial", Font.PLAIN, 10));
            lands.setBackground(new Color(252, 252, 245));
            add(new JScrollPane(lands), BorderLayout.SOUTH);
        }

        void refresh() {
            balance.setText("Balance: $" + player.getBalance());
            Slot here = model.getBoard().getSlot(player.getPosition());
            position.setText("Position: " + player.getPosition() + " (" + here.getName() + ")");
            status.setText("Status: " + player.getStatus());
            StringBuilder sb = new StringBuilder();
            if (player.getOwnedLands().isEmpty()) {
                sb.append("Owns no land.");
            } else {
                sb.append("Owns: ");
                for (Slot s : player.getOwnedLands()) {
                    sb.append(s.getName()).append(", ");
                }
                if (sb.length() >= 2) sb.setLength(sb.length() - 2);
            }
            lands.setText(sb.toString());
        }
    }
}
