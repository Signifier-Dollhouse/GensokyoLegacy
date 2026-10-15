# Gensokyo dimension (`dimension/gensokyo`)

Overworld terrain and sea level with flatter land and a handpicked biome list. Owned by
`content/dimension/GensokyoDimension.java`; registration happens in `GLDimensionGen`.

Two things differ from the overworld, and they are deliberately the only two:

1. the height chain reads **continentalness only**, where the overworld also reads erosion and weirdness;
2. the biome source is a **coarse climate grid** on continentalness / temperature / humidity / depth, with erosion
   and weirdness spanning the full range on every point so they cannot select a biome.

Everything else — sea level 63, aquifer, caves, ore veins, surface rules, spawn target, dimension type — is vanilla's.

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

Gensokyo drops the last two from both roles: `erosion` and `weirdness` stay in the router (the biome source still
reports them, and the debug screen still shows them) but nothing that shapes the ground reads them any more.

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

Those quantiles were solved against **vanilla `minecraft:overworld/continents`**, so the `share below` column is the
vanilla field's cumulative distribution. §7 swaps the spline onto a finer field with a different distribution, which
leaves the curve itself untouched — the waterline still lands where §4 needs it — but not the shares. The
measured-against-target table further down is the fine field's, and §4's band shares are sampled from it directly.

| continentalness | spline | surface y | share below | band |
|---|---|---|---|---|
| ≤ −0.80 | −0.199 … −0.191 | 38 | | flat sea floor |
| −0.70 | −0.176 | 41 | | |
| −0.65 | −0.145 | 45 | | |
| −0.60 | −0.098 | 51 | | |
| −0.56 | −0.051 | 57 | | |
| −0.53 | −0.004 | 63 | 11.7% | **waterline, ocean ends** |
| −0.50 | 0.004 | 64 | | strand |
| −0.118 | 0.129 | 80 | 43.1% | 30% plain |
| 0.108 | 0.254 | 96 | 65.1% | 22% at 80 |
| 0.255 | 0.379 | 112 | 77.1% | 12% at 96 |
| 0.329 | 0.504 | 128 | 82.1% | 5% at 112 |
| 0.380 | 0.629 | 144 | 85.1% | 3% at 128 |
| 0.436 | 0.754 | 160 | 88.1% | |
| 0.505 | 0.879 | 176 | 91.1% | |
| 0.595 | 1.004 | 192 | 94.1% | |
| 0.734 | 1.129 | 208 | 97.1% | |
| ≥ 1.05 | 1.2227 | **220 (peak)** | | flat to 2.00 |

The quantiles come from one 1024 × 1024 chunk sample, a million columns. Rounding a location to three decimals moves it
far enough to be worth a second pass: each band's measured share is fed back into the cumulative above it until the
worst band is inside 0.05 points of its target.

Measured on 1024 × 1024 chunks, seed −1838080655014084215, against the targets:

| band | target | measured | | band | target | measured |
|---|---|---|---|---|---|---|
| ocean biome | = water | 7.6% | | 128-143 | 3% | 3.5% |
| 64-79 | 30% | 27.1% | | 144-159 | 3% | 3.5% |
| 80-95 | 22% | 22.8% | | 160-175 | 3% | 3.6% |
| 96-111 | 12% | 13.9% | | 176-191 | 3% | 3.8% |
| 112-127 | 5% | 5.9% | | 192-207 | 3% | 3.8% |
| | | | | 208+ | 3% | 3.4% |

Peak 220, lowest 38, mean 100.5, and 64% of the map above y 80.

Measured after §7 moved the spline onto the fine field, so the `target` column is what the points were *solved* for
against the vanilla field and the `measured` column is what the fine field actually gives. Most bands land within a
point or three; the two that moved are **ocean, four points under its 11.7% design share**, and the bands above y 144,
which take three points from the plain. The world that results is less watery and a little higher than the original
design, and the curve shape survived the swap intact.

The ocean biome is the one band whose target is not a free number: its edge sits on the waterline, so `ocean` is
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

Six continentalness bands × three temperature × three humidity × two depth = **108 points**, in
`GensokyoDimension.biomeSource`. Erosion and weirdness are `[-1, 1]` on every point; depth is `0.0` at the surface and
`1.0` underground. Bands only touch on their shared edge, where the `offset` tiebreaker falls back to list order.

Each band is a height range of the offset spline rather than an arbitrary cut, and the shares are the continentalness
quantiles from the same 1024 × 1024 sample as §3 — the biome share of a band is the map share of its continentalness
range, to within the sliver of each band that the waterline cuts across.

| continentalness | surface y | share | cold | temperate | warm |
|---|---|---|---|---|---|
| −1.00 … −0.53 | ≤ 63 | 7.3% | ocean | ocean | ocean |
| −0.53 … −0.44 | 63 … 66 | 3.8% | beach | beach | beach |
| −0.44 … −0.118 | 66 … 80 | 24.4% | snowy_plains | dry `sunflower_plains`, normal `plains`, wet `forest` | dry `desert`, normal `savanna`, wet `bamboo_jungle` |
| −0.118 … 0.329 | 80 … 128 | 42.6% | snowy_plains | dry `sunflower_plains`, normal `sakura_forest`, wet `magical_forest` | dry `desert`, normal `savanna`, wet `bamboo_jungle` |
| 0.329 … 0.595 | 128 … 192 | 14.4% | mountain | normal `grove`, otherwise `mountain` | mountain |
| 0.595 … 1.00 | 192 … 220 | 7.2% | mountain | normal `peak`, otherwise `mountain` | mountain |

Shares are the continentalness quantiles over the same 1024 × 1024 chunk-centre sample as §3; the 0.38% the six spans
miss is the tail below −1.00, which nearest-cell sends to `ocean` either way. `ocean` reads 7.65% against a 7.3% band
because the waterline cuts a sliver across the coast band, which is the arrangement §4 wants.

The coast band is 0.09 of continentalness wide, which is 3.8% of the map: with the steep coastal ramp behind it the band
is a strand a few blocks above the water rather than a plain. Its lower edge is the waterline, so no dry column is
labelled ocean and the ocean biome is precisely the drowned set. The `−0.118` edge is where y 80 goes by, so the upland
band is exactly the "above 80" terrain, `0.329` is y 128, where the ground stops being forest country, and `0.595` is
y 192, where the two high bands split. Moving an edge trades the biomes between the rows either side of it and nothing
else; moving the first one off the waterline is the only way to put ocean back on land.

The last two rows give `grove` and `peak` to the temperate, normally-humid column only, so a cold or a dry upland still
comes out as bare `mountain`. `peak` ends at the top of the spline, where continentalness is flat at 1.2227 and the
terrain stops rising, so y 192 … 220 is the whole of it.

Underground (depth 1.0): `plains` under water and coast, `snowy_plains` when cold, `magical_forest` otherwise.

Multi-noise lookup is nearest-cell on the six axes, so cells need not tile the space exactly — the nearest one wins
anywhere the grid is silent.

---

## 5. Structures

The fourteen allowed biomes are all mod-owned (`GLBiomes`): ten are byte-identical copies of the vanilla biome
rebuilt from registry keys, `mountain` is new, `magical_forest` and `sakura_forest` are the mod's own. Vanilla filters
structures by biome tag (`#minecraft:has_structure/*`), so a dimension made only of mod biomes cannot host
`minecraft:village_plains` and friends. The mod's own structure sets survive because their tags already name
`gensokyolegacy:magical_forest` / `sakura_forest`.

`peak` is the eleventh copy but not a straight one: `minecraft:stony_peaks` supplies the climate, effects and spawns,
and `minecraft:stony_shore` is unioned in for its features and carvers, which is why it is registered separately
instead of going through `GLBiomes.COPIES`. Both sources are merged through `LinkedHashSet`s, so the two shared carvers
collapse to one entry and `FeatureSorter` still sees vanilla's ordering.

Every copy drops `minecraft:canyon`; `mountain` uses `GLBiomes.addCarversAndLakes`, which is the overworld
carver set minus that one. Canyon ravines run far deeper than Gensokyo wants.

---

## 6. Regenerating

```
./gradlew runData
```

Writes `dimension/gensokyo.json`, `dimension_type/gensokyo.json`, `worldgen/noise_settings/gensokyo.json` and
`worldgen/density_function/{offset,factor,depth,sloped_cheese}.json`. Output is committed.

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

## 7. The fine climate

`gensokyolegacy:continentalness_small`, `:temperature_small` and `:vegetation_small` are the three climate axes with
**every octave raised by one** (`firstOctave` −9→−8, −10→−9, −8→−7), amplitude lists copied from
`minecraft/worldgen/noise/*.json`. Vanilla keeps those parameters private, so the lists are duplicated in
`GensokyoDimension.noises`. Raising `firstOctave` rather than `xz_scale` shifts the octaves *and* the shift field
together, so the axes keep the same shape at twice the frequency.

All three are plain `shifted_noise` at the vanilla `xz_scale` 0.25, deliberately **not** `flat_cache`d:
`NoiseChunk.FlatCache` indexes its table by quart position and stores the value at the cell's origin corner, so it
snaps a 2D field to a 4-block grid and biome edges come out stepped.

### Why continentalness feeds the height chain too

The obvious move is to leave the terrain on vanilla continentalness and give only the biome grid the fine field. That
breaks §4's central invariant. The ocean band edge is the waterline of the offset spline, so `ocean` is the drowned set
only while the grid and the spline read the *same* field:

| | underwater | ocean biome | ocean-but-dry | wet-not-ocean |
|---|---|---|---|---|
| one field everywhere | 7.74% | 7.73% | **0.11%** | **0.11%** |
| grid only (split) | 9.21% | 7.73% | 7.40% | 8.88% |

No threshold repairs it: `continents_small` is seeded from its own key and is therefore statistically **independent**
of vanilla continentalness, so the best fit leaves ~17% of the map disagreeing. Hence the spline is splined on
`DF_CONTINENTS_SMALL` too. The cost is that the terrain now reads a field half as wide: landforms are half as broad
and twice as steep, and the height-band shares of §3 need re-solving.

### Wiring

- `offset` and `factor` are built from `DF_CONTINENTS_SMALL` directly, in `densityFunctions`.
- `FlatTerrainFunctions` maps `NoiseRouterData.CONTINENTS` → `DF_CONTINENTS_SMALL`, so the router's biome axis and the
  spline agree.
- `NoiseRouterData.overworld` builds **temperature and vegetation inline**, behind no registry key, so they cannot be
  redirected by `FlatTerrainFunctions` — which is why `noises` has to be called from `GLFeatureGen`, the owner of the
  single `Registries.NOISE` bootstrap (§6). `GensokyoDimension.smallClimateRouter` then rebuilds the `NoiseRouter`
  field by field, carrying over the other thirteen.

Measured on 256 × 256 chunks, seed −1838080655014084215: biome patch median 64 → 23 chunks, mean 524 → 162, patch
count 125 → 403, with `ocean-but-dry` at 0.11%.