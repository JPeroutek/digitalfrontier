# TODO

Ideas and planned work for the grid world generator that aren't scheduled into a phase yet. See
`docs/grid-structures.md` for how the current generator and building templates work.

## Tuned grids

- **Idle unloading.** Every tuned grid stays loaded from server start (Fantasy persistent worlds), costing memory
  even when nobody is in it. Unload grids with no players after a while and reopen them on demand (lasers,
  `/grid open`, players logging in inside one). The `digitalfrontierMaxTunedGrids` game rule (default 8) caps
  the cost until then.
- **A price for new grids.** Tuning to a grid that doesn't exist yet is free once you own a Grid Tuning Laser.
  Consider consuming an item per new grid.
- **Settings outside the tuning bounds.** Only the settings a `GridTuning` can express have grid codes; a grid
  written by hand (e.g. a datapack replacing the default grid's settings) can't be shared as a code.

## Multiple street sizes

Today every street is exactly one chunk (16 blocks) wide, because a chunk is either a street or part of a city
block (`GridLayout.planAt`). Wanted: a hierarchy of street sizes, e.g.

- **Avenues / boulevards:** wide main roads, such as the two axis avenues and the roads out to waypoints.
- **Streets:** the current 16-block road.
- **Side streets / alleys:** narrow lanes (4–8 blocks) inside city blocks.

Approaches to weigh:

- **Wide streets as multiple chunk rows.** A boulevard is 2 (or 3) adjacent street rows that share one plan
  (like `MultiChunk` does for buildings). Light lines go on the outer edges, plus a median down the middle.
  Fits the "one chunk = one plan" model without changes.
- **Narrow streets inside a chunk.** A chunk plan gets edge strips: e.g. "this building chunk has a 6-block
  alley along its west edge". Building parts would then need smaller footprints (e.g. 10×16), or a mask that
  clears the alley after placing the part.
- **Variable block sizes.** Instead of one `blockSize` for the whole city, vary the street spacing per row and
  column (still deterministic from `gridSeed`), so some blocks are 2×2 chunks and others 4×4.

Open questions: should `GridLayoutSettings` expose the street hierarchy (for tuned grids), and how should
street furniture be authored for each width (one `street` definition per width class, with a `width` field)?

## Subway and elevated rail

Two rail networks, modeled on Lost Cities' railways (`mcjty/lostcities/worldgen/lost/Railway`,
`RailChunkInfo`). Lost Cities decides per chunk, from coordinates alone, whether a chunk holds a straight rail,
a bend, a junction or a station, and at which level. It then builds the chunk from rail parts. That works here
unchanged, because our generator already plans each chunk independently.

### Shared design

- **A rail plan per chunk.** Add a `RailPlan` (or a field on `ChunkPlan`): `NONE`, `STRAIGHT_X`, `STRAIGHT_Z`,
  `JUNCTION`, `STATION`, `RAMP` (changing level), plus the level (subway or elevated). It's computed in
  `GridLayout` from coordinates and `gridSeed` only.
- **Lines follow the street grid.** Rails run under or over street rows/columns (e.g. every second or third
  street line), so they stay straight and chunk-aligned and never cut through building foundations. Lines
  extend past the city along the highways to the waypoints (Phase 5).
- **New template kinds.** `GridBuildingDef.Kind` gains `RAIL` and `STATION`, authored along X like street
  furniture and rotated for Z. Junctions and ramps get their own kinds or a `rail_shape` field.
- **Stations.** Placed at intersections or chosen city blocks at a regular spacing, with stairs or elevators
  up to street level. Waypoints and the arrival terminals get a station, so the rail network doubles as
  fast travel.
- **Rideable.** Start with vanilla minecarts on powered rails. Later: a custom light-rail train entity.

### Subway

- A tunnel band below the street, e.g. Y 44–56. The ground under Y 64 is currently solid gridstone
  (`GridTerrain`), so the tunnel templates carve their space out with air.
- Building bases above a subway line may need a basement-free variant, or the rail plan simply never places
  tunnels under building chunks (only under streets).
- Optional "subway dungeons" (like Lost Cities' tunnel corridors): side passages and maintenance rooms, a good
  home for corrupted programs at higher corruption tiers.

### Elevated rail

- A viaduct at a fixed height above the street, e.g. track at Y 80, on pillars that land on the street
  between the light lines (keep the light-cycle lanes clear).
- Runs over avenues and highways, so it reads clearly from the ground and can glow in the district color.
- Where an elevated line meets a tall building it must pass beside it; following street lines avoids this.
- `GridChunkGenerator.getHeight` and heightmaps must account for viaducts so mobs don't spawn on the track.

### Order of work

1. `RailPlan` in `GridLayout` with unit tests: lines are continuous, junctions connect, stations are spaced
   out.
2. Placeholder rails built in code (like Phase 1's placeholder towers) to check the layout in-game.
3. Rail, station and ramp templates and definitions, plus authoring notes in `grid-structures.md`.
4. Trains.
