# Stronghold lieutenants

Every grid has a stronghold deep in its most corrupted territory, and every stronghold has a **lieutenant**: a
named boss who appears when players come near, and who comes back some time after being derezzed. Lieutenants
are data: a datapack can rename them (for example to lore-accurate TRON names), reskin them, change when they
return, or add new ones.

## Defining a lieutenant

Each lieutenant is a file at `data/<namespace>/grid/lieutenant/<name>.json`. The mod ships two:

| File | Name | Archetype | Comes back |
|---|---|---|---|
| `digitalfrontier:the_sovereign` | The Sovereign | `ruler` | after a week, or sooner if the grid feeds the regime |
| `digitalfrontier:the_warden` | The Warden | `duelist` | when its stronghold is deserted and then revisited, or for new players |

```json
{
  "name": { "translate": "lieutenant.digitalfrontier.the_warden" },
  "archetype": "duelist",
  "texture": "digitalfrontier:textures/entity/rinzler64.png",
  "health": 220,
  "weight": 10,
  "min_grid_corruption": 0.0,
  "max_grid_corruption": 1.0,
  "respawn": {
    "min_delay": 12000,
    "max_respawns": -1,
    "trigger": { "type": "stronghold_entered", "after_absence": 24000 }
  }
}
```

| Field | Required | Meaning |
|---|---|---|
| `name` | yes | A text component: `"Rinzler"`, or `{"translate": "..."}` so a resource pack can rename it. |
| `archetype` | yes | How it fights (built in): `ruler` keeps its distance and calls in enforcers; `duelist` is fast and closes distance in sudden dashes. |
| `texture` | no | A 64×64 player-style skin. Defaults to the archetype's (`clu64` for rulers, `rinzler64` for duelists). |
| `health` | no | Defaults to 300 (ruler) or 220 (duelist). |
| `weight` | no (10) | Relative chance when a stronghold picks its lieutenant. |
| `min_grid_corruption` / `max_grid_corruption` | no (0 / 1) | Only grids whose corruption setting (the Grid Tuning Laser's slider, 0–1) is in this range can have this lieutenant. |
| `respawn.min_delay` | no (0) | Ticks after the defeat before anything can bring it back (24000 ticks = 1 day). |
| `respawn.max_respawns` | no (-1) | How many times it can come back; -1 for no limit. |
| `respawn.trigger` | yes | When it comes back; see below. |

Each stronghold picks its lieutenant once, when the grid first needs it, by weight and grid seed. Defeated
lieutenants drop an Escape Key.

### Lore names

To use the films' names, put a datapack in the world's `datapacks` folder that replaces the shipped files:

```
lore_names/
  pack.mcmeta                 {"pack": {"pack_format": 61, "description": "Lore names"}}
  data/digitalfrontier/grid/lieutenant/the_warden.json      ... "name": "Rinzler" ...
  data/digitalfrontier/grid/lieutenant/the_sovereign.json   ... "name": "Clu" ...
```

The replacement must be a complete definition (copy the shipped one and change what you like). A resource pack
can instead override just the names through the `lieutenant.digitalfrontier.the_warden` and
`lieutenant.digitalfrontier.the_sovereign` translation keys.

## Respawn triggers

A trigger is an object with a `type`. **State** triggers look at the world now; **event** triggers count things
that happened since the lieutenant's defeat; **combinators** combine others. The lieutenant returns as soon as
its trigger is satisfied (and `min_delay` has passed). Event counts reset at every defeat.

### States

| Type | Fields | Satisfied when |
|---|---|---|
| `time` | `ticks` | That much game time has passed since the defeat. |
| `overworld_moon_phase` | `phases`: list of 0–7 (0 full, 4 new) | The real world's moon is in one of these phases. |
| `overworld_thunder` | | It's thundering in the real world (a power surge). |
| `scoreboard` | `objective`, `holder`, `min` | That score is at least `min`. `holder` is a player name or a fake one like `#regime`. |
| `all_lieutenants_defeated` | | Every stronghold in the grid has a defeated lieutenant. |

### Events

Fields named `item` or `block` take an id (`"minecraft:echo_shard"`) or a tag (`"#minecraft:logs"`); leaving
them out matches anything. `radius` is in blocks around the stronghold; -1 means anywhere in the grid.

| Type | Fields (defaults) | Counts each time |
|---|---|---|
| `grid_entered` | `new_players_only` (false) | A player arrives in the grid (optionally only players who'd never been there). |
| `stronghold_entered` | `after_absence` (0 ticks) | A player comes within 48 blocks after nobody had been near for at least that long. |
| `item_crafted` | `item` | Someone crafts a matching item. Crafting in the real world counts for every grid. |
| `item_used_at_stronghold` | `item`, `radius` (16), `consume` (true) | A player uses (right-clicks) a matching item near the stronghold, like an offering. |
| `block_placed` | `block`, `radius` (32) | A player places a matching block. |
| `block_broken` | `block`, `radius` (32) | A player breaks a matching block. |
| `redstone` | `radius` (32) | A **Stronghold Uplink** receives a redstone signal. Every starter keep has one inside. |
| `lieutenant_defeated` | `lieutenant`, `others_only` (true) | A lieutenant in this grid is defeated (by default, only another one: when one falls, another rises). |
| `player_derezzed` | `radius` (-1) | A player dies in the grid. |
| `portal_used` | | Someone leaves the grid through its Portal. |
| `program_derezzed` | `radius` (-1) | A player kills a clean program. |
| `memory_paged` | | A player cheats death with Memory Paging or a Core Dump. |
| `function` | `signal` | `/grid lieutenant trigger <grid> [signal]` runs, e.g. from a datapack function. With `signal` set, only that signal counts. |

### Combinators

| Type | Fields | Satisfied when |
|---|---|---|
| `any_of` | `triggers` | Any of them is. |
| `all_of` | `triggers` | All of them are. |
| `not` | `trigger` | It isn't. |
| `count` | `count`, `trigger` (one event trigger) | The event has happened at least `count` times. |
| `chance` | `chance` (0–1), `trigger` | Each matching event only counts with this probability. A state inside is rolled once a second while it holds, and stays satisfied once a roll succeeds. |

Example: back after three days, or right away if three players die in the grid, or on a full moon with a 10%
chance per second:

```json
"trigger": {
  "type": "any_of",
  "triggers": [
    { "type": "time", "ticks": 72000 },
    { "type": "count", "count": 3, "trigger": { "type": "player_derezzed" } },
    { "type": "chance", "chance": 0.1, "trigger": { "type": "overworld_moon_phase", "phases": [0] } }
  ]
}
```

## Commands

| Command | Who | Does |
|---|---|---|
| `/grid lieutenant status <grid>` | everyone | Each stronghold's lieutenant, whether it's alive, and (if defeated) how far each trigger has got. |
| `/grid lieutenant trigger <grid> [signal]` | operators | Fires a `function` event. |
| `/grid lieutenant defeat <grid> <stronghold>` | operators | Marks a lieutenant defeated without a fight, to test triggers. |
| `/grid lieutenant respawn <grid> <stronghold>` | operators | Brings a defeated lieutenant back now. |

Grids are named by id, e.g. `digitalfrontier:default`; strongholds by site id, e.g. `stronghold_0`.
