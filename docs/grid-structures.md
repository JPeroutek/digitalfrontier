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
| `kind` | yes | `building`, `landmark`, `plaza`, `street`, `terminal` or `portal` (see below). |
| `zones` | yes | Where it can appear: `city_core`, `city`, `outskirts`, `waypoint`, `outlands`, `unallocated`. Buildings only generate where the layout places buildings (city zones, and a few in the outskirts). Plazas appear in the city and at waypoints. |
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
- **`street`**: decorations placed on about a third of straight street chunks. **Author them for a street
  running east–west (along X)**: the street's light lines run along Z = 1 and Z = 14, and the edges at
  Z = 0 and Z = 15 meet the sidewalks. The generator turns them 90° for streets running north–south. Keep the
  middle (Z = 2..13) clear so light cycles can drive through.
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
| `starter_terminal` | terminal | `grid/starter/terminal/platform` (pad at 7, 0, 7) |
| `starter_portal` | portal | `grid/starter/portal/beam` (a 4×4 column of Portal blocks, 47 tall) |

Use them as references, or load one with `/place template digitalfrontier:grid/starter/tower/floor_glass ~ ~ ~`
to see how it's built. When you have your own buildings, lower the starters' weight or remove their
definitions from the provider.
