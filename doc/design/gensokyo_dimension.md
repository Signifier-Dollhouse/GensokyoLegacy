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
`63.5 + 128 * spline`, so the points read as:

| continentalness | spline | surface y |
|---|---|---|
| ≤ −0.51 | −0.2222 | 35 (ocean floor) |
| −0.44 … −0.30 | −0.12 | 48 (shelf) |
| −0.20 | −0.09 | 52 |
| −0.10 | −0.02 | 61 (shoreline) |
| 0.00 | 0.03 | 67 |
| 0.45 | 0.15 | 83 |
| 1.00 | 0.38 | 112 (highland top) |

The land is therefore a monotone ramp with no flat spots, which is what a river needs to exist. Total relief is 77
blocks against the overworld's 208. Sea level 63 puts the shoreline near continentalness −0.075.

`factor` ramps 3.95 → 5.0 over the same range; it only controls how steeply density falls off below the surface.

`sloped_cheese` drops the `jaggedness` term entirely, keeping `base_3d_noise` so the fine relief matches.

### Wiring it in

`NoiseRouterData.overworld` is `protected static`, so `OverworldNoiseRouter` subclasses it to be callable. The vanilla
`depth`, `factor` and `sloped_cheese` cannot be rewritten in place — their sub-trees live in density function registry
entries the overworld shares — so `FlatTerrainFunctions` wraps the `HolderGetter` and hands the builder our four keys
instead. Every other lookup, including `continents` and `base_3d_noise`, passes straight through.

Note that `NoiseRouter.mapAll` exists but is unusable here: `HolderHolder.mapAll` calls `holder.value()`, and during
`RegistrySetBuilder` bootstraps the built-in holders are unbound, so it throws.

---

## 4. The climate grid

Six continentalness bands × three temperature × three humidity × two depth = **108 points**, in
`GensokyoDimension.biomeSource`. Erosion and weirdness are `[-1, 1]` on every point; depth is `0.0` at the surface and
`1.0` underground. Bands only touch on their shared edge, where the `offset` tiebreaker falls back to list order.

| continentalness | cold | temperate | warm |
|---|---|---|---|
| −1.00 … −0.55 deep ocean | ocean | ocean | ocean |
| −0.55 … −0.25 ocean | ocean | ocean | ocean |
| −0.25 … −0.02 coast | beach | beach | beach |
| −0.02 … 0.20 lowland | snowy_plains | dry `sunflower_plains`, normal `plains`, wet `forest` | dry `desert`, normal `savanna`, wet `bamboo_jungle` |
| 0.20 … 0.55 upland | snowy_plains | dry `sunflower_plains`, normal `sakura_forest`, wet `magical_forest` | dry `desert`, normal `savanna`, wet `bamboo_jungle` |
| 0.55 … 1.00 highland | mountain | mountain | mountain |

Underground (depth 1.0): `plains` under water and coast, `snowy_plains` when cold, `magical_forest` otherwise.

Multi-noise lookup is nearest-cell on the six axes, so cells need not tile the space exactly — the nearest one wins
anywhere the grid is silent.

---

## 5. Structures

The twelve allowed biomes are all mod-owned (`GLBiomes`): nine are byte-identical copies of the vanilla biome rebuilt
from registry keys, `mountain` is new, `magical_forest` and `sakura_forest` are the mod's own. Vanilla filters
structures by biome tag (`#minecraft:has_structure/*`), so a dimension made only of mod biomes cannot host
`minecraft:village_plains` and friends. The mod's own structure sets survive because their tags already name
`gensokyolegacy:magical_forest` / `sakura_forest`.

All nine copies also drop `minecraft:canyon`; `mountain` uses `GLBiomes.addCarversAndLakes`, which is the overworld
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