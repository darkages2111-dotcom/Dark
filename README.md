# Neon Depths

A hard-mode 2D dungeon-builder in Java Swing. It is dependency-free and uses no external assets.

## Features

- 42 × 30 grid dungeon editor
- Medieval, modern, sci-fi, and industrial visual themes
- Walls, floors, doors, security gates, traps, turrets, consoles, and rooms
- Limited build budget and faction reputation
- Day/night-style threat phases
- Autonomous defenders and hostile drones
- Undo/redo, save/load, pause, and build/simulate modes
- Procedural generated starting facility

## Run

```bash
mvn compile exec:java
```

With Java 17+ and Maven 3.9+.

## Controls

- Left click/drag: place the selected tool
- Right click: erase
- `Z`: undo
- `Y`: redo
- `Space`: pause/resume simulation
- `Esc`: return to erase tool
- `S`: save
- `L`: load
