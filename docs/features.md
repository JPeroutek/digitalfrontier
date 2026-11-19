# Features and current state

What the Digital Frontier does today (branch `26.3`, Minecraft 26.3, Fabric), and what's still rough. For how to play
it, see [getting-started.md](getting-started.md) and [progression.md](progression.md). For building content, see
[grid-structures.md](grid-structures.md), [lieutenants.md](lieutenants.md) and [regime.md](regime.md).

The mod's own names are franchise-agnostic (the Warden, Bool, Data Disc, System Core). The built-in **Lore Names**
pack (a datapack plus a resource pack, both `digitalfrontier:lore`, off by default) swaps in the films' names and
skins: Rinzler, Bit, Identity Disc, Master Control Program, Tower Guardian, Clu.

## Getting in and out

| Feature | What it does |
|---|---|
| **Digitizing Laser** | A block in the real world. Use it and stand still within 4 blocks for 2 seconds to be digitized into its linked grid terminal. Unlinked, it links itself to the default grid's terminal nearest to it. |
| **Laser Tuner** | Use on a laser to cycle its link through the nearest terminal and every terminal you've discovered. |
| **Grid Tuning Laser** | A late-game laser that opens a screen to tune it to *any* grid: your own generated world, from a seed and settings (see below). |
| **Terminals** | Arrival points in a grid: the City Terminal at the center and numbered Waypoints out in the outlands. Coming within 2 chunks of one discovers it, so lasers can send you there. |
| **Portal** | A beam of light at the grid's far edge. Step into it to leave the grid, once its toll is paid: the first player to step in holding an Escape Key spends it, and the Portal stays open to everyone from then on. |
| **Escape Key** | Single use: leaves the grid from anywhere. |
| **Memory Paging** / **Core Dump** | Carried when you die in a grid, it respawns you at the last terminal you arrived at instead of the real world. Memory Paging drops your items as usual; a Core Dump keeps your inventory and experience (and Trinkets Updated slots). A Data Disc in your offhand stands in for a Core Dump. |

## The grid

Each grid is its own dimension, generated chunk by chunk from a seed and a handful of settings.

- **Regions**, outward from the center: the City Core, the City (four color districts, varied streets), the
  Outskirts, the Outlands (highways out to Waypoint settlements), Unallocated Space crumbling over the Sea of
  Simulation, and the Derelict Sectors beyond.
- **The city's levels:** skyroads, elevated rail, a subway (with express lines out to every Waypoint) and skyways
  between towers. Each can be turned off per grid.
- **Buildings** are data: structure templates stacked into buildings by JSON definitions, recolored to their
  district (see [grid-structures.md](grid-structures.md)). The starter set includes a garage and the Portal.
- **Grid settings** (tuning laser screen, or a `DF4.` grid code you can copy and share): seed, city radius, street
  style, levels, district colors, waypoints, **Decay** and **Regime**.
- **Commands** (`/grid`): `list`, `open`, `delete`, `code`, `locate <grid> <site>` (terminals, `portal_0`,
  `stronghold_N`, `garage`), `portal status|open|seal <grid>`, plus the `lieutenant`, `regime` and `core` groups below.
- F3 (with the `digitalfrontier:grid` debug section on) shows the chunk's decay and control and your regime alignment.

## Two forces: decay and the regime

The grid has two separate but correlated axes (details: [grid-structures.md](grid-structures.md#decay-and-the-regime)).

- **Decay** rises with distance from the city, in blobs. Decayed ground shows flickering and stray-colored pattern
  lines, cracked gridstone, holes, shifted columns, then static creeping in and buildings breaking apart. Corrupted
  programs live there.
- **The regime** holds territory around its **strongholds**: clean, orderly ground with red pattern lines, patrolled
  by enforcers. Strong control suppresses decay, so the regime's land is almost never decayed, and its roads never are.

## Inhabitants

| Who | Behavior |
|---|---|
| **Programs** | The grid's citizens. Each has a hidden **sentiment** toward the regime (−1 to 1): loyalists report rebel players to enforcers, rebels flee patrols, and the resistance fights recruiters. Gifts of Luminessence and defending them win them toward your side. Near decay or corrupted programs they slowly **drift into decay**, jittering and fraying until they turn. |
| **Corrupted Programs** | Decayed code: they jitter, freeze when watched, echo and stalk. A **Disinfection Pad** cures one that steps on it. |
| **Enforcers** | The regime's patrols, in controlled land, in **squads** with a leader (kill it and the rest scatter) and roles: staff, heavy, grenadier (see [regime.md](regime.md#squads)). They attack players the regime doesn't tolerate, hunt rebels, and hunt *wanted* players from twice as far. |
| **Recognizers** | Flying arches piloted by enforcers, patrolling the regime's land with a searchlight and dropping squads on Chutes onto spotted players. Kill the pilot and you can fly it. |
| **Recruiters** | Grow the regime: programs that linger near one, or accept its bribes, are **conscripted** into enforcers, more eagerly while the regime is short of members. Pay one tribute in Luminessence to raise your standing. |
| **Lieutenants** | A boss in each stronghold (The Warden, The Sovereign), data-driven, with respawn and **redemption** triggers (see [lieutenants.md](lieutenants.md)). Redeemed, a lieutenant turns on the regime. |
| **The System Core** | The final boss, sealed under the city center until every lieutenant falls. |
| **Bool** | A small flying companion, tamed with Luminessence. It turns red and buzzes near threats, and shows yellow "yes" when you face the nearest stronghold (or the awake core). |
| **Mechanic** / **Portal Keeper** | Job programs who trade, at prices that depend on how your alignment compares with their sentiment. |

**Derezzing:** everything that dies in a grid (players and vanilla mobs too), and the mod's own mobs anywhere, comes
apart into cubes instead of tipping over and vanishing in smoke. By default it becomes a voxel statue of itself (its
own model and skin, players' skins included, with its armour and held items, rebuilt in cubes) that holds a moment
and then flies apart from the head down. Three other styles stay available with `/derez style
<voxel|sweep|instant|freeze>`: a sweep dissolving the body from the head down, an instant burst, or a white-flashing
freeze before the burst. The choice is per client and kept in `config/digitalfrontier-client.properties`.

Your **alignment** with the regime (per player, per grid, −1 to 1) moves with what you do: tribute and killing
rebels raise it; killing enforcers or recruiters, defeating lieutenants and the core lower it. At 0.4 and above
patrols leave you alone; at −0.5 and below you're wanted. See [regime.md](regime.md).

## Items and blocks

| | |
|---|---|
| **Luminessence** | The currency and crafting essence. Mined from Luminance Ore (real world, below Y 20); dropped by lieutenants, the core and cured programs. A **Luminessence Detector** finds ore below a block. |
| **Binary 0 / 1, Byte** | Bits; eight make a Byte. Luminance Ore gives up 0–2 (not with Silk Touch); in the grid, every Static block breaks into one, and corrupted programs drop 1–3. |
| **Pixel Dust, Pixel, Pixel Lamp** | Building materials; Pixels go into lasers. |
| **Gridstone** | The grid's stone, in bricks, cracked, slabs, stairs, walls and patterned, glowing variants in every dye color. |
| **Data Disc** | A thrown weapon: ricochets off up to 3 blocks, hits once per pass, returns to your hand. |
| **Light Staff** | A weapon with reach (4.5 blocks) that blocks attacks from the front like a light shield. |
| **Light Grenade** | Thrown; bursts on impact into an area hit and a ring of light walls that fade after five seconds. |
| **Force Shield** | A shield that also sends projectiles striking its face straight back (discs and grenades too). |
| **Chute** | Worn on the back; glides like an elytra and opens by itself after a 4-block fall. |
| **Overclock** | A drink: a minute of strength and speed, paid for in player decay (+0.25 a dose, fading 0.01 a minute). While overclocked or decayed past 0.5 you read as corrupt: programs and patrols attack, corrupted programs leave you be, and you flicker. |
| **Disinfection Pad** | Cures corrupted programs that step on it. |
| **Lightcycle Baton** | Deploys a rideable light cycle. |
| **Boost Block**, **Light Barrier Emitter** | A speed pad; a redstone-powered wall of light. |
| **Coal Dust** | Fuel. |
| **Stronghold Uplink** | Found in strongholds; a redstone signal into it can be wired as a lieutenant's respawn trigger. |

## Known gaps

- **Placeholder art** for every new mob (programs share one model with generated skins).
- **Derez cubes don't light the world** (they're client-side particles); the Bool, light cycles and the System Core do.
- Light cycles drive jittery and slow, and lay no walls yet; strongholds are small and few. More in [TODO.md](TODO.md).
- Features placed at chunk generation (the garage, the core chamber, the Portal Keeper, the greeter Bool) only
  appear in chunks generated with this version.
