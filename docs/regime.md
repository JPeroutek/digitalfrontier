# The regime

Every grid has two forces at work: **decay**, the digital corruption of a neglected grid, and **the regime**, an
orderly power that rules from its strongholds. Decay is a property of places: it glitches the terrain and spawns
corrupted programs (see `docs/grid-structures.md`). The regime is a society: it holds territory, and it grows by
recruiting the grid's programs. This page covers the society.

## Territory

Each stronghold controls the ground around it; see "Decay and the regime" in `docs/grid-structures.md` for the
control field, its tiers and how it holds decay back. Enforcers patrol controlled ground; a defeated lieutenant's
ground is patrolled at half strength until it returns.

## Programs' sentiment

Every program has a **sentiment** toward the regime, from -1 to 1, saved on the entity as `Sentiment`. A new
program's sentiment is drawn from a bell curve: around 0 in a free city, leaning loyal where the regime holds the
ground (up to +0.3 under full control) and under a strong regime. Most programs are neutral; some are loyal, some
rebels.

| Sentiment | Stance | Behaviour |
|---|---|---|
| 0.5 and up | loyal | Drifts toward enforcers and recruiters. Reports players the regime counts as rebels (alignment under -0.3) to enforcers within 32 blocks. |
| -0.5 to 0.5 | neutral | Goes about its business. |
| -0.5 and down | rebel | Avoids enforcers and recruiters. Enforcers hunt it. |
| -0.8 and down | resistance | Attacks recruiters, and enforcers that are attacking a player or a program. |

What moves it:

| Event | Change |
|---|---|
| Each second within 4 blocks of a recruiter | +0.01; a program at -0.6 or below hardens instead (-0.01) |
| Refusing a bribe | -0.05 |
| Seeing an enforcer kill a program, within 16 blocks | -0.15 |
| An enforcer killing a corrupted program that was attacking it | +0.1 |
| A player's gift of Luminessence (right-click) | 0.1 toward the player's side |
| A player killing something that was attacking it | 0.15 toward the player's side |
| A player hurting it | 0.2 away from the player's side |

A player's effect scales with how firmly they've picked a side (their alignment, below), but is never less than 0.02.
Players are outsiders: until a player's alignment is positive, "their side" counts as against the regime.

## Players' alignment

Each player has an **alignment** with each grid's regime, from -1 (its enemy) to 1 (its ally), starting at 0.

| Act | Change |
|---|---|
| Paying a recruiter tribute (right-click with Luminessence) | +0.05 |
| Killing a rebel program | +0.05 |
| Killing a loyal program | -0.02 |
| Killing an enforcer | -0.03 |
| Killing a recruiter | -0.08 |
| Defeating a lieutenant | -0.25 |
| Redeeming a lieutenant (see `docs/lieutenants.md`) | -0.3 |
| Being within 48 blocks when the System Core is derezzed | -0.3 |

What it does:

- **Under 0.4:** enforcers attack on sight (so everyone, at first).
- **0.4 and up:** enforcers leave the player alone unless attacked. Lieutenants stay hostile to everyone.
- **Under -0.3:** loyal programs report the player to enforcers.
- **Under -0.5:** wanted. Enforcers hunt the player from 64 blocks, twice their usual range, seen or not.
- **Trading:** job programs (the Mechanic, the Portal Keeper) price their offers by how far the player's alignment
  is from their own sentiment: a quarter cheaper for a player exactly as aligned as they are, up to a quarter dearer
  for one on the opposite extreme.

## Recruiting

**Recruiters** are the regime's recruitment agents: unarmed, persistent, and found with patrols on its ground while
the regime has fewer members than it wants (at most one per 64 blocks). A recruiter goes to the nearest program
that isn't a rebel and stays with it.

- **Company:** a program within 4 blocks of a recruiter builds up exposure (saved as `Exposure`, in ticks), which
  wears off at half the rate away from recruiters. Every 100 ticks of exposure is a recruitment attempt.
- **Bribes:** every 30 seconds a recruiter offers a bribe to the nearest program within 8 blocks it isn't already
  working on.

The chance an attempt succeeds is the method's base chance (0.15 for company, 0.35 for a bribe), times
`((sentiment + 1) / 2)²` (a loyalist at 1 is certain to be willing, a rebel at -0.9 almost never), times how badly
the regime needs members: `ideal / (ideal + 2 × max(0, members − ideal) + members / 4)`. That's 1 with no members,
0.8 at the ideal, and falls off fast past it without reaching 0.

A program that joins becomes an **enforcer** for good: a conscript, which doesn't despawn.

**Members and the ideal.** The regime's lasting members are its conscripts and recruiters; patrols that come and go
don't count. It wants 8 per standing stronghold at a 50% regime, scaled by `0.5 + regime` (so 12 at 100%). With every
stronghold fallen it wants none, and recruits nobody.

## Tuning the regime

A datapack can change any of these numbers with `data/digitalfrontier/grid/regime.json`. Every field is optional:

```json
{
  "recruiting": {
    "exposure_chance": 0.15, "bribe_chance": 0.35, "exposure_ticks": 100, "bribe_interval": 600,
    "exposure_radius": 4, "bribe_radius": 8, "ideal_per_stronghold": 8
  },
  "sentiment": {
    "exposure_per_second": 0.01, "brutality": -0.15, "protection": 0.1, "bribe_refused": -0.05,
    "gift": 0.1, "defended": 0.15, "hurt": 0.2
  },
  "alignment": {
    "tribute": 0.05, "kill_rebel": 0.05, "kill_enforcer": -0.03, "kill_recruiter": -0.08,
    "defeat_lieutenant": -0.25, "kill_loyal": -0.02, "redeem_lieutenant": -0.3, "defeat_core": -0.3,
    "tolerated_from": 0.4, "reported_below": -0.3, "wanted_below": -0.5
  }
}
```

## Commands

| Command | Who | Does |
|---|---|---|
| `/grid regime status <grid> [player]` | everyone | The regime's members against its ideal, and a player's alignment (yours without one). |
| `/grid regime alignment <grid> <player> <value>` | operators | Sets a player's alignment, -1 to 1. |

A program's sentiment can be read and set with `/data`, e.g.
`/data merge entity @e[type=digitalfrontier:program,limit=1,sort=nearest] {Sentiment:0.9f}`. The F3 grid section
shows your own alignment.
