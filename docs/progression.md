# Progression: what you do in the grid

Once you've arrived (see [getting-started.md](getting-started.md)), the grid has a loose arc: explore and equip,
pick a side in the regime's world, break its strongholds, and finally face the System Core. None of it is forced;
the grid is also simply a place to explore and build.

## Stage 1: explore and equip

**Goal:** a companion, a weapon, a ride and a way home.

- **Tame the Bool** by the City Terminal (Luminessence). Keep it with you all game: it warns you of threats and
  points the way to strongholds, and it matters for redemption later.
- **Craft a Data Disc** (four Luminessence around a Byte). Throw it: it ricochets off walls up to three times and
  comes back. It's also insurance: in your offhand it counts as a Core Dump if you die.
- **Buy a Lightcycle Baton** from the Mechanic in the City Core's garage, and ride out. The subway's express lines
  also reach every Waypoint.
- **Discover terminals.** Pass within two chunks of each Waypoint to discover it; your laser can then send you
  straight there. Find the **Portal** at the far edge, and its Keeper, who sells Escape Keys.
- **Craft Disinfection Pads** (three Luminessence over three Gridstone, makes two). You'll want them in stages 3
  and 4.

**Earning Luminessence in the grid:** curing corrupted programs on Disinfection Pads (1–2 each), defeating
lieutenants and the core. The real world's ore is the steady supply.

## Stage 2: read the land, and pick a side

**Goal:** understand the two forces, and decide where you stand.

- **Decay** grows with distance from the city. You'll see it build up: flickering lines, cracks, then holes and
  static, until buildings break apart. Corrupted programs stalk decayed ground (they freeze while you watch them).
  Programs near decay slowly turn; lead corrupted ones over a Disinfection Pad to cure them.
- **The regime** holds the land around its strongholds. The ground gets *cleaner* and its pattern lines turn red.
  Your Bool goes red as patrols appear. **Enforcers** attack anyone the regime doesn't tolerate, and that's
  everyone at first. **Recruiters** walk among the programs, conscripting the willing into enforcers.

Your **alignment** decides how the regime treats you:

| You... | Alignment | Effect |
|---|---|---|
| Pay a recruiter tribute (Luminessence) | +0.05 | At **0.4** and above, patrols leave you alone. |
| Kill a rebel program | +0.05 | |
| Kill an enforcer / a recruiter | −0.03 / −0.08 | At **−0.3** and below, loyal programs report you to patrols. |
| Defeat / redeem a lieutenant | −0.25 / −0.3 | At **−0.5** and below you're **wanted**: patrols hunt you from twice as far. |

**Paths:**
- **Collaborator.** Pay tribute until patrols ignore you. Safe passage, but the Portal Keeper and rebel programs
  charge you more, and breaking the strongholds later costs you that standing.
- **Rebel.** Fight. Give rebel programs Luminessence and defend them: they'll flee patrols, and the resistance
  will fight beside you. Loyalists will turn you in, so the regime's land gets dangerous fast.

Prices from the Mechanic and the Portal Keeper move up to 25% either way with how far your alignment is from
theirs.

## Stage 3: the strongholds

**Goal:** defeat (or redeem) every lieutenant.

- **Find them.** Face around until your Bool shows its yellow diamond, then follow it. The regime's land thickens
  and reddens as you close in; the stronghold itself is a building at the heart of it.
- **Fight the lieutenant.** A boss with a health bar. Lieutenants drop an Escape Key, Luminessence, and sometimes
  Memory Paging (or a Core Dump from the Sovereign).
- **Redeem the Warden.** Defeat the Warden with your tamed Bool within 16 blocks and, instead of dying, it's
  **redeemed**: it heals, turns white, and fights the regime's enforcers beside you. Its stronghold gets a new,
  hostile lieutenant, so you're not done there.
- **Lieutenants come back.** Each has its own respawn condition (time, or something players do). A defeated
  lieutenant halves the patrols around its stronghold until it returns.

## Stage 4: the System Core

**Goal:** break the regime.

- **The awakening.** When no stronghold has a living lieutenant, a broadcast announces that the **System Core**
  stirs beneath the city center, and the seal at the bottom of the **shaft** opens. The shaft is in the middle of
  the central intersection, red-rimmed, with a ladder. Your Bool points there once the core is awake.
- **Phase 1 (above half health): absorbing.** The core calls decayed programs in from the chamber's edges and draws
  every program toward it; any that reach it are absorbed and heal it. **Place Disinfection Pads** in their path to
  cure them on the way in, and keep up the damage.
- **Phase 2 (below half): exposed.** Homing bolts every few seconds, each with its own trail: **snowflakes** freeze
  you in place for a second, **gusts** throw you back, **end rods** make you float. Pairs of enforcers arrive.
- **Phase 3 (below a fifth): enraged.** Faster bolts and squads, and **ink** bolts that black out your view.
- **Victory.** Two Core Dumps, a Data Disc and Luminessence. The regime in that grid is broken for good: patrols
  thin out, recruiters stop, and its numbers fall. Defeating it costs 0.3 alignment for everyone who fought.

Iron-level gear, a Data Disc and a stack of pads is meant to be enough. Bring Memory Paging or a Core Dump.

## Afterwards

- **Tune your own grids.** The **Grid Tuning Laser** (a Digitizing Laser with diamonds, netherite, a Pixel and a
  Nether Star) opens a screen to generate a new grid: seed, city size, street style, levels, district colors,
  waypoints, and the **Decay** and **Regime** sliders. A high regime means more strongholds and wider control; high
  decay means a more broken world. Share a grid as its `DF4.` code.
- **Build.** Gridstone, Pixels, Pixel Lamps, Boost Blocks and Light Barriers are building blocks; the grid's
  buildings themselves are data you can extend (see [grid-structures.md](grid-structures.md)).
