package view;

import javax.swing.*;
import java.awt.*;

public class LogPanel extends JPanel {

    private final JTextArea area = new JTextArea(8, 30);

    public LogPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Game Log"));
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setBackground(new Color(250, 250, 240));
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void append(String msg) {
        area.append(msg + "\n");
        area.setCaretPosition(area.getDocument().getLength());
    }
}
