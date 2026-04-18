package view;

import model.Board;
import model.GameModel;
import model.Player;
import model.Slot;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BoardPanel extends JPanel {

    private final GameModel model;
    private static final int GRID = 7; 

    private final List<SlotCell> cells = new ArrayList<>();

    public BoardPanel(GameModel model) {
        this.model = model;
        setLayout(new GridLayout(GRID, GRID, 2, 2));
        setBackground(new Color(205, 225, 205));
        buildCells();
    }

    private void buildCells() {
        cells.clear();
        Board board = model.getBoard();
        int[][] perimeter = perimeterCoords();

        for (int i = 0; i < GRID * GRID; i++) {
            int row = i / GRID;
            int col = i % GRID;
            boolean onPerimeter = row == 0 || row == GRID - 1 || col == 0 || col == GRID - 1;
            if (onPerimeter) {
                int perimIndex = indexOfPerimeter(row, col, perimeter);
                if (perimIndex < 0 || perimIndex >= board.size()) {
                    add(new JLabel(""));
                } else {
                    SlotCell cell = new SlotCell(board.getSlot(perimIndex), model);
                    cells.add(cell);
                    add(cell);
                }
            } else {
                if (row == 3 && col == 3) {
                    JLabel center = new JLabel("<html><center>MINI-<br/>MONOPOLY</center></html>",
                            SwingConstants.CENTER);
                    center.setFont(new Font("Arial", Font.BOLD, 18));
                    center.setForeground(new Color(80, 20, 20));
                    add(center);
                } else {
                    add(new JLabel(""));
                }
            }
        }
    }

    private int[][] perimeterCoords() {
        List<int[]> coords = new ArrayList<>();
        int last = GRID - 1;
        
        coords.add(new int[]{last, last});
        
        for (int c = last - 1; c >= 0; c--) coords.add(new int[]{last, c});
        
        for (int r = last - 1; r >= 0; r--) coords.add(new int[]{r, 0});
        
        for (int c = 1; c <= last; c++) coords.add(new int[]{0, c});
        
        for (int r = 1; r < last; r++) coords.add(new int[]{r, last});
        return coords.toArray(new int[0][]);
    }

    private int indexOfPerimeter(int row, int col, int[][] perimeter) {
        for (int i = 0; i < perimeter.length; i++) {
            if (perimeter[i][0] == row && perimeter[i][1] == col) return i;
        }
        return -1;
    }

    public void refresh() {
        for (SlotCell cell : cells) cell.repaint();
    }

    static class SlotCell extends JPanel {
        private final Slot slot;
        private final GameModel model;

        SlotCell(Slot slot, GameModel model) {
            this.slot = slot;
            this.model = model;
            setPreferredSize(new Dimension(90, 90));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
            setToolTipText(buildTooltip());
        }

        private String buildTooltip() {
            StringBuilder sb = new StringBuilder("<html>");
            sb.append("<b>[").append(slot.getNumber()).append("] ")
              .append(slot.getName()).append("</b><br/>");
            if (slot.isLand()) {
                sb.append("Price: $").append(slot.getPrice()).append("<br/>");
                sb.append("Rent: $").append(slot.getBaseRent()).append(" (base)<br/>");
                sb.append("Owner: ")
                  .append(slot.isOwned() ? slot.getOwner().getName() : "None");
            } else {
                sb.append("(No effect)");
            }
            sb.append("</html>");
            return sb.toString();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (slot.isLand()) {
                Color top = slot.isOwned() ? slot.getOwner().getColor() : Color.LIGHT_GRAY;
                g2.setColor(top);
                g2.fillRect(0, 0, w, 14);
            } else {
                g2.setColor(new Color(255, 220, 100));
                g2.fillRect(0, 0, w, 14);
            }

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            drawWrappedName(g2, slot.getName(), w, 24);
            if (slot.isLand()) {
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                g2.drawString("$" + slot.getPrice(), 4, h - 22);
            } else if (slot.getNumber() == 0) {
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                g2.drawString("GO", w / 2 - 12, h - 22);
            }

            drawTokens(g2, w, h);

            if (slot.getNumber() == model.getCurrentPlayer().getPosition()) {
                g2.setStroke(new BasicStroke(3f));
                g2.setColor(new Color(255, 140, 0));
                g2.drawRect(1, 1, w - 3, h - 3);
            }
        }

        private void drawWrappedName(Graphics2D g2, String name, int w, int y) {
            FontMetrics fm = g2.getFontMetrics();
            String[] words = name.split(" ");
            StringBuilder line = new StringBuilder();
            int cursorY = y;
            for (String word : words) {
                String trial = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(trial) > w - 8) {
                    g2.drawString(line.toString(), 4, cursorY);
                    cursorY += fm.getHeight();
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(trial);
                }
            }
            if (line.length() > 0) g2.drawString(line.toString(), 4, cursorY);
        }

        private void drawTokens(Graphics2D g2, int w, int h) {
            List<Player> on = new ArrayList<>();
            for (Player p : model.getPlayers()) {
                if (p.getPosition() == slot.getNumber() && p.isActive()) on.add(p);
            }
            int size = 12;
            int gap = 2;
            int totalW = on.size() * size + Math.max(0, on.size() - 1) * gap;
            int startX = (w - totalW) / 2;
            int y = h - size - 4;
            for (int i = 0; i < on.size(); i++) {
                Player p = on.get(i);
                int x = startX + i * (size + gap);
                g2.setColor(p.getColor());
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.BLACK);
                g2.drawOval(x, y, size, size);
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                g2.drawString(String.valueOf(p.getId()), x + 3, y + size - 2);
            }
        }
    }
}
