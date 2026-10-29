# Development and testing

`./gradlew runClient` and `./gradlew runServer` start the game with the mod and a set of testing mods. The testing
mods are `localRuntime` dependencies in `build.gradle`: they only load in these dev runs, and are never compiled
against, published, or bundled in the mod's jar.

| Mod | What it's for |
|---|---|
| WorldEdit | Building and editing templates (see [grid-structures.md](grid-structures.md)) |
| Xaero's Minimap and World Map | Checking grid layouts from above |
| Jade | Tooltips naming the block or entity you're looking at |
| REI (Roughly Enough Items) | Item and recipe lookup; chosen when neither JEI nor EMI had a Fabric build (EMI still has none for 26.1.2) |
| Sodium | Frame rate |
| Trinkets Updated | Trying the kept-trinkets integration: equip a trinket, die in a grid holding a Core Dump |
| Distant Horizons | Seeing far out, e.g. the derelict sectors from the city; set its render distance in its settings |
| Chunky | Pregenerating chunks (only with `-Ppregen`, see below) |

## Pregenerating the grid

Distant Horizons and Chunky crash together (Distant Horizons calls into Chunky before Chunky has started), so a run
has one or the other. Runs have Distant Horizons by default; add `-Ppregen` for Chunky instead:

```
./gradlew runClient -Ppregen
```

(or `./gradlew runServer -Ppregen` for a dedicated server). Then, in chat with cheats on, or in the server console:

```
/chunky world digitalfrontier:default
/chunky radius 3000
/chunky start
```

A radius of 3000 blocks covers everything out to the far reach of the default grid: the city, the outlands (to about
2,050 blocks out), unallocated space crumbling over the Sea of Simulation, and the first derelict sectors (from about
2,240). Chunky's default shape is a square, so that's about 142,000 chunks: around 13 minutes, at 100–180 chunks a
second (`/chunky shape circle` first cuts it to about 110,000). `/chunky pause`,
`/chunky continue` and `/chunky cancel` control a running task, and progress shows in chat and the log. A tuned grid
is its own dimension: use its id (shown when the Grid Tuning Laser is tuned) in place of `digitalfrontier:default`.

The chunks stay in the world, so afterwards a normal `./gradlew runClient` opens it with Distant Horizons. Leave
Distant Horizons time to build its distant terrain from them the first time.

## Notes on the testing mods

- **Version ids.** Chunky and Distant Horizons are pinned by Modrinth version id rather than version number: Chunky
  gives its Fabric, Forge and NeoForge builds the same number, and Modrinth's maven may otherwise serve the wrong one.
