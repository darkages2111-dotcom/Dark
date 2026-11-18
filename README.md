# Neon Depths — Dungeon Management

A Java 17 dungeon-management prototype with a persistent world, four settings, several modes, resource chains, research, defenders, raids, morale, and a buildable tile map. No third-party game engine or art assets required.

## Run

```bash
mvn compile exec:java
```

Requires Java 17+ and Maven 3.9+.

## What you manage

- **Settings:** Sci-fi, Steampunk, Fantasy, and Dark Ages themes.
- **Modes:** Campaign (survive 30 days), Endless (keep growing), Siege (survive 20 days of escalating attacks), and Sandbox.
- **Difficulty:** Relaxed, Standard, Brutal, and Nightmare change starting supplies and raid pressure.
- **Economy:** Gold, stone, iron, mana, and food. Treasury, farms, workshops, libraries, and temples provide different production and support.
- **People:** Recruit guards, hatch minions, protect the Dungeon Heart, and keep morale and food supplies up.
- **Threats:** Raids arrive over time. Defenders fight, invaders damage rooms on their path, traps hurt them, and a breach can damage morale.
- **Construction:** 20 tile types including walls, doors, gates, rooms, traps, turrets, portals, water, lava, and crystals.

## Controls

- Select a build tile in the Overseer Console, then click or drag on the map.
- Right-click to erase a tile.
- **Advance day** runs the economy and threat forward. **Start / pause** runs or pauses the automatic clock.
- Save and Load use `~/neon-depths.save`.

The game is a prototype: themes change the visual palette, while the economy, raids, objectives, and difficulty are shared across settings.
