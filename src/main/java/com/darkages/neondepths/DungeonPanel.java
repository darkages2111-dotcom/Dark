package com.darkages.neondepths;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

final class DungeonPanel extends JPanel {
    private final DungeonApp.DungeonModel model;
    private final int tileSize = 20;

    DungeonPanel(DungeonApp.DungeonModel model) {
        this.model = model;
        setBackground(new Color(8, 10, 18));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { editAt(e); }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) { editAt(e); }
        });

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_Z -> model.undo();
                    case KeyEvent.VK_Y -> model.redo();
                    case KeyEvent.VK_SPACE -> model.paused = !model.paused;
                    case KeyEvent.VK_ESCAPE -> model.selected = DungeonApp.Tile.EMPTY;
                    case KeyEvent.VK_S -> saveGame();
                    case KeyEvent.VK_L -> loadGame();
                }
                repaint();
            }
        });
    }

    JComponent createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(new Color(25, 29, 43));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 5));
        tools.setOpaque(false);
        for (DungeonApp.Tile tile : DungeonApp.Tile.values()) {
            JButton button = new JButton(tile.name + " [" + tile.cost + "]");
            button.setForeground(Color.WHITE);
            button.setBackground(new Color(48, 53, 72));
            button.addActionListener(event -> model.selected = tile);
            tools.add(button);
        }

        JButton simulate = new JButton("PAUSE");
        simulate.setForeground(new Color(255, 210, 90));
        simulate.setBackground(new Color(68, 42, 54));
        simulate.addActionListener(event -> model.paused = !model.paused);

        JComboBox<DungeonApp.Theme> themes = new JComboBox<>(DungeonApp.Theme.values());
        themes.setSelectedItem(model.theme);
        themes.addActionListener(event -> model.theme = (DungeonApp.Theme) themes.getSelectedItem());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        right.setOpaque(false);
        right.add(themes);
        right.add(simulate);
        toolbar.add(tools, BorderLayout.CENTER);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    private void editAt(MouseEvent e) {
        requestFocusInWindow();
        int x = e.getX() / tileSize;
        int y = e.getY() / tileSize;
        if (SwingUtilities.isRightMouseButton(e) || model.selected == DungeonApp.Tile.EMPTY) {
            model.erase(x, y);
        } else {
            model.place(x, y, model.selected);
        }
        repaint();
    }

    void tick() {
        model.updateSimulation();
        repaint();
    }

    private void saveGame() {
        model.save();
        JOptionPane.showMessageDialog(this, "Saved to ~/neon-depths.save");
    }

    private void loadGame() {
        model.load();
        JOptionPane.showMessageDialog(this, "Loaded from ~/neon-depths.save");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int y = 0; y < model.rows; y++) {
            for (int x = 0; x < model.cols; x++) {
                drawTile(g, x, y, model.grid[y][x]);
            }
        }

        for (DungeonApp.Enemy enemy : model.enemies) {
            int px = (int) (enemy.x * tileSize);
            int py = (int) (enemy.y * tileSize);
            g.setColor(new Color(255, 65, 92));
            g.fillOval(px + 3, py + 3, tileSize - 6, tileSize - 6);
            g.setColor(new Color(255, 185, 195));
            g.fillOval(px + 7, py + 6, 5, 5);
        }

        g.setColor(new Color(15, 18, 30, 220));
        g.fill(new RoundRectangle2D.Double(840, 20, 250, 96, 12, 12));
        g.setColor(new Color(185, 202, 240));
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("CREDITS  " + model.credits, 860, 48);
        g.drawString("REPUTATION " + model.reputation, 860, 70);
        g.drawString("THREAT " + model.threat, 860, 92);
        g.drawString(model.paused ? "SIMULATION PAUSED" : "SIMULATION ACTIVE", 860, 112);

        g.dispose();
    }

    private void drawTile(Graphics2D g, int x, int y, DungeonApp.Tile tile) {
        int px = x * tileSize;
        int py = y * tileSize;

        Color floor = switch (model.theme) {
            case MEDIEVAL -> new Color(74, 66, 58);
            case MODERN -> new Color(82, 88, 96);
            case SCI_FI -> new Color(22, 52, 67);
            case INDUSTRIAL -> new Color(78, 68, 55);
        };

        g.setColor(tile == DungeonApp.Tile.EMPTY ? new Color(9, 12, 20) : floor);
        g.fillRect(px, py, tileSize, tileSize);
        g.setColor(new Color(255, 255, 255, 12));
        g.drawRect(px, py, tileSize - 1, tileSize - 1);

        switch (tile) {
            case WALL -> {
                g.setColor(model.theme == DungeonApp.Theme.MEDIEVAL ? new Color(118, 105, 88)
                        : model.theme == DungeonApp.Theme.SCI_FI ? new Color(78, 139, 160)
                        : new Color(112, 118, 126));
                g.fillRoundRect(px + 2, py + 2, tileSize - 4, tileSize - 4, 3, 3);
                g.setColor(new Color(20, 24, 32, 120));
                g.drawLine(px + 4, py + 10, px + 16, py + 10);
            }
            case FLOOR -> {
                g.setColor(new Color(160, 178, 188, 70));
                g.drawRect(px + 5, py + 5, 9, 9);
            }
            case DOOR -> {
                g.setColor(new Color(230, 173, 64));
                g.fillRect(px + 4, py + 2, 12, 16);
                g.setColor(new Color(54, 38, 20));
                g.fillRect(px + 7, py + 5, 6, 13);
            }
            case SECURITY_GATE -> {
                g.setColor(new Color(255, 93, 102));
                g.fillRect(px + 2, py + 3, 16, 14);
                g.setColor(new Color(35, 20, 28));
                for (int i = 4; i < 17; i += 5) g.drawLine(px + i, py + 4, px + i, py + 16);
            }
            case TRAP -> {
                g.setColor(new Color(255, 201, 72));
                int[] xs = {px + 3, px + 10, px + 17};
                int[] ys = {py + 16, py + 4, py + 16};
                g.fillPolygon(xs, ys, 3);
            }
            case TURRET -> {
                g.setColor(new Color(86, 225, 196));
                g.fillOval(px + 3, py + 3, 14, 14);
                g.setColor(new Color(12, 30, 38));
                g.fillRect(px + 9, py + 7, 8, 5);
            }
            case CONSOLE -> {
                g.setColor(new Color(74, 132, 255));
                g.fillRoundRect(px + 3, py + 4, 14, 12, 3, 3);
                g.setColor(new Color(180, 225, 255));
                g.fillRect(px + 6, py + 7, 8, 4);
            }
            default -> { }
        }
    }
}
