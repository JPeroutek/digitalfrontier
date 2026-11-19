# Stronghold lieutenants

Every grid has strongholds at the seats of the regime's territory, and every stronghold has a **lieutenant**:
a named boss who appears when players come near, and who comes back some time after being derezzed. Lieutenants
are the regime's, not the decay's: they're programs turned to its cause, disciplined rather than glitching. How
many strongholds a grid has depends on its regime setting (the Grid Tuning Laser's "Regime" slider): one up to 64%
(the default grid has 50%), two from 65%, three from 85%. Lieutenants
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
  "texture": "digitalfrontier:textures/entity/warden64.png",
  "health": 220,
  "weight": 10,
  "min_grid_corruption": 0.0,
  "max_grid_corruption": 1.0,
  "min_grid_regime": 0.0,
  "max_grid_regime": 1.0,
  "loot_table": "digitalfrontier:entities/lieutenant",
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
| `texture` | no | A 64×64 player-style skin. Defaults to the archetype's (`sovereign64` for rulers, `warden64` for duelists). The image belongs in a resource pack; players who don't have it see the archetype's skin instead. |
| `health` | no | Defaults to 300 (ruler) or 220 (duelist). |
| `weight` | no (10) | Relative chance when a stronghold picks its lieutenant. |
| `min_grid_corruption` / `max_grid_corruption` | no (0 / 1) | Only grids whose decay setting (the Grid Tuning Laser's "Decay" slider, 0–1) is in this range can have this lieutenant. |
| `min_grid_regime` / `max_grid_regime` | no (0 / 1) | Only grids whose regime setting (the "Regime" slider, 0–1) is in this range can have this lieutenant. |
| `loot_table` | no | What it drops when derezzed (a loot table id). Defaults to `digitalfrontier:entities/lieutenant`. |
| `respawn.min_delay` | no (0) | Ticks after the defeat before anything can bring it back (24000 ticks = 1 day). |
| `respawn.max_respawns` | no (-1) | How many times it can come back; -1 for no limit. |
| `respawn.trigger` | yes | When it comes back; see below. |
| `restore` | no | A trigger that, if it holds at the moment the lieutenant is defeated, **redeems** it instead; see "Redemption" below. Without one it can't be redeemed. |
| `restored_texture` | no | Its skin once redeemed. Defaults to the archetype's redeemed skin (`sovereign_restored64`, `warden_restored64`: the same suit, its circuitry white). |

Each stronghold picks its lieutenant once, when the grid first needs it, by weight and grid seed. Strongholds
pick in order (`stronghold_0` first), each preferring a lieutenant no other stronghold of the grid has; only once
every eligible lieutenant is taken do they repeat.

### Loot

Lieutenants drop their `loot_table`, an ordinary loot table (`data/<namespace>/loot_table/...`) with the
`minecraft:entity` context, so `killed_by_player`, looting and the like all work. The mod ships two:

| Table | Drops |
|---|---|
| `digitalfrontier:entities/lieutenant` (the default, used by the Warden) | an Escape Key; a 25% chance of Memory Paging; 2–5 Luminessence |
| `digitalfrontier:entities/lieutenant/the_sovereign` | the same, but the 25% chance is a Core Dump |

A datapack can point a lieutenant at its own table, or replace either shipped table to change every lieutenant
that uses it. `"loot_table": "minecraft:empty"` drops nothing.

### Lore names

The mod's own names and skins don't come from the films. It also ships a built-in **lore pack**, off by default,
which gives its characters the films' names and the project's character skins. The pack has two halves, and
each is switched on separately:

- **Data**, per world: tick "Digital Frontier: Lore Names" on the Data Packs screen when creating a world, or run
  `/datapack enable "digitalfrontier:lore"` (then defeat and respawn a lieutenant to see the change).
- **Assets**, per player: Options → Resource Packs. This renames the characters and holds their skins. A player
  without it still sees the default skins and names.

The lore pack is generated (`ModLorePackProvider`): its definitions are the shipped ones with only `texture`
changed, and its names override the translation keys.

To use names of your own, put a datapack in the world's `datapacks` folder that replaces the shipped files:

```
my_names/
  pack.mcmeta                 {"pack": {"description": "My names", "min_format": 121, "max_format": 121}}
  data/digitalfrontier/grid/lieutenant/the_warden.json      ... "name": "..." ...
  data/digitalfrontier/grid/lieutenant/the_sovereign.json   ... "name": "..." ...
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
| `core_defeated` | | The grid's System Core has been derezzed (see "The System Core" below). |

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
| `lieutenant_defeated` | `lieutenant`, `others_only` (true) | A lieutenant in this grid is defeated: by default only one at another stronghold (when one falls, another rises); with `lieutenant` set, only that one. |
| `player_derezzed` | `radius` (-1) | A player dies in the grid. |
| `portal_used` | | Someone leaves the grid through its Portal. |
| `program_derezzed` | `radius` (-1) | A player kills a clean program. |
| `program_restored` | `radius` (-1) | A Disinfection Pad cures a corrupted program. |
| `memory_paged` | | A player cheats death with Memory Paging, a Core Dump, or a Data Disc in the offhand. |
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

## Redemption

Lieutenants are the regime's, good programs turned to its cause, and some can be turned back. A lieutenant whose
definition has a `restore` trigger is tested as it takes its fatal blow: if the trigger holds, it isn't derezzed.
Instead it heals, takes its `restored_texture`, and from then on never fights players; it attacks enforcers and
recruiters around its old stronghold, and enforcers attack it. Its stronghold picks a different lieutenant, which
isn't there yet: it arrives by its own respawn trigger, as if just defeated. A redeemed lieutenant is never picked
again in that grid, and to the other strongholds its redemption counts as a `lieutenant_defeated`. The player who
redeemed it loses 0.3 alignment with the regime (see `docs/regime.md`).

The shipped Warden is redeemed if a tamed Bool is within 16 blocks when it falls:
`"restore": {"type": "companion_present"}`.

A `restore` trigger is built from the same triggers as a respawn trigger, plus three about the defeat itself, which
never hold at any other time:

| Type | Fields (defaults) | Holds when |
|---|---|---|
| `companion_present` | `radius` (16) | A tamed Bool is within `radius` blocks of the lieutenant. |
| `defeated_unarmored` | | The player who brought it down wears no armour. |
| `killer_alignment` | `max` | The player who brought it down has an alignment with the regime of at most `max`. |

Events under `restore` count while the lieutenant is alive, from when it last (re)appeared; e.g.
`{"type": "count", "count": 3, "trigger": {"type": "block_broken"}}` redeems it once three blocks were broken near
its stronghold during its reign. `time` counts from its last defeat.

## The System Core

Under the center of every grid's city, below the subway, is a chamber where the regime's heart sleeps. A shaft
leads down to it from the crossing of the central avenues (its mouth is ringed in red light), with a ladder on its
west wall, but an unbreakable **core seal** closes its foot while the regime's lieutenants stand.

When every stronghold has lost its lieutenant (defeated, or redeemed), the core **wakes**: the grid is told, the
seal opens, and a tamed Bool's "yes" points at the core. It stays awake even if lieutenants return. The core
appears when a player comes within 48 blocks of it. It never moves, and fights in three phases:

| Health | Phase | What it does |
|---|---|---|
| over half | absorbing | Calls up a decayed program at the chamber's edge every 5 seconds (up to 6), and draws every program within 24 blocks toward it. Any that reaches it is absorbed and heals it 15. Disinfection Pads in the chamber cure decayed programs on their way in. |
| under half | exposed | Its shell breaks: homing bolts every 3 seconds, and a pair of enforcers every 15. |
| under a fifth | enraged | Bolts every 1.25 seconds, enforcers every 10, and glitch bolts join the mix. |

Each bolt deals 4 damage and does one more thing, shown by its trail:

| Bolt | Trail | On a hit |
|---|---|---|
| Freeze | snowflakes | Held in place for a second, frosting over. |
| Knockback | gusts | Thrown away from the core (less with knockback resistance). |
| Levitate | end rods | Lifted for 3 seconds. |
| Glitch (enraged only) | ink | Darkness for 3 seconds. |

The regime's own enforcers and recruiters shrug the bolts off, so a squad never turns on the core.

It has 600 health and 10 armour. Derezzed, it drops two Core Dumps, a Data Disc and 8-16 Luminessence
(`digitalfrontier:entities/system_core`), and the regime is broken in that grid for good: patrols spawn as if its
control were a tier weaker, no more recruiters come, and it wants half as many members. Players within 48 blocks
lose 0.3 alignment. The `core_defeated` trigger can bring this into lieutenants' respawn rules.

## Commands

| Command | Who | Does |
|---|---|---|
| `/grid lieutenant status <grid>` | everyone | Each stronghold's lieutenant, whether it's alive, and (if defeated) how far each trigger has got. |
| `/grid lieutenant trigger <grid> [signal]` | operators | Fires a `function` event. |
| `/grid lieutenant defeat <grid> <stronghold>` | operators | Marks a lieutenant defeated without a fight, to test triggers. |
| `/grid lieutenant respawn <grid> <stronghold>` | operators | Brings a defeated lieutenant back now. |
| `/grid core status <grid>` | everyone | The System Core's state: `dormant`, `awake` or `defeated`, and where its shaft is. |
| `/grid core reset <grid>` | operators | Puts the core back to sleep: it goes, its seal closes, and every fallen lieutenant returns. |

Grids are named by id, e.g. `digitalfrontier:default`; strongholds by site id, e.g. `stronghold_0`.
