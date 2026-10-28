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

## Light cycles

- **Smoother, faster driving.** Driving is jittery and slow.
- **Light-wall hotkey.** A key while driving that switches laying light barriers on and off.
- **Translucent light barriers.** They're fully opaque; they should be partly see-through.
- **Animated treads.** The tire treads would look good animated.
- **Packing it back up.** A light cycle can't be broken back down into its baton.
- **Deploy and mount.** Using the baton while sprinting should deploy the light cycle with the player already riding it.

## City variety

- **Less perfect quadrants.** The city's four quadrants are too regular; it needs more variation.
- **More waypoint kinds in the city**, e.g. a light-cycle arena, or a large plaza holding smaller POIs.

## Corruption and inhabitants

- **Bigger, more detailed strongholds, and more of them.**
- **A final boss.** `all_lieutenants_defeated` is in place to bring back (or summon) something bigger once the
  whole regime falls.
- **Real art.** Corrupted programs, enforcers and lieutenants use the Program model with existing or generated
  skins; corruption shows as a render jitter.
- **Trinkets.** Core Dumps keep Accessories slots (which also covers Trinkets mods through Accessories'
  compatibility layer). Trinkets itself has no 1.21.6 release; add direct support if it updates.
- **More trigger ideas.** Each trigger type is one record in `Triggers`; candidates: an advancement earned, the
  number of corrupted programs near the stronghold, a Grid Tuning Laser retuned to this grid.

## Streets

Varied streets (lanes, streets, boulevards, blocks of 2–5 chunks) are in; see `StreetAxis`. Possible follow-ups:

- **Boulevards out of town.** The central boulevards narrow to one-chunk highways at the city edge; wide
  highways would need the highway and terminal-connector plans to know about two-chunk roads.
- **Alleys inside a chunk.** Lanes are whole road chunks. Narrower alleys cut into building chunks would need
  building parts with smaller footprints, or a mask that clears the alley after placing a part.
- **Waypoint settlements** still use plain one-chunk streets; they could use the same street styles.

## The city's levels

Skyroads, elevated rail, subway and skyways are in, with template kinds for every piece (`TransitLayout` decides
where, `GridTransit` places the templates and builds whatever has none in code). Next:

- **Real level art.** The starter pieces copy the code-built shapes; hand-built templates would make each line
  and station distinct (several definitions per kind are picked at random already).
- **Templated end ramps.** Where a deck comes down at the city edge, the height changes partway through a chunk,
  so those stretches are still built in code.
- **Skyroads to the waypoints.** Express subway lines reach every waypoint; skyroads out along the highways
  don't yet. Diagonal highways would need a stepped deck or a dog-leg like the express lines.
- **Express lines into the city.** Express termini stop just outside the city edge; passengers walk (or ride) in.
  A transfer station joining an express line to a city line would make it one network.
- **Trains.** A light-rail train entity instead of vanilla minecarts.
- **Lifts.** Station staircases climb 17–23 blocks (more at express termini in hilly ground); a lift would be kinder.
- **Station names as data.** Names are made from two built-in word lists; a datapack could supply its own.
- **Passage loot.** Maintenance passages are empty rooms apart from their programs; a chest of salvage would give
  a reason to go looking.
- **Skyways in uniform grids**, whose taller blocks would suit them.
