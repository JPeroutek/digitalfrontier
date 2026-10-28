# Building grid structures

How to design buildings, plazas and street furniture for the Digital Frontier in-game, save them as structure
templates (`.nbt`), and wire them into world generation.

## How the generator uses your structures

The grid is generated chunk by chunk (16×16 blocks). The layout decides what each chunk is: a street, a
building, a plaza, and so on. For building, plaza and street chunks, the generator looks for a **building
definition** (a small JSON file) that matches the chunk. Each definition is a **vertical stack of parts**, and
each part is one structure template:

```
        roof            ← optional, placed once on top
        floor           ┐
        floor           │ repeated: the layout picks how many (clamped by min_floors/max_floors),
        floor           │ each floor picks one of the listed floor parts at random
        floor           ┘
        base            ← optional, placed once at street level
  ═══════════════════   ← the street surface (Y 64), generated for you
```

- **Single-chunk** parts are exactly **16×16** blocks.
- **Multi-chunk** parts (2×2 chunks) are exactly **32×32** blocks.
- The height of each part is up to you. Floors of 6 blocks match the layout's height profile, but anything works.
- The stack starts at **Y 65**, the first block above the street surface. Part Y=0 lands there.
- Parts are **rotated** randomly in 90° steps (except street furniture, see below). Blocks with a facing
  direction (stairs, doors, etc.) are rotated to match.

If no definition matches a chunk, the generator falls back to the plain placeholder towers, so the world
always generates.

## District colors: build with white

Every chunk belongs to a district with a color (light blue, cyan, blue, orange by default). When a part is
placed, **every block whose id contains `white` is swapped for the district's color**, if that colored
block exists. Properties like facing and slab type are kept.

| You place | In a blue district it becomes |
|---|---|
| `digitalfrontier:gridstone_pattern_full_white` | `digitalfrontier:gridstone_pattern_full_blue` |
| `minecraft:white_stained_glass` | `minecraft:blue_stained_glass` |
| `minecraft:white_stained_glass_pane` | `minecraft:blue_stained_glass_pane` |
| `minecraft:white_concrete` | `minecraft:blue_concrete` |
| `minecraft:white_carpet`, `white_wool`, `white_terracotta`, … | the matching blue block |

So build all the parts that should follow the district color in **white**, and use any other color for parts
that should always stay that color (for example red warning lights). Pattern gridstone glows and gives off
light, so the white pattern blocks are your light lines.

## Air vs. structure void

- **Air** in a template is placed as air: it carves out space. Chunks start empty above the street, so this
  only matters when parts overlap.
- **Structure void** (and anywhere the template has no block) is **left alone**. Structure blocks skip
  structure void when saving.

Fill the empty space inside your bounding box with air (the default) unless you specifically want "don't touch".

## Setting up a build world

1. Run the dev client: `./gradlew runClient`.
2. Create a **Creative, Superflat** world with cheats on. A *Redstone Ready* or *Void* preset works well.
3. Turn on chunk borders with **F3 + G**. Build each part exactly on one chunk (or a 2×2 group of chunks), so
   the 16×16 boundary is easy to see.
4. Optional: get a reference street level by building on a layer of gridstone and treating the block above it
   as part Y=0.

WorldEdit is on the dev runtime, so `//wand`, `//set`, `//walls`, `//copy`, `//paste` and `//stack` all work
for building. **Save with a structure block, not WorldEdit**: WorldEdit's `//schem save` writes `.schem`
files, which the generator can't read.

## Saving a part with a structure block

1. Get a structure block: `/give @s minecraft:structure_block`.
2. Place it **outside** the part, for example one block below the part's north-west bottom corner.
3. Open it (right-click) and set the mode to **Save** (cycle the mode button in the bottom-left).
4. **Structure Name**: use the id the part will have, in the `digitalfrontier` namespace under `grid/`,
   for example:
   ```
   digitalfrontier:grid/neon_tower/floor_a
   ```
5. **Relative Position**: the offset from the structure block to the part's north-west bottom corner, e.g.
   `0 1 0` if the block sits directly below that corner.
6. **Structure Size**: `16 <height> 16` (or `32 <height> 32` for a 2×2 part). Use **Detect** if the part is
   enclosed by structure blocks in Corner mode. The width and depth must be exactly 16 or 32, or the
   definition that uses the part is rejected (see the log).
7. Turn **Show Bounding Box** on to check the outline sits exactly on the chunk borders.
8. Turn **Include Entities** off. Entities are not placed by the generator.
9. Press **SAVE**.

The file is written to:

```
run/saves/<world name>/generated/digitalfrontier/structure/grid/neon_tower/floor_a.nbt
```

Copy it into the mod's resources at the matching path:

```
src/main/resources/data/digitalfrontier/structure/grid/neon_tower/floor_a.nbt
```

The id stays the same: `digitalfrontier:grid/neon_tower/floor_a`.

To **edit** an existing part later, load it back with a structure block in **Load** mode (same name), or run
`/place template digitalfrontier:grid/neon_tower/floor_a ~ ~ ~`, change it, and save again.

## Writing the building definition

Create `src/main/resources/data/digitalfrontier/grid/building/<name>.json`. The file name is the building's
id (`digitalfrontier:<name>`), which F3 shows when you stand on it.

```json
{
  "kind": "building",
  "zones": ["city_core", "city"],
  "weight": 10,
  "footprint": 1,
  "base": "digitalfrontier:grid/neon_tower/base",
  "floors": [
    "digitalfrontier:grid/neon_tower/floor_a",
    "digitalfrontier:grid/neon_tower/floor_b"
  ],
  "roof": "digitalfrontier:grid/neon_tower/roof",
  "min_floors": 2,
  "max_floors": 20
}
```

| Field | Required | Meaning |
|---|---|---|
| `kind` | yes | `building`, `landmark`, `plaza`, `street`, `terminal`, `portal` or `stronghold` (see below). |
| `zones` | yes | Where it can appear: `city_core`, `city`, `outskirts`, `waypoint`, `outlands`, `unallocated`, `derelict`. Buildings only generate where the layout places buildings: city zones, a few in the outskirts, the low buildings around each waypoint (1–3 floors), and the ruins on derelict sectors (1–4 floors, always badly corrupted). Derelict sectors are islands of old floor far out over the Sea of Simulation, past where unallocated space crumbles away. Plazas appear in the city and at waypoints; street furniture on city and waypoint streets. |
| `weight` | no (10) | Relative chance against other matching definitions. |
| `footprint` | no (1) | `1` for 16×16 parts, `2` for 32×32 parts. |
| `base` | no | Part placed once at street level. |
| `floors` | no | Parts for the repeated middle section. Each floor picks one at random. |
| `roof` | no | Part placed once on top. |
| `min_floors` / `max_floors` | no (0 / 64) | Clamp on the floor count the layout asks for. The layout asks for up to 24 floors in the core, tapering to about 3 at the city edge. |

At least one of `base`, `floors` or `roof` is required. Floors stop early rather than build past the top of
the world (Y 319).

### Kinds

- **`building`**: any building chunk in its zones. With `footprint: 2`, it is used for the 2×2 building lots
  the layout reserves in city blocks.
- **`landmark`**: the 2×2 tower at the center of the city (chunks 1..2, 1..2). It must have `footprint: 2`. Give it
  a high `min_floors`; the starter landmark uses 32–40.
- **`plaza`**: open squares in the city, and the open areas at waypoints. Usually only a `base` (benches, light
  posts, a monument).
- **`street`**: decorations for road chunks (never intersections). **Author them for a road running east–west
  (along X)**; the generator turns them for roads running north–south. The optional `road` field says which kind
  of road they're for (default `street`):
  - `street` (one chunk): placed on about a third of straight street chunks. Light lines run along Z = 1 and
    Z = 14; keep the middle (Z = 2..13) clear so light cycles can drive through.
  - `boulevard` (two chunks): author **one half**, with its outer edge at Z = 0 and the median at Z = 15. The
    other half gets the same part turned 180°, so the medians meet. Also placed on about a third of chunks.
  - `lane` (one chunk): **always** placed on straight lane chunks when one exists, because the strips either
    side of the lane are what make it narrow. The roadway is Z = 5..10 (lit at 5 and 10), with a sidewalk at
    Z = 4 and 11; fill Z = 0..3 and 12..15 with walls, planters, kiosks and the like, leaving gaps to reach the
    buildings behind. Without a lane part, the generator puts low walls there.
- **`terminal`**: the building on an arrival terminal's chunk: the main terminal just outside the city (zone
  `outskirts`) and the center of each waypoint (zone `waypoint`). Single chunk only. Place one
  **Arrival Pad** (`digitalfrontier:arrival_pad`) where players should appear: they arrive standing on top of
  it, facing the way they were facing. If a part has several pads, the first one found is used; if the
  stack has none, players arrive in the middle of the chunk on top of whatever is there. Leave at least two
  blocks of air above the pad.
- **`portal`**: the building at a grid's Portal site, far out in the outlands (zone `outlands`). Single chunk
  only. Any **Portal** block (`digitalfrontier:grid_portal`) in it sends a player who walks into it back to
  their respawn point in the real world. Portal blocks have no collision, so make sure players can walk into
  at least one at ground level. Make it tall and bright: players have to find it from a distance.
- **`stronghold`**: the building at a stronghold site, at the heart of the grid's most corrupted territory
  (usually zone `outlands`). Single chunk only; use `min_floors` to set its height, since the site asks for none.
  Colors other than white aren't recolored, so the regime's red stays red.

## Street layout

Grids have one of two street styles (`street_style` in the grid settings, or the Streets button on the Grid
Tuning Laser):

- **`varied`** (the default grid): lanes, streets and two-chunk boulevards, with city blocks of 2 to 5 chunks
  (around the `block_size` setting) between them. A boulevard runs through the city center along both axes.
- **`uniform`**: a one-chunk street every `block_size + 1` chunks, as grids made before street styles have.

2×2 buildings fit in any block at least 2 chunks wide both ways.

## The city's levels

Above and below the streets, grids can have (`levels` in the grid settings, or the Levels button on the tuning
laser; the default grid has all four):

| Level | Height | Where |
|---|---|---|
| Skyroads | drive at Y 80 | a 16-wide deck over every boulevard, on pillars along the median; on-ramps climb a boulevard's outer lane, and the deck ramps down at the city edge |
| Elevated rail | rails at Y 88 | over some one-chunk streets running north–south, with stations at intersections |
| Subway | rails at Y 48 | under some one-chunk streets running east–west, with stations at intersections; plus an express line from just outside the city to each waypoint |
| Skyways | Y 101, 107 or 113 (and sometimes a second, two floors higher) | bridges across a street between two towers, at their floor heights; the towers get doorways |

The levels are placed over (and carved into) whatever building and street parts put there, so **leave the space
clear**: in tall buildings, keep floors at 6-block intervals from Y 65 (so the floor plates skyways land on are at
Y 100, 106, 112, 118, 124) and expect a 4-wide doorway in the middle of an outer wall at those heights.

**Stations** have two spiral staircases (a 4×4 shaft around a lit pillar) in opposite corners of the chunk,
(0..3, 0..3) and (12..15, 12..15), and glowing signs with a generated station name and its line: city lines are
named by color ("Cyan Line"), express lines by their waypoint. Subway signs hang on the platform walls; elevated
station signs stand on the platforms either side of the tracks.

**Express lines** run at subway depth from a station just outside the city edge to a station beside their waypoint.
Lines to waypoints on the axes run straight out; lines to diagonal waypoints run along a column two chunks off the
Z axis, then turn (a curved corner) along the waypoint's row. They never meet each other or the city's lines.
Their staircases climb to wherever the ground is, so express termini are always built in code.

**Maintenance passages** open off about one in seven ordinary tunnel chunks: a small lit room behind a doorway in
the tunnel wall. In unstable ground or worse, a corrupted program lurks in each one (one that fights back where
the ground is corrupted).

### Authoring level pieces

Each level piece is a building definition of one of the kinds below, with only a `base` part (16×16, footprint 1).
Unlike buildings, a piece isn't stacked from the street: its part Y = 0 goes at a fixed height. **Author every piece
for a line running east–west (along X)**; the generator turns it for lines along Z, turns it 180° for the other half
of a boulevard, and flips on-ramps to climb either way. Anything a template leaves as structure void keeps what's
there; use air to carve (tunnels, headroom).

| Kind | Part Y = 0 at | Author it as |
|---|---|---|
| `skyroad` | Y 65 | one deck half: median at Z = 15, outer edge at Z = 8, deck surface (the block you drive on) at part Y 14; pillars down to Y 0 |
| `skyroad_junction` | Y 65 | a quarter of a junction: both medians meet at X = 15, Z = 15 |
| `skyroad_ramp_lower` / `skyroad_ramp_upper` | Y 65 | the two chunks of an on-ramp in Z = 2..7, climbing half a block per block toward +X: from the street at the lower part's X = 0 to deck height (part Y 14) at the upper part's X = 14. Carve the deck railing (Z = 8, part Y 15) where the ramp joins it. |
| `guideway` | Y 65 | tracks along X at Z = 6 and 9 on part Y 23 (Y 88), with supports |
| `guideway_crossing` | Y 65 | the same, over an intersection: no supports in the crossing road |
| `guideway_station` | Y 65 | a station over an intersection, with a way down to the street; leave X = 7..8, Z = 2 and 13 at part Y 23 clear for the signs |
| `subway` | Y 47 | the tunnel: floor at Y 0, tracks along X at Z = 6 and 9 on Y 1; carve it with air |
| `subway_station` | Y 47 | a station under an intersection; part Y 17 is the street surface, where its way up comes out. Leave X = 7..8 at part Y 3 clear against both walls (Z = 1 and 14): the station signs go there |
| `subway_corner` | Y 47 | a bend in an express line: tracks come in from the south (Z = 15) at X = 6 and 9 and leave east (X = 15) at Z = 6 and 9 |
| `subway_passage` | Y 47 | a maintenance room beside a tunnel, toward Z = 0, with its doorway through the tunnel wall at Z = 3. It's placed before the tunnel, so its blocks win; it's mirrored for rooms on the other side |
| `skyway` | the bridge's floor | a bridge across a road along X, so it runs along Z (Z = 0..15); walk on part Y 1 |

Rails and other directional blocks turn with the part. White blocks take the district color as usual. Powered rails
need their redstone; the starter pieces power rails every 8 blocks (X = 0 and 8). Where a piece has no template, the
generator builds the starter shape in code, and the end ramps (where a deck comes down at the city edge) are always
built in code.

## Corruption

Corruption is applied to your parts after they're placed and recolored, so you never build corrupted versions.
Where a grid is corrupted (shown on F3 as `Grid corruption`), the same template comes out:

| Tier | What happens to your blocks |
|---|---|
| LOW | Pattern lines show the wrong color; gridstone and gridstone bricks crack. |
| UNSTABLE | Holes through walls and floors; 4×4 columns of the building slide up or down; pattern lines glitch. |
| CORRUPTED, SEVERE | Blocks turn to static; some buildings break off partway up and float apart in fragments. |

Site buildings (terminals, the Portal, strongholds) keep their shape: only their materials corrupt. Arrival Pads
and Portal blocks are never changed.

## Testing your structures

1. Run `./gradlew runClient` (or `runServer`). On startup the log reports
   `Loaded N grid parts and M grid buildings`. Any problem with a definition is logged as
   `Skipping grid building …` with the reason (a missing part, wrong part size, invalid JSON).
2. Changes only affect **chunks generated after the change**. To see a new building, either:
   - delete the grid's chunks in your test save: `run/saves/<world>/dimensions/digitalfrontier/default`, or
   - travel to chunks you haven't visited yet.
3. `/reload` reloads parts and definitions without restarting, but again only new chunks use them.
4. Use the **Grid Teleporter (Debug)** item to jump to the main arrival terminal, then fly into the city.
   **F3** shows, for the chunk you're in, the zone, the plan type, the district color, the building id
   (`Grid building: …`), and for 2×2 buildings the origin chunk.
5. To make a new definition show up often while testing, give it a large `weight` (e.g. 1000) and set it back
   afterwards.

## Limitations (for now)

- **Block entity contents are not placed.** Chests, signs and similar blocks are placed empty or blank.
- **Entities in templates are ignored.**
- Parts can't hang outside their footprint. A 16×16 part can't overhang into the next chunk; use a 2×2
  footprint for anything bigger.
- Only one rotation is applied per building, so the whole stack (base, floors, roof) turns together.

## Starter set

The mod ships a small generated placeholder set under `grid/starter/` (made in code by
`datagen/grid/ModGridStarterProvider.java`):

| Definition | Kind | Parts |
|---|---|---|
| `starter_tower` | building, 16×16 | `grid/starter/tower/base`, `floor_glass`, `floor_striped`, `roof` |
| `starter_block_tower` | building, 32×32 | `grid/starter/block_tower/base`, `floor`, `roof` |
| `starter_core_tower` | landmark, 32×32 | the block tower parts, 32–40 floors |
| `starter_plaza` | plaza | `grid/starter/plaza/square` |
| `starter_street_lights` | street | `grid/starter/street/lights` |
| `starter_boulevard` | street (`road`: boulevard) | `grid/starter/street/boulevard_half` |
| `starter_lane` | street (`road`: lane) | `grid/starter/street/lane` |
| `starter_skyroad`, `starter_skyroad_junction`, `starter_skyroad_ramp_lower`, `starter_skyroad_ramp_upper`, `starter_guideway`, `starter_guideway_crossing`, `starter_guideway_station`, `starter_subway`, `starter_subway_station`, `starter_subway_corner`, `starter_subway_passage`, `starter_skyway` | the level kinds | `grid/starter/level/<kind>` |
| `starter_terminal` | terminal | `grid/starter/terminal/platform` (pad at 7, 0, 7) |
| `starter_stronghold` | stronghold | `grid/starter/stronghold/base`, `floor`, `roof` (3–5 floors) |
| `starter_portal` | portal | `grid/starter/portal/beam` (a 4×4 column of Portal blocks, 47 tall) |

Use them as references, or load one with `/place template digitalfrontier:grid/starter/tower/floor_glass ~ ~ ~`
to see how it's built. When you have your own buildings, lower the starters' weight or remove their
definitions from the provider.
