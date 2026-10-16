# Gensokyo Dimension: Biome Naming, Features, and Surface Rules
**Target:** Minecraft Java 1.21.1 + NeoForge  
**Audience:** coding agent implementing the Gensokyo dimension  
**Status:** design/implementation guide, not a drop-in patch. Adapt registry names, block IDs, and existing worldgen architecture to the repository.

## 1. Goals and assumptions

The dimension has eight climate-space slices. Three slices contain multiple biomes. The dimension also has an underground region. The current vertical bands are:

- **lower ground:** Y=64–80
- **upper ground:** Y=80–96
- **Mountain:** Y=96–128
- **Underground:** separate region, with its own biome selection

Within each surface band, the four biomes are listed in the order **hot humid, cold humid, hot dry, cold dry**. Treat this ordering as the project's climate classification, not as a claim about canonical Touhou geography.

The generator should separate four responsibilities:

1. **Terrain shape and biome selection:** climate/continentalness/erosion/peaks-valleys or the project's custom noise pipeline.
2. **Surface material:** grass, dirt, sand, mud, stone, ash, etc. Use dimension-wide defaults, then altitude/region rules, then biome overrides.
3. **Biome features:** trees, flowers, rocks, ponds, vegetation patches, fossils, structures, and other placed features.
4. **Atmosphere/gameplay:** fog, sky/water/foliage colors, ambient particles, music, mob spawns, and special effects.

Do not try to express all vegetation and structures through surface rules. Surface rules replace terrain blocks during terrain generation; placed features decorate the generated terrain later.

## 2. English biome names

Use established English names for known Touhou locations where possible. A few entries are descriptive biome labels rather than official named locations. Keep the internal registry path stable even if the display name changes.

| Vertical band | Chinese name | Recommended English display name | Notes |
|---|---|---|---|
| lower ground | sunflower plains | **Sunflower Plains** | Existing Minecraft biome name; use only if this is intentionally the vanilla-like sunflower biome. |
| lower ground | 无缘冢 | **Muenzuka** | Established English spelling. A lonely burial ground for the unconnected/deceased; avoid making it a generic flower meadow. |
| lower ground | 人里 | **Human Village** | Established location name. |
| lower ground | 命莲寺荒原 | **Myouren Temple Barrens** | Descriptive biome name based on Myouren Temple; “Barrens” is a biome label, not a separate canonical location. Consider “Myouren Temple Wastes” if the landscape is rocky/ruined. |
| upper ground | 魔法森林 | **Forest of Magic** | Established English name. |
| upper ground | 迷途竹林 | **Bamboo Forest of the Lost** | Established English name. |
| upper ground | 樱花森林 | **Sakura Forest** | Descriptive biome label; “Cherry Blossom Forest” is clearer to players, but “Sakura Forest” has a fantasy-biome feel. |
| upper ground | 无名之丘 | **Hill of the Nameless** | Established English location name; distinct from Muenzuka. |
| Mountain | 未踏溪谷 | **Untrodden Valley** | Established English location name. |
| Mountain | 玄武之泽 | **Genbu Ravine** | “Genbu Ravine” is the established location name. If this biome is predominantly a wetland, retain the name but build it as a ravine marsh. |
| Mountain | 妖怪山麓 | **Foot of Youkai Mountain** | Descriptive/established location wording. “Youkai Mountain Foothills” is also natural as a biome display name. |
| Mountain | 圣域 | **Sanctuary** | Ambiguous without a specific Touhou location reference. Use “Sanctuary” provisionally; do not imply it is a canon proper noun unless the project lore defines it. |
| Underground | 血池地狱 | **Hell of Blood Pools** | Established English location name. |
| Underground | 畜生界 | **Animal Realm** | Established English name. |
| Underground | 灼热地狱 | **Hell of Blazing Fires** | Established English rendering. If representing the abandoned former location specifically, use “Former Hell of Blazing Fires.” |
| Underground | 旧地狱 | **Former Hell** | Established English name. |

Suggested registry IDs (illustrative; follow the mod's existing naming convention):

```text
gensokyolegacy:sunflower_plains
gensokyolegacy:muenzuka
gensokyolegacy:human_village
gensokyolegacy:myouren_temple_barrens
gensokyolegacy:forest_of_magic
gensokyolegacy:bamboo_forest_of_the_lost
gensokyolegacy:sakura_forest
gensokyolegacy:hill_of_the_nameless
gensokyolegacy:untrodden_valley
gensokyolegacy:genbu_ravine
gensokyolegacy:youkai_mountain_foothills
gensokyolegacy:sanctuary
gensokyolegacy:hell_of_blood_pools
gensokyolegacy:animal_realm
gensokyolegacy:hell_of_blazing_fires
gensokyolegacy:former_hell
```

Do not use spaces or translated display names as registry paths. If these biome IDs already exist, preserve them to avoid breaking saved worlds, datapacks, or references.

## 3. Feature design guide: surface biomes

For each biome, implement a coherent feature palette rather than a large random list. Start with a small set of high-frequency ground cover, a few medium-frequency landmarks, and rare set pieces. Features should have sensible placement predicates, spacing, and survival checks.

### 3.1 Sunflower Plains

**Visual target:** open, bright grassland with broad sightlines.

- Surface: grass block and dirt; occasional coarse dirt or packed dirt paths.
- Vegetation: sunflower patches, tall grass, dandelions, poppies, small seasonal flower clusters.
- Terrain accents: low grassy mounds, shallow ponds, occasional boulders.
- Trees: very sparse; isolated small deciduous trees if desired.
- Structures: rare farmsteads, abandoned carts, or small roadside shrines only if supported by the mod's structure system.
- Avoid: dense forests, frequent cliffs, or feature density that obscures the open plains silhouette.

### 3.2 Muenzuka

**Visual target:** quiet, slightly eerie burial ground at the edge of civilization.

- Surface: muted grass, podzol/coarse dirt in patches, exposed roots and stones.
- Vegetation: sparse dark trees, low grass, withered flowers, small purple-flower accents; keep clearings around graves.
- Landmarks: unmarked grave mounds, old stone markers, a distinctive purple-blossomed tree as a rare landmark.
- Ground details: leaf litter, roots, mossy stones, small depressions.
- Atmosphere: subdued foliage palette and optional low-frequency particles; do not make the whole biome a poisonous hazard unless gameplay calls for it.
- Avoid: cheerful, dense sunflower-like flower carpets. Keep it visually distinct from Hill of the Nameless.

### 3.3 Human Village

**Visual target:** cultivated, navigable countryside around a safe settlement.

- Surface: grass and dirt, with coarse dirt/gravel paths around roads and building plots.
- Vegetation: crops and gardens should mostly belong to structures or dedicated farm features, not blanket random decoration.
- Trees: scattered orchard trees, hedges, bamboo only as a rare imported accent.
- Features: paths, irrigation ditches, wells, small ponds, fences, field margins.
- Structures: village houses, shops, school/temple-related buildings, market stalls, and paths. Prefer the mod's structure templates/structure sets over trying to approximate buildings with random placed features.
- Spawn design: favor relatively flat buildable areas and enough clearance around structure footprints.
- Avoid: making every chunk a village. Keep structures sparse enough that players can build and terrain remains readable.

### 3.4 Myouren Temple Barrens

**Visual target:** open, dry or degraded ground around a religious landmark, not necessarily a literal ruined temple.

- Surface: coarse dirt, gravel, exposed stone, sparse grass; occasional mossy patches near water or old stonework.
- Vegetation: low scrub, dry grass, a few hardy trees and weeds.
- Features: stone outcrops, weathered paving fragments, small prayer-stone clusters, shallow gullies.
- Structures: temple/cemetery compounds should be rare structures with clear footprints, not dozens of isolated temple blocks.
- Palette: muted earth tones with occasional Buddhist-temple red/stone accents in structures.
- Avoid: excessive lava, ash, or nether-like blocks unless this biome is intentionally volcanic.

### 3.5 Forest of Magic

**Visual target:** dense, uncanny woodland with oversized mushrooms and magical undergrowth.

- Surface: grass, podzol, rooted dirt, occasional mycelium-like patches if appropriate to the mod.
- Trees: dense mixed canopy, irregular tree sizes, occasional giant trees or hollow trunks as rare landmarks.
- Understory: mushrooms, ferns, tall grass, glowing plants, strange flowers, fallen logs, roots.
- Features: small clearings, witch-like huts or abandoned magical camps as rare structures; magical crystals should be very rare and not look like ordinary ore deposits.
- Atmosphere: darker foliage and optional subtle ambient particles. Keep visibility and navigation playable.
- Avoid: uniform tree spacing. Use multiple tree placed features with different footprints and rarity to create irregular density.

### 3.6 Bamboo Forest of the Lost

**Visual target:** tall, dense bamboo with narrow sightlines and winding clearings.

- Surface: grass and podzol/rooted dirt; damp mud near ponds.
- Vegetation: dense bamboo clumps with varied height; sparse ferns and small flowers along clearings.
- Features: winding footpaths, small pools, fallen bamboo, occasional stone lanterns or abandoned trail markers.
- Structures: Eientei or other major sites should be rare structures, not common decoration.
- Generation: use clump/patch features with a suitable ground predicate and enough horizontal spacing to prevent every column from becoming solid bamboo.
- Avoid: an impassable wall of bamboo. Preserve navigable gaps and occasional clearings.

### 3.7 Sakura Forest

**Visual target:** cherry-blossom woodland with seasonal color and a soft, readable canopy.

- Surface: grass, podzol and occasional moss; petals can be represented with custom ground-cover blocks or flower features.
- Trees: cherry/sakura trees of several sizes, mixed with small understory trees; use clustered groves rather than an even grid.
- Vegetation: pink/white flowers, grass, ferns, petals, occasional mushrooms.
- Features: blossom clearings, shallow streams, small ponds, stepping stones, fallen blossom patches.
- Rare landmark: a large ancient sakura tree or a blossom-lined path.
- Avoid: using only one tree size or filling every open block with pink ground cover. Maintain contrast between groves and clearings.

### 3.8 Hill of the Nameless

**Visual target:** secluded, mournful hillside with poisonous-lily associations.

- Terrain: rolling hills and shallow hollows; avoid cliffs so steep that the biome no longer reads as a hill.
- Surface: grass, coarse dirt, exposed stone on slopes.
- Vegetation: sparse trees, pale/purple lilies-of-the-valley-like flowers, small white flowers, tall grass.
- Features: flower clusters near hollows, isolated stones, abandoned trail fragments, occasional small pools.
- Atmosphere: quiet, desaturated, unsettling; any poison effect should be a deliberate gameplay feature, not an automatic consequence of merely touching ordinary flowers.
- Avoid: confusing it with Muenzuka. Hill of the Nameless should read as a flowered hillside; Muenzuka should read as a graveyard/wooded burial place.

### 3.9 Untrodden Valley

**Visual target:** rugged, secluded valley with streams and natural rock walls.

- Terrain accents: valley floor, steep banks, cliffs and rocky spurs supplied primarily by terrain shaping rather than block-by-block features.
- Surface: grass and dirt on the floor, stone/gravel on steep slopes, exposed rock outcrops.
- Vegetation: conifers or hardy mountain trees, ferns, grasses, moss near water.
- Features: stream bends, waterfalls where height and fluid rules support them, boulders, fallen logs, small caves.
- Structures: kappa/industrial hints only as rare points of interest if consistent with the mod's lore.
- Avoid: placing waterfalls without a reliable source block or creating many floating water features.

### 3.10 Genbu Ravine

**Visual target:** a deep ravine/wetland with water, rock and a strong sense of a hidden ecosystem.

- Terrain: ravine walls and low wet areas; use terrain noise and aquifer/fluid settings for the large forms.
- Surface: stone and gravel on walls, mud/clay/silt on wet banks, grass and moss on ledges.
- Vegetation: reeds, ferns, moss, small wetland trees, hanging vines, aquatic plants.
- Features: pools, trickling streams, small waterfalls, boulders, ledge vegetation, occasional giant-toad-pond-like clearing.
- Palette: cool greens and dark stone. Keep enough dry ledges for traversal.
- Avoid: decorating vertical cliff faces with features that require a full grass block unless the feature's placement predicate supports it.

### 3.11 Youkai Mountain Foothills

**Visual target:** the lower slopes of a great youkai mountain: forested, steepening terrain with streams.

- Terrain: rolling foothills that transition toward larger mountains; exposed stone increases with slope and elevation.
- Surface: grass and dirt on gentle slopes, gravel/stone on steep slopes, coarse dirt around trails.
- Vegetation: mountain trees, shrubs, ferns, grasses, occasional autumn-colored trees.
- Features: stream channels, boulder fields, small caves, cliffside vegetation and trail switchbacks.
- Structures: tiny tengu/kappa outposts can be rare; major shrines should use dedicated structures and proper spacing.
- Avoid: making the foothills indistinguishable from Untrodden Valley. This biome should feel like the inhabited approach to a mountain, while Untrodden Valley feels secluded and enclosed.

### 3.12 Sanctuary

**Visual target:** a calm, elevated, deliberately composed sacred landscape. The exact Touhou referent is unspecified, so keep this biome generic until lore requirements identify the intended site.

- Terrain: stable terraces, gentle ridges, occasional natural stone platforms.
- Surface: clean grass, moss, pale stone, and small gravel paths.
- Vegetation: carefully spaced trees, sacred-looking groves, white flowers, subtle luminous plants.
- Features: springs, stone arches, standing stones, small shrine-like landmarks, rare large landmark structure.
- Palette: restrained, bright, and less chaotic than Forest of Magic.
- Avoid: inventing a specific canon building or faction affiliation without project confirmation.

## 4. Feature design guide: underground biomes

Underground biomes need more than surface vegetation. Combine cave shape, ceiling/floor materials, fluids, particles, ambient lighting, and biome-specific placed features. Ensure each feature is valid on cave floors, walls, or ceilings as appropriate.

### 4.1 Hell of Blood Pools

**Visual target:** an oppressive blood-red basin or subterranean pool system.

- Cave shape: broad chambers and basins connected by narrow passages.
- Blocks: dark stone, red-toned rock, iron-rich-looking deposits, custom blood-fluid blocks if the mod supports safe fluid behavior.
- Features: red crystal clusters, bone piles, dark stalagmites, cracked banks, sparse dead roots.
- Fluid: use a custom fluid only if its physics, bucket behavior, rendering, and worldgen are implemented. Otherwise use a clearly recognizable red block palette rather than pretending lava is blood.
- Atmosphere: red fog/particles and ominous ambient audio; tune visibility for combat and navigation.
- Avoid: flooding all traversable caves. Keep dry routes and make fluid hazards legible.

### 4.2 Animal Realm

**Visual target:** a hostile, competitive spirit-world shaped by animal factions.

- Cave shape: broad caverns, territorial pockets, tunnels, and occasional arena-like spaces.
- Blocks: dark earth/stone, bone-like or fossil accents, faction-colored mineral/vegetation accents.
- Features: dens, burrows, skeletal remains, claw-marked stone, fungal patches, small territorial shrines.
- Structures: faction outposts or arena rooms should be rare, with structure spacing and spawn rules that prevent overlaps.
- Mobs: use biome spawn settings appropriate to the mod's spirit/faction ecosystem; don't rely on surface plants to convey the biome identity.
- Avoid: making it look like a generic red lava cave. Its identity should come from territorial ecology and spirit-world architecture.

### 4.3 Hell of Blazing Fires

**Visual target:** active, extremely hot subterranean region associated with intense fire and volcanic energy.

- Cave shape: large vertical chambers, vents, lava channels, narrow bridges and protected ledges.
- Blocks: dark basalt-like stone, scorched rock, magma, ash, occasional bright mineral veins.
- Features: lavafalls only where fluid placement is safe, smoke/ember particles, fire vents, basalt pillars, scorched roots.
- Lighting: use emissive blocks and ambient particles sparingly to keep silhouettes readable.
- Gameplay: heat damage or fire hazards are optional and should be communicated clearly.
- Avoid: indiscriminate lava lakes that make exploration impossible. Provide safe routes and pockets of solid ground.

### 4.4 Former Hell

**Visual target:** a once-hellish region now occupied by an unruly underground settlement and older volcanic remains.

- Cave shape: connected caverns, broad settlement chambers, old industrial/volcanic corridors.
- Blocks: dark stone and old masonry, weathered bricks, cracked paving, patches of ash and cooled lava.
- Features: abandoned furnaces, ruined bridges, old mining traces, hot springs, scattered rubble.
- Structures: an Old Capital-inspired settlement should be generated through dedicated structure templates/sets, with terrain adaptation and strict spacing.
- Atmosphere: warmer colors and occasional steam, but less uniformly hostile than Hell of Blazing Fires.
- Avoid: making every cave a ruin. Keep natural caves between settlement landmarks.

## 5. Surface rules: recommended architecture

### 5.1 Core principle

Use one ordered surface-rule tree, assembled in Java, with this priority:

1. **Hard exclusions / special cases:** bedrock or dimension-specific bottom rules if the dimension needs them.
2. **Underground-region surface materials:** only if the underground region is actually part of the same terrain column and surface-rule context. Cave biome surfaces may need cave-aware predicates.
3. **Dimension-wide defaults:** the common topsoil and subsurface materials for Gensokyo.
4. **Vertical-band overrides:** plains, foothills, and mountain foothills, if Y-based material differences are intentional.
5. **Biome-specific overrides:** only for biomes whose surface material genuinely differs (e.g. Muenzuka podzol patches, Genbu Ravine mud, Hell of Blazing Fires ash/basalt).
6. **Fallback:** leave vanilla/custom base stone or use the dimension's standard surface material.

SurfaceRules is an ordered decision tree: earlier matching branches win. Put narrow/special conditions before broad defaults. A broad “replace surface with grass” rule near the top can swallow every later biome rule.

### 5.2 Important distinction: Y bands versus terrain bands

The listed Y intervals are design bands, but surface rules operate on the terrain-generation context and may evaluate Y, stone depth, water, slope, biome, and noise conditions. If biome selection already assigns the intended biome based on climate and altitude, do not duplicate the full biome-selection algorithm in surface rules.

Use Y tests for genuinely vertical material behavior (e.g. ash below a threshold), not as a substitute for biome selection. If the terrain surface moves across Y=80 or Y=96, a hard Y split can create visible horizontal strata. Prefer biome tests for a biome's surface palette and use altitude tests only when the material boundary should truly follow world height.

### 5.3 Dimension-wide base surface

Create a helper that represents the common Gensokyo surface:

- On the terrain floor: grass block, unless underwater or overridden.
- A few blocks below: dirt.
- Deeper: the configured base stone.
- Underwater floor: gravel/sand/clay according to water and local biome conditions.
- Steep exposed surfaces: stone/gravel rather than grass where appropriate.
- Caves: do not blindly paint every cave floor with grass. Use the engine's floor/stone-depth conditions and cave-aware checks.

Conceptual Java sketch (names are illustrative; check the exact method signatures in the project's 1.21.1 mappings):

```java
private static SurfaceRules.RuleSource gensokyoBaseSurface() {
    return SurfaceRules.sequence(
        // Specific exceptions belong before these general rules.
        SurfaceRules.ifTrue(
            SurfaceRules.ON_FLOOR,
            SurfaceRules.ifTrue(
                SurfaceRules.waterBlockCheck(0, 0),
                SurfaceRules.state(Blocks.GRAVEL.defaultBlockState())
            )
        ),
        SurfaceRules.ifTrue(
            SurfaceRules.ON_FLOOR,
            SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())
        ),
        SurfaceRules.ifTrue(
            SurfaceRules.UNDER_FLOOR,
            SurfaceRules.state(Blocks.DIRT.defaultBlockState())
        )
    );
}
```

This is pseudocode for the architecture, not a guaranteed complete surface-rule implementation. In particular, a water check and floor condition may need to be combined differently for the desired underwater behavior, and a robust implementation should use stone-depth checks rather than assuming every `ON_FLOOR` context is the desired top layer. Compile against the exact NeoForge 1.21.1 API and inspect vanilla overworld surface-rule patterns before adopting the final predicates.

### 5.4 Biome override layer

Wrap only the exceptions around the shared base rule:

```java
private static SurfaceRules.RuleSource gensokyoSurfaceRules() {
    return SurfaceRules.sequence(
        // 1. Rare/specific biome surfaces first.
        SurfaceRules.ifTrue(
            SurfaceRules.isBiome(ModBiomes.MUENZUKA),
            muenzukaSurface()
        ),
        SurfaceRules.ifTrue(
            SurfaceRules.isBiome(ModBiomes.GENBU_RAVINE),
            genbuRavineSurface()
        ),
        SurfaceRules.ifTrue(
            SurfaceRules.isBiome(ModBiomes.HELL_OF_BLAZING_FIRES),
            blazingFiresSurface()
        ),

        // 2. Other targeted overrides can be grouped by shared palette.
        SurfaceRules.ifTrue(
            SurfaceRules.isBiome(
                ModBiomes.FOREST_OF_MAGIC,
                ModBiomes.BAMBOO_FOREST_OF_THE_LOST,
                ModBiomes.SAKURA_FOREST
            ),
            woodlandSurface()
        ),

        // 3. Always retain a base fallback.
        gensokyoBaseSurface()
    );
}
```

**Implementation caution:** a `SurfaceRules.sequence` branch that returns a surface state for a condition may prevent the later fallback from being considered for that position. Each override should therefore be complete for the positions it claims, or be nested so it only replaces the top/subsurface layers and intentionally delegates the rest. Do not simply return a single grass/dirt rule for an entire biome and assume deeper stone will be handled automatically.

A good pattern is a helper that takes top block, under block, and optional deeper block, then emits a complete ordered surface palette using `ON_FLOOR`, `UNDER_FLOOR`, `DEEP_UNDER_FLOOR`, `waterBlockCheck`, `stoneDepthCheck`, and `steep` where appropriate. Keep helper logic centralized.

### 5.5 Shared palette groups

To minimize duplication, define palettes instead of 16 unrelated surface functions:

| Palette | Suggested biomes | Top / near-surface material |
|---|---|---|
| Open grassland | Sunflower Plains, Human Village, Sanctuary | grass, dirt; paths and fields via features |
| Woodland | Forest of Magic, Bamboo Forest of the Lost, Sakura Forest | grass/podzol/rooted dirt; biome identity mainly from vegetation |
| Quiet/neglected ground | Muenzuka, Hill of the Nameless | grass/coarse dirt/podzol, scattered stones |
| Dry / weathered | Myouren Temple Barrens | coarse dirt, gravel, exposed stone |
| Mountain / ravine | Untrodden Valley, Genbu Ravine, Youkai Mountain Foothills | grass/dirt on gentle ground; stone/gravel on slopes; mud/clay in wet areas |
| Volcanic | Hell of Blazing Fires, Former Hell | ash, basalt-like stone, scorched stone; hot areas only |
| Blood pools | Hell of Blood Pools | dark/red stone and red-fluid banks |
| Spirit realm | Animal Realm | dark earth/stone, faction accents mostly via features |

Do not force every biome to have a unique top block. Many biomes should differ primarily through placed features, colors, climate, and structure distribution.

### 5.6 Underwater, cliffs, and caves

- **Underwater:** use a water-level-aware condition. A biome's normal grass rule should not paint underwater floors unless that is the intended look.
- **Cliffs:** use slope/steep checks or stone-depth conditions to expose stone and gravel on steep terrain. Avoid hardcoding a single Y cutoff for all cliffs.
- **Caves:** a surface rule can affect cave surfaces as the terrain is generated, but surface/floor semantics matter. Use floor/ceiling and stone-depth conditions intentionally; test cave floors, cave ceilings, and open-air cliffs separately.
- **Underground biome boundaries:** biome selection inside caves does not necessarily imply the surface rule can infer “this is an underground region” from Y alone. Use biome conditions for cave material palettes where possible, and use Y only for true vertical stratification.
- **Fluids:** test aquifers, custom fluids, and surface rules together. Avoid creating unsupported fluid states or open fluid sources that produce cascading updates or undesirable cave flooding.

## 6. Efficiency: what should be dimension-level versus biome-level?

**Dimension-level first is the right default for shared terrain materials.** The objective is not to make one huge complicated rule; it is to avoid repeating identical branches and to keep the rule tree understandable.

Recommended split:

- **Dimension-wide surface rule:** common topsoil, dirt depth, base stone assumptions, generic underwater floor handling, shared steep-slope handling.
- **Band-level rule:** only if plains/foothills/mountain foothills intentionally have different material strata independent of biome.
- **Biome-level surface override:** distinctive mud, ash, red stone, podzol, or other material palette that is integral to the biome.
- **Biome decoration / placed features:** trees, flowers, bamboo, mushrooms, boulders, ruins, ponds, reeds, structures.
- **Biome definition:** precipitation, temperature, downfall, grass/foliage/water colors, ambient particles/sounds, mob spawns.
- **Noise/terrain settings:** height distribution, slopes, ravines, cave shapes, aquifers, and the climate-to-biome mapping.

Do not build a separate full copy of the entire surface rule for every biome. Also do not put all 16 biomes into one generic branch if they require visibly different materials. Use shared helper methods and named palette groups.

## 7. Biome features implementation workflow

For each biome, implement features in this order:

1. **Surface palette:** validate topsoil and subsurface blocks with the surface-rule system.
2. **Common vegetation:** trees, grass, ferns, flowers, bamboo, mushrooms. Use placed features with correct placement modifiers.
3. **Ground accents:** boulders, fallen logs, roots, leaf litter, small rocks.
4. **Water and microterrain features:** ponds, springs, reeds, small falls. Prefer terrain/noise for major river and ravine geometry.
5. **Structures:** use structure templates/sets for houses, temples, ruins, settlements, and large landmarks. Configure spacing/separation and biome validity.
6. **Atmosphere and gameplay:** colors, particles, sounds, spawn lists, special hazards.
7. **Balancing:** tune feature count and footprint in a test world. A feature can look good alone but become oppressive when several placed features overlap.

Prefer reusing existing vanilla placed features when they meet the need. For custom features, separate the **ConfiguredFeature** (what is generated) from the **PlacedFeature** (where/how often it generates). Use biome modifiers or the project's existing biome bootstrap approach consistently; do not register the same feature through two independent paths.

## 8. Generation and debugging checklist

- [ ] Confirm the intended biome is selected at the expected climate and Y band.
- [ ] Check that the eight climate slices do not create abrupt, repetitive biome seams.
- [ ] Verify that each of the three multi-biome slices has correct weighting/selection and is not unintentionally overridden by a later condition.
- [ ] Test surface rules on a flat area, steep slope, underwater floor, open cliff, cave floor, and cave ceiling.
- [ ] Confirm that special surface rules are ordered before broad fallback rules.
- [ ] Confirm biome overrides preserve the intended dirt/stone layers.
- [ ] Inspect chunk borders for surface seams and mismatched materials.
- [ ] Verify features survive on their target blocks and do not generate floating trees or buried landmarks.
- [ ] Check that large features/structures have suitable spacing and do not overlap excessively.
- [ ] Test underground fluids, lava, particles, mob spawns, and safe traversal.
- [ ] Keep feature frequency moderate at first; increase only after inspecting several hundred chunks.
- [ ] Test fresh chunks and already-generated chunks separately. Worldgen changes generally do not retroactively regenerate old terrain.

## 9. Reference links

Use these as naming/API references; project-specific implementation should still follow the actual codebase.

- Touhou Wiki — [Human Village](https://en.touhouwiki.net/wiki/Human_Village)
- Touhou Wiki — [Myouren Temple](https://en.touhouwiki.net/wiki/Myouren_Temple)
- Touhou Wiki — [Hill of the Nameless](https://en.touhouwiki.net/wiki/Perfect_Memento_in_Strict_Sense/Hill_of_the_Nameless)
- Touhou Wiki — [Youkai Mountain](https://en.touhouwiki.net/wiki/Youkai_Mountain), including the Untrodden Valley and mountain foothills
- Touhou Patch Center glossary — [English location terminology](https://www.thpatch.net/wiki/Portal%3AEo/Vortaro)
- NeoForge 1.21.1 — [Biome Modifiers](https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/)
- Minecraft 1.21.1 mappings — [SurfaceRules API reference](https://lexxie.dev/neoforge/1.21.1/net/minecraft/world/level/levelgen/SurfaceRules.html)

## 10. Coding-agent instructions

Before changing code:

1. Inspect the repository's existing biome registry, dimension/level-stem setup, noise settings, surface-rule entry point, feature bootstrap, biome modifiers, and structure registration.
2. Preserve existing registry IDs and established architecture unless there is a concrete reason to migrate.
3. Identify whether surface rules are currently assembled in Java or data-driven. Follow that path rather than introducing a second competing mechanism.
4. Implement shared surface helpers first, then add only necessary palette overrides.
5. Implement features in small groups and validate in-game after each group.
6. Do not invent block IDs or APIs. Search the repository for existing blocks/features; if a proposed custom block does not exist, document it as a dependency or substitute an existing block.
7. Deliver a concise summary of changed files, registry IDs, assumptions, and testing performed.

**Expected result:** a coherent Gensokyo dimension where biome identity comes mainly from terrain context, surface palette, vegetation and landmarks, while shared dimension-level rules reduce duplication and make future biome additions easier.
