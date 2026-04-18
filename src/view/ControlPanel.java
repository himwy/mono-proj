package view;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    public final JButton rollButton    = new JButton("Roll Dice");
    public final JButton buyButton     = new JButton("Buy Land");
    public final JButton declineButton = new JButton("Decline Buy");
    public final JButton endTurnButton = new JButton("End Turn");
    public final JButton tradeButton   = new JButton("Trade");
    public final JButton landsButton   = new JButton("Show Land Ownership");
    public final JButton editorButton  = new JButton("Editor");

    public ControlPanel() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
        setBackground(new Color(60, 60, 70));
        for (JButton b : new JButton[]{
                rollButton, buyButton, declineButton,
                endTurnButton, tradeButton, landsButton, editorButton}) {
            b.setFocusPainted(false);
            b.setFont(new Font("Arial", Font.BOLD, 12));
            add(b);
        }

        editorButton.setVisible(false);
    }

    public void updateForPhase(String phase, boolean gameOver) {
        boolean awaitRoll   = phase.equals("AWAITING_ROLL");
        boolean awaitBuy    = phase.equals("AWAITING_BUY_DECISION");
        boolean awaitEnd    = phase.equals("AWAITING_END_TURN");

        rollButton.setEnabled(awaitRoll && !gameOver);
        buyButton.setEnabled(awaitBuy && !gameOver);
        declineButton.setEnabled(awaitBuy && !gameOver);
        endTurnButton.setEnabled((awaitEnd || awaitBuy) && !gameOver);
        tradeButton.setEnabled(awaitRoll && !gameOver);
        landsButton.setEnabled(true);
    }
}
