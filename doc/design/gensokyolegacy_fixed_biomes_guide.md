# Gensokyo Fixed Biomes: Names, Features, and Surface Rules
**Project:** Gensokyo Legacy  
**Mod ID:** `gensokyolegacy`  
**Target:** Minecraft Java 1.21.1 + NeoForge  
**Scope:** Four fixed-location biomes to add to the existing Gensokyo dimension.

This is an additive implementation brief. Inspect the existing dimension, biome registry, surface-rule assembly, configured/placed features, biome modifiers, and structures before editing. Reuse the project's current registration style and preserve existing registry IDs.

## 1. Recommended English names and registry IDs

| Chinese name | Recommended English display name | Suggested biome ID | Naming notes |
|---|---|---|---|
| 雾之湖 | **Misty Lake** | `gensokyolegacy:misty_lake` | Established English name for the lake associated with Cirno and the Scarlet Devil Mansion. |
| 雾之湖畔 | **Misty Lake Shore** | `gensokyolegacy:misty_lake_shore` | Descriptive biome name for the lakeside zone, including Cirno's usual area and the approach/grounds around the Scarlet Devil Mansion. |
| 天狗聚落（妖怪山腰） | **Tengu Settlement** | `gensokyolegacy:tengu_settlement` | Descriptive name for a settlement on Youkai Mountain's slopes. “Tengu Village” also works if the area is specifically a village. |
| 妖怪山顶 | **Youkai Mountain Summit** | `gensokyolegacy:youkai_mountain_summit` | Clear display name for the high-altitude summit region. |

Keep display names separate from registry paths. If any of these biomes already have IDs in the codebase or saved-world data, preserve those IDs rather than renaming them.

## 2. Misty Lake

### Identity
A broad freshwater lake with cool mist, clear shallow water, deeper water toward the center, and occasional reeds or small islands. The lake should dominate the view; avoid making it read as a swamp.

### Feature palette
- **Lakebed:** sand and gravel in shallows, clay patches in sheltered areas, normal base stone or darker sediment deeper down.
- **Aquatic vegetation:** sparse lily pads, small aquatic plants, and occasional submerged vegetation.
- **Shoreline details:** reeds/cattails should be concentrated in coves, not distributed uniformly around the entire lake.
- **Landmarks:** rare rocky islets, submerged ruins, or distinctive stone outcrops if compatible with project lore.
- **Atmosphere:** cool water color, light surface mist, subdued ambient sounds. Keep fog subtle enough for navigation and combat.
- **Avoid:** large trees and dense grass in open water; frequent islands; heavy permanent fog.

### Implementation notes
- Use water-aware placement predicates for lily pads and aquatic plants.
- Use shore/edge checks for reeds so they do not appear in deep water or dry hills.
- Verify the feature's placement modifiers and survival checks agree about required water and lakebed blocks.
- The main lake basin and water body should come from terrain/noise, aquifers, and fluid settings—not from many small “lake” features. Small pond features are more appropriate inside other biomes.

### Surface rules
Apply the Misty Lake palette to **underwater lakebed terrain**, not every block in the biome.

1. Underwater floor: sand/gravel; clay patches can be handled by placed features or controlled noise conditions.
2. Shoreline: ideally transitions into `misty_lake_shore`.
3. Exposed steep banks: stone/gravel where slope conditions justify it.
4. Deeper subsurface: preserve the dimension's normal stone/deep-material rules.
5. Open water itself is not created by surface rules; water level, aquifers, terrain shape, and fluids must already be configured.

Do not replace every floor block in `misty_lake` with sand: that can affect cave floors or exposed surfaces that are not lakebed. Combine the biome predicate with floor, water-level, and stone-depth checks supported by the current rule tree.

## 3. Misty Lake Shore

### Identity
The transition zone between the lake and surrounding land. It should evoke Cirno's familiar lakeside environment and provide a plausible approach to the Scarlet Devil Mansion. This is a shoreline biome, not another copy of the open lake.

### Feature palette
- **Ground:** grass/dirt on stable land; sand/gravel at the waterline; clay, mud, or coarse dirt in damp coves; scattered stones and boulders on exposed shore.
- **Vegetation:** reeds and aquatic plants near water; grass, ferns, and flowers further inland; sparse small trees and bushes to preserve lake views.
- **Minor landmarks:** stepping stones, a short dock, a fishing spot, or a tiny abandoned lakeside shelter.
- **Ice:** optional small ice patches in selected cold areas if consistent with the dimension's climate design. Do not freeze the entire lake unless that is intentional.
- **Major landmark:** the Scarlet Devil Mansion should normally use the structure system with strict spacing and a valid placement region, not be a common random feature.
- **Atmosphere:** slightly cool and humid; mist is stronger near water but should not obscure entrances.

### Implementation notes
- Prefer several narrow shoreline features over one giant uniform decoration pass.
- Use water-edge conditions for reeds, aquatic plants, and mud patches.
- Keep the shore navigable; do not make every waterline block tall vegetation.
- Configure the mansion's biome validity and placement rules explicitly. Ensure the footprint has enough flat land and does not spawn underwater or on an extreme slope.
- Make biome selection create a coherent band around the lake rather than scattering the shore biome across unrelated terrain.

### Surface rules
- Dry ground: grass block over dirt.
- Waterline: sand/gravel, with local clay/mud patches.
- Steep banks: stone/gravel.
- Underwater floor: share the Misty Lake lakebed palette where water crosses the biome boundary.

Reuse shared helper methods or palette definitions for `misty_lake` and `misty_lake_shore`, but use different conditions:
- `misty_lake`: underwater floor and submerged terrain.
- `misty_lake_shore`: exposed ground and narrow water-edge transitions.

Biome checks alone do not guarantee a smooth shore. Biome selection must place the shore at the correct boundary, and surface rules must still distinguish underwater floor, exposed ground, and steep slopes.

## 4. Tengu Settlement

### Identity
A built-up settlement on Youkai Mountain's slopes. It should feel like a functioning mountain community, with buildings and paths adapted to uneven terrain. Its identity should come mainly from structures, roads, and settlement layout—not an unusual grass block.

### Feature palette
- **Ground:** grass/dirt on terraces; coarse dirt, gravel, and stone on paths and steep sections.
- **Infrastructure:** retaining walls, stone steps, short bridges, terraces, and platforms connecting different elevations.
- **Vegetation:** small mountain-tree clusters around the settlement edge, shrubs, ferns, and hardy grasses on unused slopes. Keep roads, plazas, and building footprints clear.
- **Buildings:** houses, watchtowers, gates, platforms, bridges, and gathering spaces should use structure templates/sets.
- **Layout:** a clear path hierarchy with a main route, secondary paths, stairs, and courtyards. Rare watchtowers or lookouts can occupy higher ledges.
- **Atmosphere:** mountain air, moving-cloud/wind particles, and distant bird-like ambient sounds if supported. Use lanterns or existing lighting blocks without excessive light spam.

### Implementation notes
- Major buildings should be structures, not `PlacedFeature`s that stamp buildings randomly into terrain.
- Configure biome validity, spacing/separation, terrain checks, and structure sets using the project's current NeoForge 1.21.1 approach.
- If the settlement must be one coherent area, consider a large structure or custom settlement placement system instead of many unrelated structure starts.
- Paths and terraces must connect logically. Random path patches alone tend to look noisy.
- Prevent overlapping structures, buildings over ravines, floating foundations, and buried entrances.
- Account for structure footprints when placing natural vegetation.

### Surface rules
- Default settlement terrain: grass block over dirt.
- Natural exposed steep slopes: stone/gravel.
- Paths: gravel/coarse dirt generated by path features or structure blocks, not by painting the whole biome floor with a surface rule.
- Retaining walls and foundations: place them through structures.

Do not encode paths, foundations, or buildings in surface rules. Keep the dimension-level grass/dirt rules unless the settlement's natural topsoil genuinely differs. Let structures and placed features establish its identity.

## 5. Youkai Mountain Summit

### Identity
A high-altitude summit zone with exposed rock, wind-swept vegetation, steep ridges, and occasional snow or frost depending on the dimension's climate model. It should feel higher, harsher, and more exposed than the foothills.

### Feature palette
- **Terrain:** peaks, ridgelines, cliffs, and scree slopes should come mainly from terrain/noise settings.
- **Surface:** stone, gravel, exposed rock, and sparse soil pockets.
- **Snow/frost:** snow layers or snow blocks if compatible with the intended temperature and seasonal rules.
- **Vegetation:** sparse alpine grass, hardy shrubs, moss in sheltered places, and occasional stunted trees below the most exposed summit. Reduce tree frequency with elevation/exposure.
- **Local features:** boulders, scree patches, rare small caves/rock arches, wind-exposed ledges, snowdrifts.
- **Landmark:** a rare summit shrine, lookout, or other major landmark if supported by project lore.
- **Atmosphere:** cooler temperature, wind-like particles, cloud/mist accents. Avoid constant dense fog or particle storms.

### Implementation notes
- Use height/exposure-aware placement if supported; otherwise constrain features with heightmap predicates, slope checks, or biome placement.
- Keep trees uncommon and avoid giant-tree features.
- Place boulders only where they have valid ground support.
- Snow should follow the intended biome temperature and worldgen logic; registration alone does not guarantee correct snow placement.
- Use terrain generation for cliffs and peaks; use features for local detail.

### Surface rules
- Exposed summit floor: stone or the dimension's mountain rock.
- Gentle ledges and soil pockets: sparse grass/dirt.
- Loose slopes: gravel or scree-like blocks.
- Snow/frost: use the appropriate snow surface/placement logic if this biome should be cold enough.
- Deep subsurface: preserve shared dimension stone rules.

Condition summit overrides on the summit biome and relevant surface context. Use floor/stone-depth/steep predicates where available. Avoid a simple `Y > threshold => stone` rule if it creates a horizontal stone layer through neighboring biomes or affects underground terrain. Use the biome predicate for the summit palette, with height/slope checks only for details that truly depend on exposure.

## 6. Integrating with the existing dimension surface rules

Add these biomes to the existing `gensokyolegacy` surface-rule architecture; do not create a parallel generation path.

### Recommended priority
1. Bedrock and dimension-boundary rules.
2. Special underwater/lakebed rules, conditioned on biome and water/floor context.
3. Summit exposed-rock/snow rules.
4. Any genuinely unique shore or settlement topsoil overrides.
5. Existing dimension-wide topsoil and subsurface defaults.
6. Existing fallback terrain material.

The exact nesting depends on the existing `SurfaceRules.sequence` structure. Order matters: an early broad rule can shadow a more specific later rule. Each biome override must either fully handle the positions it claims or be structured so unmatched positions can reach the shared fallback.

### Suggested palette helpers

| Helper/palette | Biomes | Purpose |
|---|---|---|
| `lakebedSurface` | Misty Lake and underwater parts of Misty Lake Shore | Sand/gravel/clay on underwater floor only |
| `shoreSurface` | Misty Lake Shore | Grass/dirt inland; sand/gravel at water edge |
| `settlementSurface` | Tengu Settlement | Usually reuse dimension defaults; path blocks are features/structure blocks |
| `summitSurface` | Youkai Mountain Summit | Exposed rock, scree, sparse soil, optional snow/frost |

### Illustrative Java structure

This is pseudocode: adapt it to the project's existing helpers and compile against the exact 1.21.1 mappings.

```java
// Use ResourceKeys from the project's registered biomes.
private static SurfaceRules.RuleSource gensokyoSurfaceRules() {
    return SurfaceRules.sequence(
        // Specific lakebed and summit exceptions first.
        mistyLakeSurfaceRules(),
        mistyLakeShoreSurfaceRules(),
        youkaiMountainSummitSurfaceRules(),

        // Only needed if natural settlement topsoil differs.
        tenguSettlementSurfaceRules(),

        // Existing shared dimension-level fallback.
        existingGensokyoBaseSurfaceRules()
    );
}
```

Do not copy the sequence blindly: if an override returns a rule for all positions in its biome, it may shadow the base rule for deeper layers. Implement each helper with correct floor, water-level, stone-depth, and steepness conditions, or nest it within the existing surface-rule design.

### Surface rules versus features

| Desired result | Correct mechanism |
|---|---|
| Sand/gravel on lake floor | Surface rule with underwater/floor conditions, or carefully placed lakebed features |
| Reeds, lily pads, aquatic plants | Configured/placed features with water/shore checks |
| Grass, dirt, stone topsoil | Shared surface rules with biome-specific exceptions |
| Dock or bridge | Structure/template or placed feature, depending on size |
| Scarlet Devil Mansion | Structure system |
| Tengu houses, roads, stairs, retaining walls | Structures/templates and settlement placement |
| Summit cliffs and peak shape | Terrain/noise generation |
| Boulders, alpine plants, snowdrifts | Placed features |
| Mist, wind, ambience, colors | Biome effects / dimension atmosphere systems |

## 7. Suggested registry IDs and implementation checklist

```text
gensokyolegacy:misty_lake
gensokyolegacy:misty_lake_shore
gensokyolegacy:tengu_settlement
gensokyolegacy:youkai_mountain_summit
```

- [ ] Inspect existing biome registration and preserve existing IDs.
- [ ] Confirm `misty_lake` generates a contiguous basin and `misty_lake_shore` a coherent transition band.
- [ ] Ensure lake water and lakebed terrain come from the terrain/fluid system, not vegetation features.
- [ ] Register aquatic and shore vegetation with placement checks matching their required blocks.
- [ ] Add the Scarlet Devil Mansion through the structure system with terrain validity and spacing.
- [ ] Implement Tengu Settlement as coherent structures and paths adapted to mountain slopes.
- [ ] Make Youkai Mountain Summit visibly rockier and more exposed than the foothills.
- [ ] Integrate only necessary material overrides into the existing dimension surface-rule tree.
- [ ] Test lakebed, shoreline, steep banks, summit cliffs, caves, and chunk borders.
- [ ] Verify surface-rule order preserves shared dirt and stone layers.
- [ ] Test fresh chunks; existing generated chunks generally will not regenerate automatically.

## 8. Reference terminology

- Touhou Wiki — [Misty Lake](https://en.touhouwiki.net/wiki/Misty_Lake)
- Touhou Wiki — [Scarlet Devil Mansion](https://en.touhouwiki.net/wiki/Scarlet_Devil_Mansion)
- Touhou Wiki — [Youkai Mountain](https://en.touhouwiki.net/wiki/Youkai_Mountain)
- Touhou Wiki — [Tengu](https://en.touhouwiki.net/wiki/Tengu)
- NeoForge 1.21.1 — [Biome Modifiers](https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/)
- NeoForge/Minecraft 1.21.1 API — [SurfaceRules](https://lexxie.dev/neoforge/1.21.1/net/minecraft/world/level/levelgen/SurfaceRules.html)

## 9. Final direction

Add these four biomes to the existing dimension rather than creating a parallel generation path. Keep common grass/dirt/stone behavior at dimension level, add lakebed and summit material rules only where necessary, and use placed features or structures for vegetation, buildings, paths, and landmarks.

The result should read as four distinct places:
- **Misty Lake:** open water and underwater terrain.
- **Misty Lake Shore:** natural transition land, Cirno-associated details, and access toward the Scarlet Devil Mansion.
- **Tengu Settlement:** coherent mountain architecture and navigable paths.
- **Youkai Mountain Summit:** exposed rock, sparse alpine life, and a high-altitude silhouette.
