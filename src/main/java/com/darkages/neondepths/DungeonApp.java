package com.darkages.neondepths;

import java.util.ArrayList;
import java.util.List;

public final class DungeonApp {
    private final javax.swing.JFrame frame;

    public DungeonApp() {
        frame = new javax.swing.JFrame("Neon Depths — Dungeon Architect");
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new java.awt.BorderLayout());

        DungeonModel model = new DungeonModel(42, 30);
        DungeonPanel panel = new DungeonPanel(model);
        frame.add(panel, java.awt.BorderLayout.CENTER);
        frame.add(panel.createToolbar(), java.awt.BorderLayout.NORTH);

        javax.swing.Timer timer = new javax.swing.Timer(50, event -> panel.tick());
        timer.start();

        frame.setSize(1120, 780);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static final class DungeonModel {
        final int cols;
        final int rows;
        final Tile[][] grid;
        final List<Enemy> enemies = new ArrayList<>();
        Tile selected = Tile.FLOOR;
        Theme theme = Theme.SCI_FI;
        int credits = 2400;
        int reputation = 100;
        int threat;
        int tick;
        boolean paused;

        final java.util.Deque<Tile[][]> undo = new java.util.ArrayDeque<>();
        final java.util.Deque<Tile[][]> redo = new java.util.ArrayDeque<>();

        DungeonModel(int cols, int rows) {
            this.cols = cols;
            this.rows = rows;
            grid = new Tile[rows][cols];
            generate();
        }

        void generate() {
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    grid[y][x] = (x == 0 || y == 0 || x == cols - 1 || y == rows - 1)
                            ? Tile.WALL : Tile.EMPTY;
                }
            }

            room(3, 3, 10, 8, Tile.FLOOR);
            room(15, 5, 12, 7, Tile.FLOOR);
            room(29, 3, 9, 10, Tile.FLOOR);
            room(9, 16, 13, 10, Tile.FLOOR);
            room(25, 17, 13, 8, Tile.FLOOR);

            corridor(12, 7, 15, 7);
            corridor(27, 8, 29, 8);
            corridor(21, 12, 21, 17);
            corridor(14, 21, 25, 21);

            grid[7][13] = Tile.DOOR;
            grid[8][27] = Tile.SECURITY_GATE;
            grid[21][21] = Tile.DOOR;
            grid[18][29] = Tile.TURRET;
            grid[6][31] = Tile.CONSOLE;
            grid[20][11] = Tile.TRAP;
            grid[22][31] = Tile.CONSOLE;

            enemies.add(new Enemy(4, 4));
            enemies.add(new Enemy(36, 24));
        }

        private void room(int x, int y, int w, int h, Tile floor) {
            for (int yy = y; yy < y + h; yy++) {
                for (int xx = x; xx < x + w; xx++) {
                    grid[yy][xx] = (xx == x || yy == y || xx == x + w - 1 || yy == y + h - 1)
                            ? Tile.WALL : floor;
                }
            }
        }

        private void corridor(int x1, int y1, int x2, int y2) {
            int x = x1;
            int y = y1;
            while (x != x2) {
                grid[y][x] = Tile.FLOOR;
                x += Integer.compare(x2, x);
            }
            while (y != y2) {
                grid[y][x] = Tile.FLOOR;
                y += Integer.compare(y2, y);
            }
            grid[y][x] = Tile.FLOOR;
        }

        boolean canPlace(Tile tile) {
            int cost = tile.cost;
            return cost <= credits;
        }

        void place(int x, int y, Tile tile) {
            if (x <= 0 || y <= 0 || x >= cols - 1 || y >= rows - 1) return;
            if (!canPlace(tile)) return;

            saveUndo();
            credits -= tile.cost;
            grid[y][x] = tile;
            reputation = Math.max(0, reputation + tile.reputation);
            redo.clear();
        }

        void erase(int x, int y) {
            if (x <= 0 || y <= 0 || x >= cols - 1 || y >= rows - 1) return;
            Tile old = grid[y][x];
            if (old == Tile.EMPTY) return;
            saveUndo();
            credits += Math.max(0, old.cost / 3);
            grid[y][x] = Tile.EMPTY;
            redo.clear();
        }

        void saveUndo() {
            Tile[][] copy = new Tile[rows][cols];
            for (int y = 0; y < rows; y++) copy[y] = grid[y].clone();
            undo.push(copy);
            if (undo.size() > 40) undo.removeLast();
        }

        void undo() {
            if (undo.isEmpty()) return;
            saveRedo();
            restore(undo.pop());
        }

        private void saveRedo() {
            Tile[][] copy = new Tile[rows][cols];
            for (int y = 0; y < rows; y++) copy[y] = grid[y].clone();
            redo.push(copy);
        }

        void redo() {
            if (redo.isEmpty()) return;
            saveUndo();
            restore(redo.pop());
        }

        private void restore(Tile[][] copy) {
            for (int y = 0; y < rows; y++) grid[y] = copy[y].clone();
        }

        void updateSimulation() {
            tick++;
            if (paused) return;
            if (tick % 160 == 0) {
                threat++;
                credits += 35;
                if (threat > 0 && threat % 4 == 0) {
                    enemies.add(new Enemy(cols - 2, 1 + (threat * 7) % (rows - 2)));
                }
            }

            for (Enemy enemy : new ArrayList<>(enemies)) {
                enemy.moveToward(cols / 2.0, rows / 2.0);
                int x = Math.round((float) enemy.x);
                int y = Math.round((float) enemy.y);
                if (x > 0 && y > 0 && x < cols && y < rows) {
                    Tile tile = grid[y][x];
                    if (tile == Tile.TRAP) {
                        enemy.health -= 60;
                        grid[y][x] = Tile.FLOOR;
                        reputation += 2;
                    } else if (tile == Tile.TURRET) {
                        enemy.health -= 35;
                    } else if (!tile.solid) {
                        if (x == cols / 2 && y == rows / 2) reputation--;
                    }
                }
                if (enemy.health <= 0) enemies.remove(enemy);
            }
        }

        void save() {
            java.nio.file.Path path = java.nio.file.Path.of(System.getProperty("user.home"), "neon-depths.save");
            try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(java.nio.file.Files.newOutputStream(path))) {
                out.writeInt(cols); out.writeInt(rows); out.writeObject(grid); out.writeInt(credits);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        void load() {
            java.nio.file.Path path = java.nio.file.Path.of(System.getProperty("user.home"), "neon-depths.save");
            if (!java.nio.file.Files.exists(path)) return;
            try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(java.nio.file.Files.newInputStream(path))) {
                in.readInt(); in.readInt();
                Tile[][] loaded = (Tile[][]) in.readObject();
                if (loaded.length == rows && loaded[0].length == cols) {
                    for (int y = 0; y < rows; y++) grid[y] = loaded[y].clone();
                    credits = in.readInt();
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    enum Theme { MEDIEVAL, MODERN, SCI_FI, INDUSTRIAL }

    enum Tile {
        EMPTY("Void", 0, 0, true, false), FLOOR("Floor", 8, 0, false, true),
        WALL("Wall", 25, 0, true, false), DOOR("Door", 50, 1, false, true),
        SECURITY_GATE("Gate", 90, 2, true, true), TRAP("Trap", 75, 3, false, true),
        TURRET("Turret", 180, 5, true, false), CONSOLE("Console", 140, 4, true, false);

        final String name;
        final int cost;
        final int reputation;
        final boolean solid;
        final boolean buildable;

        Tile(String name, int cost, int reputation, boolean solid, boolean buildable) {
            this.name = name;
            this.cost = cost;
            this.reputation = reputation;
            this.solid = solid;
            this.buildable = buildable;
        }
    }

    static final class Enemy {
        double x;
        double y;
        int health = 100;

        Enemy(double x, double y) { this.x = x; this.y = y; }

        void moveToward(double targetX, double targetY) {
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance > 0.4) {
                x += dx / distance * 0.045;
                y += dy / distance * 0.045;
            }
        }
    }
}
