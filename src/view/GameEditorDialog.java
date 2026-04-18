package view;

import model.GameModel;
import model.Player;
import model.Slot;

import javax.swing.*;
import java.awt.*;

public class GameEditorDialog extends JDialog {

    private final GameModel model;

    public GameEditorDialog(JFrame parent, GameModel model) {
        super(parent, "Game Editor (Easter Egg)", true);
        this.model = model;
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Player", buildPlayerTab());
        tabs.addTab("Land",   buildLandTab());
        tabs.addTab("Turn",   buildTurnTab());
        add(tabs, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.add(close);
        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel buildPlayerTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        JComboBox<Player> playerBox = new JComboBox<>(
            model.getPlayers().toArray(new Player[0]));
        JTextField balanceField  = new JTextField(8);
        JTextField positionField = new JTextField(8);
        JComboBox<Player.Status> statusBox =
            new JComboBox<>(Player.Status.values());

        c.gridx = 0; c.gridy = 0; panel.add(new JLabel("Player:"), c);
        c.gridx = 1;               panel.add(playerBox, c);
        c.gridx = 0; c.gridy = 1; panel.add(new JLabel("New balance:"), c);
        c.gridx = 1;               panel.add(balanceField, c);
        c.gridx = 0; c.gridy = 2; panel.add(new JLabel("New position:"), c);
        c.gridx = 1;               panel.add(positionField, c);
        c.gridx = 0; c.gridy = 3; panel.add(new JLabel("Status:"), c);
        c.gridx = 1;               panel.add(statusBox, c);

        JButton apply = new JButton("Apply");
        apply.addActionListener(e -> {
            Player p = (Player) playerBox.getSelectedItem();
            if (p == null) return;
            String bal = balanceField.getText().trim();
            String pos = positionField.getText().trim();
            try {
                if (!bal.isEmpty()) model.editorSetBalance(p, Integer.parseInt(bal));
                if (!pos.isEmpty()) model.editorSetPosition(p, Integer.parseInt(pos));
                model.editorSetStatus(p, (Player.Status) statusBox.getSelectedItem());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Numeric fields must be integers.");
            }
        });
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; panel.add(apply, c);
        return panel;
    }

    private JPanel buildLandTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        JComboBox<Slot> landBox = new JComboBox<>();
        for (Slot s : model.getBoard().getSlots()) {
            if (s.isLand()) landBox.addItem(s);
        }
        Player[] withNone = new Player[model.getPlayers().size() + 1];
        withNone[0] = null;
        for (int i = 0; i < model.getPlayers().size(); i++) {
            withNone[i + 1] = model.getPlayers().get(i);
        }
        JComboBox<Player> ownerBox = new JComboBox<>(withNone);
        ownerBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                String text = value == null ? "— None —" : ((Player) value).getName();
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        c.gridx = 0; c.gridy = 0; panel.add(new JLabel("Land:"), c);
        c.gridx = 1;               panel.add(landBox, c);
        c.gridx = 0; c.gridy = 1; panel.add(new JLabel("New owner:"), c);
        c.gridx = 1;               panel.add(ownerBox, c);

        JButton apply = new JButton("Apply");
        apply.addActionListener(e -> {
            Slot land = (Slot) landBox.getSelectedItem();
            Player owner = (Player) ownerBox.getSelectedItem();
            if (land != null) model.editorSetOwner(land, owner);
        });
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; panel.add(apply, c);
        return panel;
    }

    private JPanel buildTurnTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        JComboBox<Player> playerBox = new JComboBox<>(
            model.getPlayers().toArray(new Player[0]));

        c.gridx = 0; c.gridy = 0; panel.add(new JLabel("Force current turn to:"), c);
        c.gridx = 1;               panel.add(playerBox, c);

        JButton apply = new JButton("Apply");
        apply.addActionListener(e -> {
            Player p = (Player) playerBox.getSelectedItem();
            if (p != null) model.editorSetCurrentPlayer(p.getId() - 1);
        });
        c.gridx = 0; c.gridy = 1; c.gridwidth = 2; panel.add(apply, c);
        return panel;
    }
}
