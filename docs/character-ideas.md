# Character-inspired additions

Sketches for new content, drawn from the roles in `docs/characters.md`. None of this is scheduled; anything
that gets picked up moves into `docs/TODO.md`, or a design doc of its own.

**Ground rules:**
- **Archetypes, not characters.** Each idea takes a *role* from the films and shows (the masked champion, the
  club owner who sells you out, the guardian of the last free tower). It gets its own name, look and mechanics.
  The films' names stay a datapack or resource pack choice, as the lieutenants already do (`docs/lieutenants.md`).
- **Two packs.** The mod's built-in data is the agnostic default. The mod also ships a built-in **lore pack**
  (`digitalfrontier:lore`, off by default; see `docs/lieutenants.md`) that switches to the canonical names. The
  working names below (Identity Disc, Recognizer, ...) are placeholders until each feature gets its agnostic
  default name. The lore pack is the canonical mapping: names, text in our own words, and each character's own
  art (e.g. a `clu64.png` skin for the lieutenant it names Clu), from the project's artists.
- **Data-driven for the swap.** For a pack to rename or re-theme anything, it has to be data or translation keys,
  never a string in code. Every named character needs a `texture` field, as lieutenants have, so its art can
  change with its name.
- **Reuse what's there:**
  - the corruption setting and strongholds;
  - lieutenant archetypes and respawn triggers;
  - the city's levels and waypoints;
  - the Program model and skins;
  - Escape Keys, Memory Paging and Core Dumps;
  - light cycles, light barriers and the Portal.

Each sketch lists **inspired by**, **what it adds**, and **builds on**.

---

## 1. The regime

### 1.1 New lieutenant archetypes
`ruler` (The Sovereign) and `duelist` (The Warden) exist. Each new one is a built-in archetype that datapack
lieutenants can name.

| Archetype | Inspired by | How it fights |
|---|---|---|
| `commander` | Sark, General Tesler | Rides a slow **command carrier**, a flying platform entity above the stronghold. It drops enforcer squads and bombards from range. Bring it down by destroying the carrier's emitters, then fight it on the deck. |
| `scientist` | Keller, Shaw, Tacitus | Stays behind enforcers. It **repurposes** nearby programs into corrupted ones (see 1.3), and buffs allies with code vials that also corrupt them. Its stronghold has a lab, with captive programs in cells to free. |
| `officer` | Jarvis, Sark's Lieutenant, Pavel | Weak in a fight. It runs for the stronghold's uplink and, if it gets there, lowers the other lieutenants' respawn delays. Drops intel: a map of the grid's other strongholds. |
| `security` | Kernel, Dyson, the ICPs | Hates Users. It takes less damage from players than from programs, so it's best fought alongside freed programs. Carries a force shield that must be broken from behind. |

### 1.2 Redemption, not just defeat
**Inspired by:** Rinzler, who is Tron repurposed and turns back once he recognizes a User. Also Paige and Cutler.

**What it adds:** some lieutenants can be **redeemed** instead of derezzed. A lieutenant is a good program turned to
the regime's cause, not decayed code: redemption turns it back. Defeating The Warden while a condition
holds turns its circuitry from orange to white. It kneels, then becomes a friendly champion who guards the area,
or follows the player for a while. A restored lieutenant doesn't respawn as hostile; the stronghold picks a
replacement.

Possible conditions:
- the player is unarmoured;
- a Bit companion is present (2.1);
- the stronghold's uplink was destroyed first;
- it's fought at a full overworld moon.

**Builds on:** this fits the trigger system. Add a `restore` block to `LieutenantDef` holding a `Trigger`,
evaluated at the moment of defeat.

**Built:** see "Redemption" in `docs/lieutenants.md`. The Warden is redeemed with a tamed Bool nearby; the lore
pack gives the redeemed Warden the Tron skin. Restore triggers can also use `defeated_unarmored`, `killer_alignment`,
and any event counted during the lieutenant's reign.

### 1.3 Repurposing
**Inspired by:** Clu's repurposing of programs, Keller's code pathogen, and the circuitry colours.

**What it adds:** decay becomes something that happens *to* programs. Decay and the regime are separate axes
(see `docs/grid-structures.md`): decay is the only one that turns programs by area. The regime recruits instead,
through recruiters and bribery, by each program's sentiment toward it (see `docs/regime.md`).
- Neutral (white or blue) Programs near corrupted ones, or in decayed districts, slowly turn.
- Their circuitry shifts through yellow to orange, a visible stage, before they become corrupted programs.
- A city bordering decay therefore slowly loses its civilians unless the player intervenes.
- The reverse is 1.4.

**Builds on:** `ProgramEntity` and `CorruptedProgramEntity`, the decay field, and skin tinting.

### 1.4 Disinfection
**Inspired by:** the Infected Files in Killer App, which turn back into normal programs on disinfection pads and
thank the player.

**What it adds:**
- **The block:** a **Disinfection Pad**. A corrupted program pushed, led or knocked onto it reverts to a
  friendly program.
- **The reward:** the restored program drops a small reward, or gives a hint (where a stronghold is, or the grid
  code of somewhere interesting).
- **Grid-wide effect:** restoring enough programs could count as a respawn trigger *against* the regime, so a
  stronghold's lieutenant returns more slowly.

**Builds on:** a new `Triggers` record, `programs_restored`, the inverse of the existing event triggers.

**Built,** with 1.3:
- **Drifting:** each program has a decay, 0 to 1, saved as `Decay`. Every 2 seconds the ground it stands on (above
  0.3 decay) and corrupted programs within 8 blocks wear it down; clean ground lets it recover. Programs show it from
  0.3 (they jitter) and fray orange from 0.6; at 1 they become corrupted programs. On corrupted ground that takes
  under two minutes; on unstable ground, most of a day. The `digitalfrontier:program_decay` game rule (default
  true) turns it off.
- **Disinfection Pad:** two from three Luminessence over three gridstone. A corrupted program that steps on one
  becomes a clean program again, with no decay and a fresh sentiment, drops 1-2 Luminessence, and fires the
  `program_restored` event trigger (see `docs/lieutenants.md`). Nothing else on the pad is affected.
- **Not yet:** hints from restored programs.

### 1.5 The final boss
`docs/TODO.md` already plans one around `all_lieutenants_defeated`. There are two directions to choose between:

- **System core** (from the MCP): a huge, stationary rotating column in a chamber under the grid's center.
  - It **absorbs** programs and corrupted programs within reach to heal and grow, so the fight is partly about
    keeping programs away from it.
  - Its phases tear away its outer shell.
  - Defeating it lowers the grid's corruption permanently, as a visible change in the world.
- **The usurper** (from Clu 2): the regime's ruler, fought on the deck of a giant command ship that arrives
  over the city once every stronghold has fallen.
  - Earlier lieutenants return as phases: a duelist, then a commander.
  - The ship's shadow and the Voice of the Grid's announcements build up the event.

**Built:** the **System Core** (the lore pack calls it the Master Control Program); see "The System Core" in
`docs/lieutenants.md`. Defeating it weakens the regime (its patrols and recruiting) rather than lowering decay, since
decay and the regime are separate. **Not yet:** a trophy drop, and the usurper.

The system core is the more distinct of the two: The Sovereign already covers the "ruler in person" fight.

### 1.6 Squads and enforcer variants
**Built:** squads, leaders and the staff, heavy and grenadier roles (`docs/regime.md`). The chute variant comes with the
Recognizer.

**Inspired by:** Black Guards in squads of four, staff-wielding Sentries, and heavy Sentries.

**What it adds:** enforcers spawn as **squads** with a leader. Killing the leader scatters the rest. The variants
are:
- **staff**: reach, and blocks with its staff;
- **heavy**: slow and armoured;
- **grenadier**: light grenades, which leave short-lived light barrier fragments;
- **chute**: drops in from skyroads and Recognizers.

### 1.7 Recognizer patrols and conscription
**Built** as a piloted vehicle (`docs/regime.md`): patrols, searchlight, chute drops, and stealing one. Conscription
to the arena comes with the games.

**Inspired by:** Recognizers carrying captured programs and Sam to the games.

**What it adds:** a **Recognizer**, a large flying arch-shaped patrol entity, sweeps over corrupted districts.
- **Its searchlight:** a player caught in it is spotted. Enforcers drop from the Recognizer, and at high
  corruption the player can be *conscripted*.
- **Conscription:** the player is lifted, as a passenger, to the grid's arena (2.4) and must win a round to leave.
  Winning gives loot; losing costs only the round.
- **Pacing:** a teleport-style punishment with an upside; keep it rare.

---

## 2. Free programs, ISOs and the city

### 2.1 The Bit companion
**Inspired by:** Bit, which can only say yes or no, and Byte, its talkative upgrade.

**What it adds:** a small floating polyhedron pet, befriended with Luminessence. It answers the world in binary:
- **Yes:** it turns yellow-white and chimes when you face toward something the player is tracking (the nearest
  stronghold, Luminance ore, or an exit terminal).
- **No:** it turns red and spiky when there's danger: corrupted programs nearby, or a Recognizer overhead.
- **Upgrade:** a Byte-like form unlocks chat hints from a data-driven line pool, with a dry sense of humour.

**Builds on:** the Luminance Ore Detector's "direction to a thing" logic.

**Built** as the **Bool** (the lore pack calls it Bit):
- **Finding one:** one waits on the avenue outside the main arrival terminal, and wild ones appear rarely (3% of
  spawns, never two within 64 blocks) on the clean streets of the free city.
- **Befriending:** right-click with Luminessence, a 1-in-3 chance per piece. Right-click with an empty hand to make
  it stay or follow.
- **"No":** red, spiky, a low buzz, while a corrupted program, enforcer, recruiter or lieutenant is within 16
  blocks of its owner.
- **"Yes":** a bright diamond and a chime while its owner faces (within 15°) the nearest stronghold whose lieutenant
  still stands.
- **Staying alive:** while its owner is fighting it hangs back above them. Only players other than its owner can
  hurt it, so it survives a lieutenant fight.
- **Not yet:** other things to track (ore, exit terminals), and the Byte upgrade.

### 2.2 ISOs and the Sea of Simulation
**Inspired by:** ISOs, which arise on their own from the Sea of Simulation; Quorra, Radia, Gibson and the Bostrum
Colony; Clu's Purge.

**What it adds:**
- **ISO programs:** a rare kind of program that **emerges from the grid's sea** at night. Their circuitry is
  drawn with a distinct glyph on the arm, and they speak in riddles.
- **Hidden ISO colony:** a waypoint kind, an isolated settlement far from the city.
  - It holds an ISO trader, for unique items such as a light sword, or seeds of "generative" blocks.
  - The regime hunts it: corrupted programs and patrols seek ISOs.
  - Protecting the colony is a long-running goal. If the colony falls, the grid announces a Purge.

**Builds on:** the sea and waypoints, and the Program model.

### 2.3 Named roles in the city
These are NPC programs with jobs, each tied to a POI the city generator can place:

| Role | Inspired by | POI | Offers |
|---|---|---|---|
| **Tower Guardian** | Dumont, I-No | the exit terminal or Portal | Explains the way out; for a price, **sends a message to the overworld** (a sign or book at the player's bed or spawn). In a heavily corrupted grid, the regime has taken the tower, and freeing it is a quest. |
| **Club owner** | Castor/Zuse, Weaver | a nightclub at the top of the tallest tower | An information broker: buys Luminessence, sells stronghold locations and rare items. Has a chance to **betray** the player, calling a squad to the club. The club has music and dancing programs. |
| **Garage chief and mechanics** | Able, Mara, Zed | a garage in an industrial block | **Light cycle tuning**: speed, grip, wall colour, and a "deploy while sprinting" upgrade (both are in TODO). Repairs and re-packs cycles into their batons (also in TODO). |
| **Outfitter** | Gem, the Sirens | under the arena | Armour fitting: a circuitry-coloured suit set, and dyes for circuitry. |
| **Disc editor** | Gorn, Lomox, Kobol | a black-market back room | Edits an Identity Disc (3.1) with "code worms", i.e. enchantment-like modifiers, at a risk of corrupting it. |
| **Racer** | Ter, Mercury | a rooftop black market or racetrack | Light cycle races for wagers. |
| **Healer** | Moto, Tron's healing chamber | a clinic in rebel territory | Heals, and cures the player's corruption status, if one exists. |
| **Down-and-out programs** | the Destitute Program, the Monks | streets and alleys | Ambient life: programs with no function. Some are dormant ISOs, or players' lost companions. |

**Built:** the **Mechanic** (a garage in the city core, plus the odd one elsewhere) and the **Portal Keeper** (the lore
pack calls it the Tower Guardian), as job programs that stay at their post and trade through the vanilla trading
screen. The Mechanic sells booster blocks, light barrier emitters, coal dust and Light Cycle Batons; the Portal Keeper
explains the Portal and sells one Escape Key a day. Prices are in Luminessence, a quarter cheaper for players as
aligned with the regime as the program is, and a quarter dearer for those on the opposite extreme (see
`docs/regime.md`). Offers are data (`data/<namespace>/grid/job/<job>.json`) and restock daily. Job programs
never decay and are never recruited. **Not yet:** light cycle tuning and re-packing, and the Keeper's message to the
overworld.

### 2.4 The games
**Built:** the arena, its console and Announcer, champions with levels and titles, and the disc duel
(`docs/features.md`); the other games follow.

**Inspired by:** the Disc Arena, the Light Cycle Grid, the Ring Game (Hyperball), tank matches, and the Voice of
the Grid announcing it all.

**What it adds:** an **arena** waypoint (TODO already lists "a light-cycle arena"), with rounds against
programs:
- **Disc duel:** on floating platforms that fall away, with discs (3.1).
- **Light cycle match:** light barriers are already in the game; arena cycles lay walls automatically.
- **Ring game:** a variant played with cestas.

Champion bosses (inspired by Blaze, Bosh and Baxter) defend a title. Beating them gives a trophy block, and an
announcer line broadcast grid-wide. Players can also enter voluntarily, and conscription (1.7) drops them here.

### 2.5 Rebel faction and reputation
**Inspired by:** Beck the Renegade, Bartik's cell, Mara's graffiti crew, Reset.

**What it adds:** a per-grid **reputation** with the free programs.
- **Raising it:** freeing captives, restoring programs, defeating lieutenants.
- **What it unlocks:**
  - rebel safehouses, as waypoint variants;
  - better trades;
  - programs who fight beside the player when a squad shows up;
  - rebel graffiti in the city (Pixel-block murals; a **graffiti glove** item that paints Pixel and Pattern
    blocks, inspired by Rasket).

---

## 3. Equipment and vehicles

### 3.1 The Identity Disc
**Inspired by:** every program's disc: a throwing weapon that returns, which also stores the program's memory.

**What it adds:**
- **Weapon:** a throwable that ricochets off walls and returns to the thrower.
- **Records:** the disc **records** its holder. A disc taken from a defeated lieutenant can be "read" at a
  terminal, revealing stronghold intel or the lieutenant's lore text.
- **Memory backup:** a player's own disc could act as a Core Dump-style backup, keeping the inventory once when
  it's in the offhand at death and breaking afterwards. That would tie into `MemoryPaging`.
- **Dual discs:** the Warden's signature, as a rare drop that fires two discs.

**Built** as the **Data Disc** (the lore pack calls it the Identity Disc). Crafted from four Luminessence around a
Byte; 250 uses, repaired with Luminessence.
- **Throwing:** it leaves the hand, flies straight, ricochets off up to 3 surfaces (keeping 90% of its speed each
  time), and turns back after about a second and a quarter, or after its last bounce. On the way back it passes
  through walls. It deals 7 damage to each creature it passes through, once per leg, and loses 1 durability per hit.
- **Catching:** it returns to the main hand if that's empty, otherwise the inventory. If its thrower died, left,
  or changed dimension, it drops where it is.
- **Memory backup:** dying in a grid with no Core Dump but a Data Disc in the offhand, it counts as a Core Dump:
  you keep your inventory, and the disc is spent. A Core Dump, if you carry one, is used first.
- **Not yet:** records and dual discs.

### 3.2 Other gear
**Built** (except the code vial): the Light Staff, Light Grenade, Force Shield, Chute and Overclock; see
`docs/features.md`.

- **Light staff:** reach, and a sweeping block. Inspired by the Sentries, Mercury and Ares.
- **Light grenade:** an area hit that leaves short-lived light barriers.
- **Chute:** a glider that deploys from a skyroad or Recognizer fall; it could reuse elytra logic.
- **Force shield:** a shield that reflects discs.
- **Power upgrade:** inspired by Shaw. A large strength buff that adds corruption to the player, with a visible
  circuitry shift and side effects: hostile to programs, friendly to corrupted ones.
- **Code vial:** inspired by Keller. Thrown, it repurposes or restores a program depending on the vial.

### 3.3 Vehicles
| Vehicle | Inspired by | Role |
|---|---|---|
| **Light jet** | Legacy, Uprising | Flight, laying a light ribbon behind it; a late-game reward. |
| **Recognizer** | all eras | Hostile patrol (1.7); ridable once the player is restored or a rebel. |
| **Light tank** | Clu (1982), Evolution | Slow and armoured, firing bouncing shells; arena tank matches. |
| **Light runner** | Quorra | A two-seat ground car, so a companion can ride along. |
| **Light boat** | Uprising | Sea travel, and access to the ISO colony. |
| **Solar Sailer** | Yori | **A scheduled transit line**: a sail that glides along a fixed light beam between distant waypoints, out over the sea. It fits beside the express subway, and `GridTransit`'s lines. |
| **Command carrier or command ship** | Sark, Tesler, Clu | The boss platform for the commander lieutenant and the usurper boss (1.1, 1.5). |

---

## 4. Users, the overworld and the Portal

### 4.1 Programs that follow you out
**Inspired by:** Ares, who can survive only 29 minutes in the real world, and the Permanence Code that ends the
limit.

**What it adds:** a companion program (restored lieutenant, Bit, rebel) can come through the Portal with the
player.
- In the overworld it **derezzes after a timer**, returning to its grid, and flickers more as the timer runs low.
- A rare **Permanence Code** item, a final-boss or deep-stronghold drop, lifts the limit for one program.
- A visible, tangible link between the two worlds that makes the grid feel like it matters outside.

### 4.2 Users are different
**Inspired by:** programs revering or fearing Users; Rinzler recognizing Sam by his blood; the Kernel's
contempt.

**What it adds:**
- Program NPCs react to players as **Users**: ordinary programs bow or stare, ISOs are curious, security
  programs are hostile.
- A player who takes damage has a chance to show it, as red particles rather than derez cubes. Nearby programs
  notice, which feeds the Warden's redemption condition (1.2).

### 4.3 An arcade entrance
**Inspired by:** Flynn's arcade, with the digitizing laser hidden in the back.

**What it adds:** an overworld structure, a small, run-down arcade.
- Inside are **arcade cabinet** blocks that work as decorative mini-games. One hides the way down to a
  digitizing laser.
- It gives players a found way into the grid, not only a crafted one.

### 4.4 Old grids
**Inspired by:** I-No's abandoned 1980s mainframe in TRON 2.0, and the ENCOM mainframe of the first film.

**What it adds:** a Grid Tuning preset, or a rare tuned-grid style.
- **Look:** an "**old grid**": flat, low-resolution blue and red terrain. Monks pushing data, and guards with
  stun pikes.
- **Atmosphere:** one aging Tower Guardian, lonely and grateful for visitors.
- **Purpose:** a nostalgia biome that reuses the generator with a different palette and building set.

---

## 5. Glitches and infection

### 5.1 Glitch storms
**Inspired by:** the Arq Grid's glitch, Exo's time loops and Oracle's research.

**What it adds:** a grid **weather event**.
- **What happens:** blocks flicker to wrong textures, and gravity or physics briefly hitch.
- **Danger:** glitched programs, short-lived and teleporting, spawn.
- **Reward:** rare **glitch shards**, used for high-end crafting such as the light jet or the Permanence Code.
- **When:** possibly at high grid corruption, or as a respawn trigger.

### 5.2 Viral infection
**Inspired by:** Abraxas spreading infection, the Corruptor, and the FCon Monster.

**What it adds:** a corruption *source* boss distinct from the regime.
- **Spread:** a virus that spreads corrupted blocks outward over time, like a slow sculk.
- **Containment:** the grid fights it if the player helps, through disinfection pads (1.4) and freed programs.
- **Risk:** ignored, it grows a stronghold of its own.
- **Seeker:** a burrowing worm mob (inspired by the Seeker) that tunnels under the floor toward the player,
  showing as a ripple of lit floor tiles.

---

## Suggested first picks
These use the most existing systems for the effort:

1. **Bit companion (2.1):** small and self-contained; reuses the detector logic.
2. **Identity Disc (3.1):** the franchise's signature weapon. The returning throwable is a well-trodden Minecraft
   pattern (trident-like), and the memory-backup angle ties into Core Dumps.
3. **Redemption for The Warden (1.2):** a small data addition (`restore` trigger) with a big story payoff.
4. **Disinfection pads and repurposing (1.3, 1.4):** gives corruption a two-way, visible life in the city.
5. **Garage and Tower Guardian POIs (2.3):** give the city reasons to visit buildings, and address the light
   cycle TODO items (tuning, re-packing).
6. **The final boss (1.5),** as the system core.
