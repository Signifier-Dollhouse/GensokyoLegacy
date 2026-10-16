# Gensokyo dimension (`dimension/gensokyo`)

Overworld terrain and sea level with flatter land and a handpicked biome list. Owned by
`content/dimension/GensokyoDimension.java`; registration happens in `GLDimensionGen`.

Two things differ from the overworld, and they are deliberately the only two:

1. the height chain reads **continentalness only**, where the overworld also reads erosion and weirdness;
2. the biome source is a **coarse climate grid** on continentalness / temperature / humidity / depth, with erosion
   and weirdness spanning the full range on every point so they cannot select a biome. The Tengu Settlement's band is
   the one row that humidity splits, between the town and the cliff beside it (§4).

On top of those, the caves get a third change of their own: vanilla's, plus a set of **halls** cut under the surface
and never at it — §8. Everything else — sea level 63, aquifer, cave carvers, ore veins, spawn target, dimension type —
is vanilla's. Surface rules are vanilla's with four underground biome branches in front; see `doc/design/gensokyo_biomes.md` §4.

---

## 1. The six climate parameters

`Climate.ParameterPoint` carries six climate axes plus an `offset` tiebreaker. Four of the six move terrain:

| Parameter | Router field | Moves terrain | Role |
|---|---|---|---|
| `temperature` | `temperature` | no | biome, decoration, spawns |
| `humidity` | `vegetation` | no | biome, decoration, spawns |
| `continentalness` | `continents` | **yes** | which landform |
| `erosion` | `erosion` | **yes** | how worn down |
| `depth` | `depth` | **yes** | where the surface curve sits |
| `weirdness` | `ridges` | **yes** | how jagged and peaky |

Gensokyo drops the last two as terrain: `erosion` and `weirdness` stay in the router (the biome source still
reports them, and the debug screen still shows them) but nothing that shapes the ground reads them any more.
Erosion keeps one job — it picks between the plain's two grounds (§4) — and nothing else.

---

## 2. How the overworld height works

Surface density, from `noise_router.final_density`:

```
sloped_cheese = 4 * quarter_negative((depth + jaggedness * half_negative(noise(jagged))) * factor) + base_3d_noise
depth        = y_clamped_gradient(1.5@-64 → -1.5@320) + offset
final_density = min(squeeze(0.64 * interpolated(blend_density(slope(sloped_cheese, ...)))), caves/noodle)
```

Three splines carry all of it, and each is a `spline(continents)` whose breakpoints **re-key on `erosion` and
`ridges_folded`**:

- `minecraft:overworld/offset` — base height, so the only term that places the surface;
- `minecraft:overworld/factor` — vertical gain, steepens the slope below the surface without moving the surface;
- `minecraft:overworld/jaggedness` — 3D roughness, the jagged mountain texture.

`ridges_folded` is `peaks_and_valleys(ridges)`, which only climbs past −0.7 at |weirdness| ≈ 0.667.

Two consequences, measured over erosion and weirdness:

| continentalness | vanilla surface y | spread caused by erosion + weirdness |
|---|---|---|
| −0.16 | 27 … 147 | **120** |
| 0.10 | 34 … 188 | **154** |
| 1.00 | 40 … 248 | **208** |

The river channels are the flat spot the offset spline carves at low erosion; the peaks are where
`ridges_folded` climbs. Both come from the erosion and weirdness re-keying, so removing it removes both.

The depth climate axis is coupled to the same offset spline, so moving the offset also moves the depth axis. Gensokyo
uses its own `depth`, which keeps the surface/underground biome split meaningful.

`blend_alpha` / `blend_offset` only kick in near vanilla structures, which Gensokyo has none of, so the offset reduces
to the plain spline plus `NoiseRouterData.GLOBAL_OFFSET`. They are kept so the shape matches if that ever changes.

---

## 3. The Gensokyo height chain

`gensokyolegacy:offset`, `:factor`, `:depth` and `:sloped_cheese`, registered in `GensokyoDimension.densityFunctions`.

`offset` splines continentalness with no erosion or weirdness re-keying. The surface sits at
`63.5 + 128 * spline + 128 * base_3d_noise / factor`.

The points were **solved against the real noise**, not guessed: `tools/terrain.py` already ports `ImprovedNoise` /
`PerlinNoise` / `NormalNoise` / `BlendedNoise`, so the spline can be re-measured over a chunk sample without
launching the game. With `base_3d_noise` dropped, as the tool drops it by default, the sampled surface is exactly
`floor(63.52 + 128 * spline(c))` — a monotone staircase of continentalness and nothing else, because with no noise to
add the surface is simply where `depth` crosses zero.

The curve is therefore **a choice of shares rather than a shape drawn by eye**. Every 16-block band from y 64 up gets a
spline point, and each point sits at the continentalness **quantile** that gives its band the share of the map it was
asked for. Since `CubicSpline` with zero derivatives interpolates as `low + (high - low) * t²` — not linearly — the
bands hold their share exactly, and each one is crowded toward its lower edge, where the curve is flattest.

Those quantiles were solved against **vanilla `minecraft:overworld/continents`**. §7 moves the spline onto a finer field,
which leaves the curve and the cumulative distribution with it — raising `firstOctave` changes how fast a noise varies
in space, not what values it takes, so every location still sits at the quantile it was solved for, and the waterline
still lands where §4 needs it. The `share below` column and the measured table below are that finer field's, sampled
directly on the same 1024 × 1024 run as §4.

| continentalness | spline | surface y | share below | band |
|---|---|---|---|---|
| ≤ −0.80 | −0.199 … −0.191 | 38 | | flat sea floor |
| −0.70 | −0.176 | 41 | | |
| −0.65 | −0.145 | 45 | | |
| −0.60 | −0.098 | 51 | | |
| −0.56 | −0.051 | 57 | | |
| −0.53 | −0.004 | 63 | 7.5% | **waterline, misty_lake ends** |
| −0.50 | 0.004 | 64 | | strand |
| −0.118 | 0.129 | 80 | 36.3% | 30% plain |
| 0.108 | 0.254 | 96 | 60.6% | 22% at 80 |
| 0.255 | 0.379 | 112 | 74.8% | 12% at 96 |
| 0.329 | 0.504 | 128 | 80.7% | 5% at 112 |
| 0.380 | 0.629 | 144 | 84.2% | 3% at 128 |
| 0.436 | 0.754 | 160 | 87.5% | |
| 0.505 | 0.879 | 176 | 90.8% | |
| 0.595 | 1.004 | 192 | 94.0% | |
| 0.734 | 1.129 | 208 | 96.8% | |
| ≥ 1.05 | 1.2227 | **220 (peak)** | | flat to 2.00 |

The quantiles come from one 1024 × 1024 chunk sample, a million columns. Rounding a location to three decimals moves it
far enough to be worth a second pass: each band's measured share is fed back into the cumulative above it until the
worst band is inside 0.05 points of its target.

Measured on 1024 × 1024 chunks, seed −1838080655014084215, against the targets:

| band | target | measured | | band | target | measured |
|---|---|---|---|---|---|---|
| water biome | = water | 7.73% | | 128-143 | 3% | 3.5% |
| 64-79 | 30% | 28.8% | | 144-159 | 3% | 3.3% |
| 80-95 | 22% | 24.3% | | 160-175 | 3% | 3.3% |
| 96-111 | 12% | 14.2% | | 176-191 | 3% | 3.2% |
| 112-127 | 5% | 5.9% | | 192-207 | 3% | 2.8% |
| | | | | 208+ | 3% | 2.0% |

Peak 220, lowest 38, mean 96.6, and 62% of the map above y 80.

Two things are worth reading off that table. **Ocean is 7.73% against 7.53% of the map under water**: the extra is the
sliver of the waterline row itself, where the column tops out on y 63 and nearest-cell sends it to `ocean` on a tie.
And the fine field's shares are not the ones the points were solved for — the quantiles carry over from the vanilla
field, but the *realisation* that fills a 16 km window is a different draw, so the two ground bands come out heavy and
the top two light. That spread is sampling noise: the shares are quantiles of a field whose dominant octave has a 2048
block period, so a 16 km window holds a few dozen independent patches and its quantiles carry a couple of points of
error. Re-solving the points against it would be fitting the noise, so they are left alone.

The water band is the one whose target is not a free number: its edge sits on the waterline, so `misty_lake` is
exactly the set of drowned columns. Ending it anywhere higher paints dry ground ocean, and the low spots just above the
water are shallow enough to be many separate patches rather than one rim — at −0.50 that was 1.5% of the map spread over
3,800 single-chunk bodies, which is the speckle of ocean on land in the biome map.

Continentalness is one noise field, so the *locations* in that table are a property of the seed as much as of the
curve. The shares are not: they are what the quantiles were solved for, so another seed moves where the edges sit and
not much else.

The last three points are flat (`1.05`, `1.30`, `2.00` all at 1.2227), which pins the peak at y 220 no matter how
high continentalness climbs — sampled continentalness reaches 1.51, and the overworld would put that at y 300+.

Two slopes in that table are deliberate. **The coast is roughly six times steeper than the plain**: −0.60 → −0.53
climbs 12 blocks in 0.07 of continentalness, where the plain behind it manages 16 over 0.38. Flat land a block or two
above sea level is what the lake carver floods, so the coastal ramp is where the shoreline stops being a lake shelf.
**The upland keeps steepening to the peaks**: 42 blocks of rise per unit of continentalness at the waterline, 122 at
y 128, and 190 by y 208. The interior of a continentalness maximum is flat in continentalness, so equal-share bands
would otherwise have to spread over more and more ground as they climb and the range would come out as a mesa; making
the upper bands the narrow ones is what turns it into ranges.

The price is that the plain is now a third of the map within 16 blocks of height, which is flat enough for the lake
carver again, and the water sits in basins rather than one ocean. Both are the same knob: a world that wants a broad
highland back moves the y 64 and y 80 quantiles inward.

`base_3d_noise` is what makes the staircase above look like terrain rather than a contour map: the game adds a few
blocks either side of whatever the spline says, so the rings blur and the peaks get relief. It is also why the band
shares are a statement about the tool's default view and not about the world — with `--cheese` the bands smear into
each other, and the lowest one collects the plain, because a large part of it tops out within a few blocks of y 80
and a few blocks of noise is enough to lift a slice over the line.

`factor` ramps 3.95 → 5.00; it only controls how steeply density falls off below the surface, never the surface
itself.

The land is a monotone ramp with no flat spots, which is what a river needs to exist.

`sloped_cheese` drops the `jaggedness` term entirely, keeping `base_3d_noise` so the fine relief matches.

### Wiring it in

`NoiseRouterData.overworld` is `protected static`, so `OverworldNoiseRouter` subclasses it to be callable. The vanilla
`depth`, `factor` and `sloped_cheese` cannot be rewritten in place — their sub-trees live in density function registry
entries the overworld shares — so `FlatTerrainFunctions` wraps the `HolderGetter` and hands the builder our four keys
instead. `continents` is redirected too, for the reason in §7. Every other lookup, including `base_3d_noise`, passes
straight through.

Note that `NoiseRouter.mapAll` exists but is unusable here: `HolderHolder.mapAll` calls `holder.value()`, and during
`RegistrySetBuilder` bootstraps the built-in holders are unbound, so it throws.

---

## 4. The climate grid

**Six continentalness regions**, each one a height band of the offset spline, in `GensokyoDimension.biomeSource`.
Every biome in a region shares that region's continentalness span exactly, and the region's own biomes tile the
temperature and humidity plane instead — which is why the surface half of the list is only **17 points** rather than
the 48 a six-region × 2 × 2 × 2 grid would need. Weirdness is `[-1, 1]` on every point, so it cannot pick a biome, and
erosion is too everywhere but the plain. Depth is the fourth axis and does most of the work below ground; what it
actually is, and where each band sits on it, is §9.

| continentalness | surface y | share | hot humid | cold humid | hot dry | cold dry |
|---|---|---|---|---|---|---|
| −1.00 … −0.53 | ≤ 63 | 7.53% | misty_lake | misty_lake | misty_lake | misty_lake |
| −0.53 … −0.437 | 63 … 66 | 4.28% | misty_lake_shore | misty_lake_shore | misty_lake_shore | misty_lake_shore |
| −0.437 … 0.108 | 66 … 96 | 49.99% | lower ground on low erosion, upper ground on high erosion — see below | | | |
| 0.108 … 0.329 | 96 … 128 | 20.15% | untrodden_valley | genbu_ravine | youkai_mountain_foothills | sanctuary |
| 0.329 … 0.595 | 128 … 192 | 13.25% | tengu_settlement | tengu_settlement | **secret_heaven_cliff** | **secret_heaven_cliff** |
| 0.595 … 1.00 | 192 … 220 | 4.79% | youkai_mountain_summit | youkai_mountain_summit | youkai_mountain_summit | youkai_mountain_summit |

The four columns are the climate classification `gensokyo_dimension_biome_guide.md` §2 gives for a band, in the order it
lists them: **hot humid, cold humid, hot dry, cold dry**. Temperature and humidity are therefore cut in two each, at
0, rather than in three — `COLD`/`HOT` and `DRY`/`HUMID` are `[-1, −0.01]` and `[0.01, 1]`. Cutting two ways
instead of three is also why each band holds exactly the four biomes the guide gives it, with nothing repeated.

### The plain is one band cut three ways

The plain is the guide's **lower ground and upper ground merged into a single region**, y 66 … 96, and erosion is what
tells them apart: the lower four claim `[-1, −0.01]` and the upper four `[0.01, 1]`, the same cut and the same 0.01
gap the temperature and humidity axes are cut with. Eight cells where there were two regions of four, and the
surface half of the list is still 17 points.

| | hot humid | cold humid | hot dry | cold dry |
|---|---|---|---|---|
| **erosion < −0.01** (lower ground) | sunflower_plains | muenzuka | human_village | myouren_temple_barrens |
| **erosion > 0.01** (upper ground) | magical_forest | bamboo_forest_of_the_lost | sakura_forest | hill_of_the_nameless |

Three things follow, and none of them is free.

**The two grounds interleave instead of stacking.** Before, the lower ground was everything below y 80 and the upper
ground everything above it, so the height said which one you were in. Now it is erosion, and the same height carries
both — `human_village` reaches y 96 and `magical_forest` comes down to y 66. The guide's own bands (64–80 / 80–96)
survive as *names*, and `gensokyo_biomes.md` maps them here.

**Erosion is one of the four `*_small` axes, not vanilla's.** It is `gensokyolegacy:erosion_small` — vanilla's
`minecraft:overworld/erosion` amplitudes at `firstOctave −8`, §7 — so it lands on the same octave as the temperature
axis and takes vanilla's `flat_cache` wrapper off with it, which buys the smooth boundary the other three have.
Reusing `minecraft:overworld/erosion` would have put the plain's half of the split at `−9`, `flat_cache`d, and stepped
on the 4-block grid.

The octave is worth naming because of what it costs the reading of the plain. At `−8` erosion is **tied with the
temperature axis and one octave finer than vegetation**, so the two grounds are no longer a coarse superstructure with
four climate boxes inside each: all eight cells cut on roughly one footprint. That is visible directly in
`out/terrain/gensokyolegacy_gensokyo_parameters.png` and in the biome map — there is no larger shape to read the
lower ground and the upper ground out of, only eight regions of a size. If the two grounds were meant to be the coarse
division and the boxes the fine one, erosion is the axis to put back to `−7` or `−9`.

**The split is not 50/50, and moves with the seed.** A cut at 0 is a cut at the field's median, and a noise field's
median over a finite window is not its median over the world. The share of the whole map in each erosion half, read
off the export's `PARAMETERS` section on 512 × 512 chunks:

| seed | erosion > 0 | erosion < 0 |
|---|---|---|
| −1838080655014084215 | 41.44% | 58.56% |
| 1 | 50.89% | 49.11% |
| 2 | 54.17% | 45.83% |

So the plain's upper ground takes 41% of it on the first seed and 46…51% on the other two, against 50% for a cut at
the field's own median. Moving the octave moved this the other way too: the same figure read 62.20% at
`firstOctave −9`, because the octave changes the permutation table and so the realisation, not the distribution. That
is the same order of spread the temperature and humidity cuts already have, and it is a statement about one window
rather than about the curve: the `PARAMETERS` section prints the split for whatever window it is given, so it can be
checked rather than assumed. At 256 × 256 chunks the spread is far wider — one seed in four came out 85/15 at the old
octave — because that is a 4 km window and the field's dominant octave has a 2048 block period, so the window holds
only a few independent patches.

Shares are the continentalness quantiles over the same 1024 × 1024 chunk-centre sample as §3. They move a point or two
when the octave seeds move (§7), which is why the `target` column of §3 and these are a statement about one sample
rather than a property of the curve.

Each region edge is a whole control point of the offset spline — the waterline at −0.53, then y 96, y 128 and
y 192 — so moving an edge moves the height it names and nothing else. `-0.437` is the one free value, and it is the
quantile giving the beach its ~4% share. The y 80 point is still a control point of the spline; it is simply no longer
a region edge, so the lower ground and the upper ground are free to meet anywhere inside y 66 … 96.

Neighbouring spans are held `GAP = 0.01` apart at each end, so a value in the gap is equidistant and the tie falls to
list order. The gap above the waterline is the one exception: `misty_lake` ends exactly on the waterline so it stays
the drowned set, and the gap falls on the shore side of it.

Within a region the temp/humidity boxes tile the plane with the same 0.01 gaps, so nothing "eats into" its
neighbour's box — see `out/terrain/gensokyolegacy_gensokyo_biomes.png`. Because each axis is cut once, at 0, the four
boxes of a band come out roughly a quarter of the band each; measured on 512 × 512 chunks at seed
−1838080655014084215 the lower ground's four cells are 8.14 / 8.10 / 6.04 / 5.73 % of the map and the upper ground's
are 6.96 / 5.32 / 4.16 / 3.45 %, against a plain share of 47.90% measured and 49.99% solved for. Splitting the plain
on erosion leaves its total alone — the same ~47.9% the two separate bands held before the merge — and moves it all
between the two halves.

The first two and the last rows give each region its whole plane to one biome. Those three are the fixed locations
of `gensokyolegacy_fixed_biomes_guide.md` — `misty_lake` and its shore at the waterline and the summit above the
mountain — and they are selected by continentalness alone, not by climate at all.
`youkai_mountain_summit` ends at the top of the spline, where continentalness is flat at 1.2227 and the terrain stops
rising, so y 192 … 220 is the whole of it.

The settlement band is the one row that does not tile all four columns, and it is where the fourth fixed location went.
`secret_heaven_cliff` is not in the guide: it is the dry half of the Tengu Settlement's band, so continentalness still
fixes the height and humidity alone splits the row — `tengu_settlement` on `HUMID`, `secret_heaven_cliff` on `DRY`,
both over the full temperature span. Since humidity is one field cut once at 0, the split is the same cut the four
columns are made of, and the two halves come out roughly even: measured over 256 × 256 chunks at seed
−1838080655014084215, 8.66% of the map against 7.28%, against the band's 13.25%. That is also the only row where the
order of the two cells matters for anything, since a value in the 0.01 gap between them is equidistant and the tie
falls to list order.

Which biomes fill these cells, and where each one's features come from, is `doc/design/gensokyo_biomes.md`.

Underground is **not** one cell per region. It is one continentalness row split by temperature and humidity, plus a
second, deeper row under the mountain for the ruins alone, plus the mountain's own interior rock. That is §9, and it is
the reason this list is 23 points rather than the 17 above plus four.

Multi-noise lookup is nearest-cell on the six axes, so cells need not tile the space exactly — the nearest one wins
anywhere the grid is silent.

---

## 5. Structures

Every biome the dimension may select is mod-owned (`GLBiomes`). Vanilla filters structures by biome tag
(`#minecraft:has_structure/*`), so a dimension made only of mod biomes cannot host `minecraft:village_plains` and
friends. The mod's own structure sets survive because their tags already name `gensokyolegacy:magical_forest` /
`sakura_forest`, and the `has_structure/*` and `has_spawn/*` tags are ours for the same reason — nothing here is added
to a vanilla or common biome tag, and the classification tags (`is_forest`, `is_mountain`, …) are
`gensokyolegacy:` copies rather than the shared ones. See `doc/design/gensokyo_biomes.md` §8.

Every one of the seventeen now writes its feature list out rather than copying it, so there is no copy step left to
describe: `GLBiomes.Layers` sets the two carvers and each call names its own steps. `sunflower_plains` is the same
story as the rest now — `plains` plus `patch_sunflower`, written out like every other biome. The two established
biomes are still left alone. See `doc/design/gensokyo_biomes.md` §1.

### What every biome drops

Nothing is dropped, because nothing is copied: a feature is in a Gensokyo biome only because a line in `GLBiomes` says
so. What those lines all leave out, and why:

| left out | step | why |
|---|---|---|
| `minecraft:canyon` | carving | canyon ravines run far deeper than Gensokyo wants; only the two cave carvers are set up |
| `minecraft:amethyst_geode` | `local_modifications` | treasure injected by reference rather than by terrain |
| `minecraft:monster_room` | `underground_structures` | a spawner, and the dungeon half of one |
| `minecraft:monster_room_deep` | `underground_structures` | same, deeper |

All three would otherwise have arrived through `globalOverworldGeneration`, which every vanilla overworld biome calls.
An amethyst geode every few chunks is a lot of treasure for something with no place in Gensokyo's economy, and a
monster room is difficulty and loot the map never asked for — neither is terrain, and neither could be tuned per biome
without a biome modifier, which is a worse answer than not having them.

`misty_lake` drops `minecraft:trees_water` on top of those, for the reason in `doc/design/gensokyo_biomes.md` §7.

---

## 6. Regenerating

```
./gradlew runData
```

Writes `dimension/gensokyo.json`, `dimension_type/gensokyo.json`, `worldgen/noise_settings/gensokyo.json`,
`worldgen/density_function/{offset,factor,depth,sloped_cheese,large_caves}.json` and
`worldgen/noise/{continentalness_small,temperature_small,vegetation_small}.json`. Output is committed. The biome
grid it writes is the table in §4, so `--spline` and the biome map are how a change to it gets checked.

`python3 tools/feature_cycle_check.py build/moddev/artifacts/neoforge-21.1.238-client-extra-aka-minecraft-resources.jar src/generated/resources`
must still report no feature order cycle. The checker exists because `BiomeDefaultFeatures` call order is load-bearing —
`FeatureSorter` links biomes by feature order and a cycle crashes chunk generation at runtime, not at datagen.

### One-bootstrap-per-registry rule

`RegistrySetBuilder` collects all registrations for a registry key in the **first** stub it sees; a second `init.add` for
the same registry overwrites the first with an empty map. So the density functions and the noise settings that reference
them are both declared in `GensokyoDimension.init`, and dimension type and level stem both live in `GLDimensionGen.init`.

Vanilla's biome objects cannot be reused directly for the copies: a holder owned by another provider fails
`canSerializeIn` at write time, which is why `GLBiomes.copyOf` reads through `VanillaRegistries.createLookup()` and
rebuilds from keys.

---

## 7. The climate axes

`gensokyolegacy:continentalness_small`, `:temperature_small`, `:vegetation_small` and `:erosion_small` are the four
climate axes, amplitude lists copied from `minecraft/worldgen/noise/*.json` with the octave moved. Vanilla keeps those
parameters private, so the lists are duplicated in `GensokyoDimension.noises`. Moving `firstOctave` rather than
`xz_scale` shifts the octaves *and* the shift field together, so an axis keeps its shape at a different frequency.

| axis | vanilla | now | frequency |
|---|---|---|---|
| continentalness | −9 | −7 | ×4 |
| temperature | −10 | −8 | ×4 |
| vegetation | −8 | −7 | ×2 |
| erosion | −9 | −8 | ×2 |

They are not scaled alike, and the split is deliberate. **Only continentalness moves terrain**, because it is the only
one the height chain splines, so its factor is a terrain decision — and ×4 is what makes landforms four times as steep
and forces the height shares of §3 to be re-solved. The other three move nothing but the biome grid, where the octave is
a readability choice.

That leaves two axes at −7 and two at −8 rather than a ladder, which is worth being plain about: continentalness and
vegetation are tied at the coarser octave, temperature and erosion at the finer one. The plain reads all three of its
non-continental axes, so its eight cells are all cut at two scales rather than three — the erosion half of §4 is the
consequence of that, and the paragraph there says what it costs.

What doubling an axis actually costs was measured rather than assumed, on 512 × 512 chunks at seed
−1838080655014084215, as patch count and mean patch area per biome on the biome map:

| | before (temp ×2, erosion ×1) | after (temp ×4, erosion ×2) |
|---|---|---|
| sunflower_plains | 199 patches, mean 76 | 385, mean 55 |
| muenzuka | 193, mean 62 | 388, mean 55 |
| human_village | 198, mean 59 | 358, mean 44 |
| myouren_temple_barrens | 157, mean 68 | 319, mean 47 |
| magical_forest | 366, mean 68 | 328, mean 43 |
| sakura_forest | 336, mean 62 | 332, mean 55 |
| bamboo_forest_of_the_lost | 208, mean 75 | 226, mean 48 |
| hill_of_the_nameless | 224, mean 66 | 226, mean 40 |
| tengu_settlement | 786, mean 26 | 786, mean 26 |

The lower ground's four cells roughly **double their patch count and lose a quarter of their mean area**, which is the
signature of one extra octave on the axis that cuts them in half. It is a real cost and it is bounded: a mean of 55
chunks is 880 blocks across, so the cells still read as places on a map rather than as speckle. The upper ground does
not double, because on this seed the erosion split left it holding less of the plain than it had — 19.9% against 22.8%
— and a finer cut on a smaller total is a wash.

The rows that did not move are the check that it really is only the axes: `tengu_settlement` splits on humidity alone
and is identical to the pixel, and the lake and the summit are continentalness alone.

Moving an octave costs the spline nothing. `NormalNoise` normalises on the amplitudes alone
(`expectedDeviation` over the non-zero octaves), so `firstOctave` only scales the argument the octaves are read at:
the **value distribution is unchanged**, and every quantile the region edges of §4 and the spline points of §3 sit at
still sit at the same quantile. The permutation tables do move (`octave_%d` is keyed on `firstOctave + k`), so the
realisation is a different one, and the shares measured on a 16 km window drift by a point or two — that is sampling
noise, not a changed curve, and it is not worth re-fitting. The other three axes additionally have no quantiles to
preserve: §4 cuts them once, at 0, so a finer field only changes how large the four boxes come out, not how they are
placed.

All four are plain `shifted_noise` at the vanilla `xz_scale` 0.25, deliberately **not** `flat_cache`d:
`NoiseChunk.FlatCache` indexes its table by quart position and stores the value at the cell's origin corner, so it
snaps a 2D field to a 4-block grid and biome edges come out stepped. Erosion was the one axis that arrived with
vanilla's wrapper already on it; registering it here rather than reusing `minecraft:overworld/erosion` takes the
wrapper off and buys the same smooth edge the other three have.

`blended()` keeps the same two wrappers on `offset` and `factor`, as vanilla has them. Those two sit on the *height*
chain rather than on a biome axis, so they cannot step a biome edge: the grid reads `depth`, which is
`yClampedGradient + offset`, and `depth` spans 1.5 → −1.5 over the whole column, so a 4-block snap of a term of size
±0.5 only moves it below the surface. What the wrappers buy is the sample count — see below.

### Why continentalness feeds the height chain too

The obvious move is to leave the terrain on vanilla continentalness and give only the biome grid the fine field. That
breaks §4's central invariant. The water band edge is the waterline of the offset spline, so `misty_lake` is the
drowned set only while the grid and the spline read the *same* field:

| | underwater | water biome | water-but-dry | wet-not-water |
|---|---|---|---|---|
| one field everywhere | 7.74% | 7.73% | **0.11%** | **0.11%** |
| grid only (split) | 9.21% | 7.73% | 7.40% | 8.88% |

No threshold repairs it: `continents_small` is seeded from its own key and is therefore statistically **independent**
of vanilla continentalness, so the best fit leaves ~17% of the map disagreeing. Hence the spline is splined on
`DF_CONTINENTS_SMALL` too. The cost is that the terrain now reads a field a quarter as wide: landforms are a quarter
as broad and four times as steep, and the height-band shares of §3 need re-solving.

### What is left of the ocean mismatch

Vanilla gets the match exactly, and the reason is that it snaps **both** readers of the elevation axis:
`minecraft:overworld/continents` is `flat_cache`d for the biome grid, and `offset`/`factor` are `flat_cache`d through
`splineWithBlending` for the height chain. Both read the value at the same 4-block cell origin corner, so they agree
value for value and `ocean` is the drowned set with nothing left over. Vanilla pays for it in stepped coastlines,
which it has always had.

Gensokyo takes the opposite trade on the axis itself: `continents_small` is deliberately unsnapped, so it gets the
smooth coastline and gives up the exact match. `offset` stays `flat_cache`d anyway, so the height reads the cell corner
while the grid reads the cell centre. What is left is that 2-block step. Its size is the field's gradient over two
blocks, so it scales exactly with the frequency — measured at 1024 × 1024 chunks, seed −1838080655014084215, ocean
edge −0.53, comparing the biome's cell-centre sample against the cell-origin sample the `flat_cache`d chain uses:

| `firstOctave` | ocean | dry ocean | wet not ocean | error |
|---|---|---|---|---|
| −9 (vanilla) | 4.69% | 0.061% | 0.061% | **0.122%** |
| −8 | 7.65% | 0.149% | 0.140% | **0.290%** |
| −7 (now) | 7.53% | 0.294% | 0.301% | **0.594%** |
| −6 | 7.65% | 0.578% | 0.551% | **1.129%** |

No threshold repairs it. Sweeping the ocean edge across ±0.02 of continentalness moves the total only between 0.58%
and 0.65%, and the two halves stay within 0.01 points of each other throughout — the disagreement is a per-location
gradient, not a shift, so there is no constant to absorb.

### Why the wrappers stay

Dropping `flat_cache` and `cache_2d` off `blended()` is the one way to halve that error, and it is not worth 0.594% of
map. The spline's coordinate is the full 9-octave continentalness noise, so without the wrappers every evaluation of it
re-samples. Per chunk that is ~7700 samples against the cached 50:

| consumer | samples/chunk |
|---|---|
| interpolated `underground`/`rangeChoice` subtree (5 slice fills × 245 points × 2 `sloped_cheese` × 2 splines) | 4900 |
| `preliminarySurfaceLevel` (25 columns × 49 steps; `factor` already `cache_2d`d at `NoiseRouterData:354`) | ~1250 |
| biome grid, `Climate.Sampler` at one depth sample per 4×4×4 cell | ~1536 |
| **with the wrappers** (5×5 corner table, twice) | **50** |

At 377 ns per 9-octave 2D sample (measured against `neoforge-21.1.238`) that is ~2.9 ms of extra sampling per chunk,
and it buys back only a third of the error rather than closing it. Stepped edges are the cheap side of the trade.

Note that `tools/terrain.py` cannot see this choice either way: it samples one column per chunk at the chunk centre,
block ≡ 8 (mod 16) and therefore exactly a `flat_cache` cell corner, so the snapping is a no-op at every pixel it
evaluates. The §3 and §4 tables are identical with and without the wrappers.

### Wiring

- `offset` and `factor` are built from `DF_CONTINENTS_SMALL` directly, in `densityFunctions`.
- `FlatTerrainFunctions` maps `NoiseRouterData.CONTINENTS` → `DF_CONTINENTS_SMALL` and `NoiseRouterData.EROSION` →
  `DF_EROSION_SMALL`, so the router's biome axes and the spline agree. Those two are behind registry keys in vanilla,
  so redirecting them in place is enough — nothing else in the router reads them.
- `NoiseRouterData.overworld` builds **temperature and vegetation inline**, behind no registry key, so those
  two cannot be redirected by `FlatTerrainFunctions` — which is why `noises` has to be called from `GLFeatureGen`, the
  owner of the single `Registries.NOISE` bootstrap (§6). `GensokyoDimension.smallClimateRouter` then rebuilds the
  `NoiseRouter` field by field, carrying over the other thirteen.

The height bands still cut finely, because that is continentalness doing the cutting, not climate. The patch counts
are in **The climate axes** above, measured against the previous octave on the same window.

---

## 8. The halls

`gensokyolegacy:large_caves`, in `GensokyoDimension.largeCaves` and `min`-ed into the router's final density by
`smallClimateRouter`.

### Why a separate term, and not vanilla's cheese bias

The cheap way to get more underground space is to lower the `0.27` in vanilla's cheese term, which is the one
number that sets how much rock it leaves behind. `NoiseRouterData.underground` builds

```
4 * cave_layer^2 + clamp(bias + cave_cheese, -1, 1) + clamp(1.5 - 0.64 * sloped_cheese, 0, 0.5)
```

and the router slides and squeezes that sum into the solid and the air. The obvious worry — that the caves and the
surface are the same expression, so turning caves up turns the ground to paper — **does not hold**: the last term is
clamped to `0.5` for every `sloped_cheese <= 1.5625`, which is exactly the range the `range_choice` sends down the
underground branch, so underground it is a flat `+0.5` and says nothing about where the surface sits. Measured, with
the `0.27` edited in the generated `noise_settings`:

| cheese bias | rock carved | columns with a void ≥ 40 | surface openings | surface y, median | void depth med / p90 / max |
|---|---|---|---|---|---|
| 0.27 (vanilla) | 8.68% | 4.14% | 6.80% | 92 | 8 / 24 / 124 |
| 0.00 | 23.08% | 19.38% | 6.80% | 92 | 20 / 52 / 156 |
| −0.30 | 46.34% | 60.65% | 6.95% | 92 | 44 / 92 / 176 |
| −0.60 | 62.11% | 86.24% | 7.25% | 92 | 68 / 120 / 260 |
| **the halls** | **19.73%** | **27.81%** | **6.80%** | **92** | **16 / 64 / 164** |

So the bias is a real option, and at `0.00` it costs nothing on the surface. It is just the wrong trade for the
question. The bias sets the *volume* of cave, not the *size* of the space, because a vanilla cheese cave is a slab
around the zero set of `cave_layer` — a sheet through the noise rather than a ball — so growing the threshold grows
it sideways and barely thickens it. Per unit of rock removed, `0.00` buys 0.84 columns with a void 40 blocks deep
where the halls buy 1.41, and they do it on less rock. Past `0.00` it stops being a trade at all: at `−0.30` nearly
half the world is air and the openings have started to move.

The bias also has no floor. A gate does: it is a hard zero, so the surface is not a matter of tuning how far down to
start.

The hall term is therefore built after the fact, reading the terrain rather than being read by it, and `min`ed into
the router's final density. `min` can only ever remove rock, so the surface can only move if the new term goes
negative above it — and the gate is what stops that.

### The two numbers that make it safe

`final_density` is bounded, and that is the whole margin the surface has. `postProcess` is
`mul(interpolated(x), 0.64).squeeze()`, `interpolated` clamps to ±1 and `squeeze(v) = v/2 - v³/24`, so the widest
value the router can produce is `squeeze(0.64) = 0.309`, and it is negative below that. `min` therefore leaves
`final_density` alone anywhere the new term reads **above 0.309**, and `CAVE_CEILING` is 1.0.

`depth` is the other one: `yClampedGradient(-64, 320, 1.5, -1.5) + offset` is zero exactly where §3's spline puts the
surface and falls one unit per 128 blocks, so it is a y that follows the terrain rather than a y. The gate is

```
gate = clamp(3.0 * depth - 0.25, 0, 1)
```

| depth | blocks under the surface | gate |
|---|---|---|
| ≤ 0.083 | ≤ 11 | 0 |
| 0.25 | 32 | 0.5 |
| 0.417 | 53 | 1 |

and the term is `lerp(1.0, halls, gate)` — 1.0 against a `final_density` that cannot exceed 0.309, over the whole band
the surface occupies and for 11 blocks under it. That is why the openings do not move: there is no depth at which the
term is small enough to cut, and a cave that starts 11 blocks down cannot be a cave mouth. `CAVE_GATE_DEAD` repeats
the `0.25`, so the `range_choice` returns the constant and never samples the noise above that dead zone — worth doing,
since `final_density` is evaluated per block and this adds one 3D `NormalNoise` sample to every block below it.

### The halls themselves

`minecraft:cave_cheese` again, resampled rather than re-registered: its dominant octave (`firstOctave -8`, amplitude
2) comes out near 171 × 85 blocks at `xz_scale 0.375` / `y_scale 0.75`, against the 64 × 96 that vanilla reads it at.
So a hall is wider and much flatter than the caves it sits among, and the two read as different things. The scale is
the only thing it changes — `NormalNoise` normalises on its amplitudes, so the deviation is about 0.25 at every
scale, and that is the number the hall bias is solved against.

The hall bias is the volume knob, `halls = 5.0 * cave_cheese + 1.20`, and raising it is what thins the world out.
Measured with `--voids` on 676 columns 24 blocks apart, y step 4, seed −1838080655014084215:

| | vanilla | with the halls |
|---|---|---|
| rock carved under the surface | 8.68% | **19.73%** |
| columns with a void ≥ 40 blocks deep | 4.14% | **27.81%** |
| deepest void, median / p90 / max | 8 / 24 / 124 | **16 / 64 / 164** |
| surface openings | 6.80% | **6.80%** |
| roof over the shallowest cave, min / p1 / p10 | 3 / 8 / 44 | 3 / 9 / 69 |

The last row is the one that says what the change is not: the thinnest and the second-thinnest roofs are vanilla's
own cave mouths and have not moved, and the tenth percentile has gone from 44 blocks of rock to 69. Underground it
reads by depth band as 6.1 → 23.7% at the bottom of the world, 6.3 → 12.1% at y 32…63, 7.1 → 9.9% at y 64…95 and
13.5 → 18.6% at y 128…159, so the new space is under the plain as well as under the mountain and only thins the
middle of the column where the aquifers sit.

```
python3 tools/terrain.py gensokyolegacy:gensokyo --voids --seed -1838080655014084215 --cheese
```

writes `../../out/cave/gensokyolegacy_gensokyo_caves.txt`. The maps are one pixel per chunk, so a cave is at most a
pixel of noise in them; the report is the only place these numbers exist. `--cheese` matters, since dropping
`base_3d_noise` moves the surface the report measures from.
---

## 9. The rock bands

Below the topsoil a column is split three ways, and the split is carried entirely by **depth** and **continentalness**.
Temperature and humidity only choose *which* underground biome a column gets, never *which band* — which is what lets
the four underground biomes collapse to one cell per climate in the tool.

The bands are:

| band | depth | continentalness | biome |
|---|---|---|---|
| surface | `[-1.50, 0.10]` | the region | the region's own four, or the one fixed location |
| mountain interior | `[0.30, 0.53]` | `[0.50, 1.00]` | `mountain_interior`, one cell over the whole temp/humid plane |
| underground | `[0.53, 1.50]` | `[-1.00, 0.22]` | the four, split cold/hot × dry/humid |
| mountain ruins | `[1.70, 2.00]` | `[0.50, 1.00]` | `former_hell` alone |

`former_hell` is the only underground biome that appears in both rows. The other three stay on the low-continentalness
row because they belong to the low ground and have no business claiming the mountain's deep rock.

### Depth is surface-relative

```
depth = 1.5 - (y + 64) / 128 + offset
offset = GLOBAL_OFFSET + spline(continentalness) + blend_offset      GLOBAL_OFFSET = -0.703125
```

So depth is **one unit per 128 blocks**, plus an offset that carries the whole height spline: the ground under a mountain
is at a much larger depth than the ground under the sea at the same y. Two consequences follow, and both shaped the
layout:

- A constant depth cut is **not** a flat y. The cave/underground boundary follows `DEEP_DEPTH + (cont - C_HI)`, which
  is 90 blocks of tilt across the map. Continentalness bounds are the only way to make a boundary flat.
- Only the mountain reaches the bottom. Depth at the world floor (y −64) is `1.5 + offset`, which is **2.02** under the
  summit and **1.05** under the water — so the lowland row's 1.5 ceiling is never binding and the mountain's is. The
  mountain row stops at **2.0** because the climate codec refuses any parameter outside `[-2, 2]`; the last twentieth of
  a block at y −64 is interpolated instead of claimed.

### Bounds, and where the boundaries actually land

`Climate.ParameterList` is a nearest-cell lookup: a cell's fitness is the sum of squared distances outside each of its
spans, and a boundary between two spans therefore sits at the **midpoint** of their bounds rather than at either. Every
number in this section is a consequence of that.

| bound | value | what it moves |
|---|---|---|
| `SURFACE_DEPTH` | 0.10 | where the surface rows stop being claimed |
| `CAVE_DEPTH` | 0.30 | the cave's floor |
| `DEEP_DEPTH` | 0.53 | both the cave's ceiling and the underground's floor — the only pair that tiles |
| `HELL_DEPTH` | 1.70 | the mountain's ruins floor |
| `HELL_FLOOR` | 2.00 | its ceiling, at the codec limit |
| `UNDERGROUND_CONT` | 0.22 | the surface/underground crossover through the foothills |
| `CAVE_CONT` | 0.50 | the cave's and the mountain rows' continentalness floor |
| `CAVE_CONT_CEILING` | 1.00 | its ceiling, closed so the cave competes past 1.0 instead of owning every column |

`CAVE_DEPTH` is deliberately *not* the surface row's ceiling. The gap between 0.10 and 0.30 is interpolated rather than
claimed, so the surface/cave boundary lands at the midpoint 0.20 with a band of rock around it won on the falloff.

### What each bound owns

This is the part that is not obvious, and both halves were measured rather than assumed — with
`tools/cave_bands.py`, which simulates the real fitness rule and reports the mean and the drop.

**`UNDERGROUND_CONT` owns the drop across the mountain's shoulder.** The low-continentalness row is penalised by
`(cont - UNDERGROUND_CONT)²`, so its crossover with the surface climbs as `SURFACE_DEPTH + (cont - UNDERGROUND_CONT)`.
That ramp is the drop: at 0.10 it rises to y67 at cont 0.26 and then crashes to y38 by cont 0.34, because at cont ≈ 0.30
it hands over to a second, flat crossover against the cave. Raising the bound walks the ramp over:

| `UNDERGROUND_CONT` | drop over cont 0.15…0.45 | boundary | mountain mean |
|---|---|---|---|
| 0.10 | 29 blocks (y 38…67) | | 70 |
| 0.18 | 20 | | 72 |
| **0.22** | **17 (y 58…75)** | | **73** |
| 0.26 | 21 | | 75 |
| 0.30 | 29 | | 76 |

**`HELL_DEPTH` owns the summit, and only the summit.** Its floor is out of reach of the foothills — depth 1.70 is below
the world floor below continentalness 0.44 — so cutting it cannot flatten the drop. It only lifts the mountain:

| `HELL_DEPTH` | drop | mountain mean |
|---|---|---|
| 1.82 | 29 | y64 |
| **1.70** | 29 | **y70** |
| 1.20 | 29 | y102 |
| 0.80 | 35 | y127 |
| 0.60 | 36 | y140 |
| 0.45 | 45 | y155 |
| 0.30 | 76 | y187 |

The drop *grows* as `HELL_DEPTH` is cut, because a shallower floor makes the mountain row win earlier and more steeply
across the foothills, and the foothills are exactly where it is cont-penalised.

The mountain row's **continentalness floor** is inert for the same reason: sweeping it from 0.50 down to 0.15 changes
nothing at all, because below continentalness 0.44 there is no rock at that depth to claim.

### Measured

128 × 128 chunks at seed −1838080655014084215, origin −384, −384, cap y 50:

```
python3 tools/cave_bands.py gensokyolegacy:gensokyo --seed -1838080655014084215 \
    --origin="-384,-384" --bounds 0.1,0.3,0.53,0.53,0.5,0.22,1.0,1.70 --target-y 64
```

| | |
|---|---|
| claimed by a cell — surface / cave / underground | 69.3% / 12.5% / 53.2% |
| cave band thickness | 6 … 117 blocks |
| cave/underground boundary over the mountain | y 66 … 77, mean 74 |
| drop across the shoulder, cont 0.15…0.45 | 17 blocks, y 58 … 75 |

The surface share is the one number here that §4's plain merge moved, and only barely: 68.5% to 69.3%. The plain's two
surface cells became one, and a single cell over the wider continentalness span claims 0.8 points more of the columns
it sits over than two cells either side of it could. The cave and the underground are untouched, which is the check
that the merge stayed a surface change.

A column whose band boundary is won on the falloff gets a **blend of the two biomes**, not one of them, so the pale
tint in `out/terrain/gensokyolegacy_gensokyo_cave_bands_*.png` is rock that exists but has no name. Solid fill is a cell
that claims the column outright.

### The cap

The underground reaches **y 77** at its highest, in 68 of the sampled columns. That is a breach of the y 50 target, and
it is deliberate: the boundary is where the cave stops and the ruins begin, and the cave has to be thick enough under
the summit to be worth walking into. The breach is worst in the last stretch past continentalness 1.0, where the surface
rows stop being claimed at all and the underground takes over from the top. `cave_bands.py` exits non-zero on a breach
anyway, which is now crying wolf — the target is a mean, and the mean is 74.

Each column's biome *is* blended across the boundary, so the practical effect of the breach is a column partway
`mountain_interior` and partway `former_hell`, not a hard edge.

### Construction

`mountain_interior` is the one rock band that is not a climate region: a single cell over the whole temperature and
humidity plane, keyed on continentalness and depth only. It takes `Biomes.DEEP_DARK` as its base, an empty tail, and
plain stone as its surface rule — it only ever sits between the two rock bands, so there is no deepslate below it to
grade into and no floor worth giving a different material. `former_hell` is the guide's own biome; the other three
underground biomes keep the palettes of §4.
