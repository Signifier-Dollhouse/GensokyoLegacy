# 魔法森林植被代码评审（2026-09-22）

范围：分支 `magical_forest_vegetation` 相对 `main` 的全部改动——Java 14 个文件（新增约 900 行）、`tools/template_export/` 7 个 Python 文件（约 1500 行）、35 个 NBT 模板、datagen 输出。角度：正确性、可读性、可维护性、可测试性、性能。设计背景见 `doc/design/magical_forest_vegetation.md`。

标记：✅ 已在本次评审中修改；⏳ 建议，未改。条目按影响排序。

## 总评

- 分层清楚：造景（NBT，离线导出）→ 放置（`TemplateFeature` + `TemplateBlendProcessor`）→ 分布（6 个 `PlacementModifier`）→ 参数（`MagicalForestFeatures`），和设计文档逐段对应；每个类只做一件事，Javadoc 说明了"为什么"。
- 最大的风险不在代码本身，而是**至今没有在真实世界里生成过一次**：所有验证只到 `compileJava` / `runData` / `build` 和一个离线布局模拟器。
- 评审前的代码有 1 个潜在崩溃、1 个多余且有副作用的过滤器、若干可读性问题；都已修。剩下的建议集中在测试和常量的双份维护。

## 1. 正确性 / 健壮性

### 1.1 ✅ `AvoidStructuresFilter` 可能越过世界生成区域读区块 — HIGH

**位置：** `content/worldgen/placement/AvoidStructuresFilter.java`

原实现用 `StructureManager.startsForStructure(chunk, predicate)`，它会为每个引用去加载**结构起点所在区块**（`getChunk(..., STRUCTURE_STARTS)`）。1.21.1 的 `WorldGenRegion.getChunk` 只允许读到与正在生成的区块棋盘距离 ≤ 8 的区块（FEATURES 步骤的依赖半径），超出直接 `ReportedException` 崩溃。过滤器探测相邻区块（距离 1）的引用，而引用可以指向距该区块 8 格的起点，即距中心 9 格。

现有的三个结构包围盒最远只有 92 格（约 6 区块），触发不了；但 `structures` 是数据驱动的列表，谁往里加一个大结构就会偶发崩溃。

**修改：** 自己遍历 `ChunkAccess.getAllReferences()`，对每个起点先 `level.hasChunk()`（等价于"距离 ≤ 8"）再读；读不到的起点跳过（它的包围盒必须横跨 7 个区块才可能影响到这里）。探测的区块范围也改为"边距方块覆盖到的区块 ∩ 中心 3×3"，替代原来 9 个采样点 + 只能去重相邻重复的写法。Javadoc 写明了"必须在位置仍在本区块内时运行（任何随机偏移之前）"这个前提。

### 1.2 ✅ `tree()` 里的 `SurfaceWaterDepthFilter` 多余，且会误拒 — MEDIUM

**位置：** `init/data/biome/MagicalForestFeatures.tree`

该过滤器算的是 `WORLD_SURFACE − OCEAN_FLOOR`（非空气最高点 − 阻挡移动最高点）。藤蔓、耳菇、草都"非空气但不阻挡移动"，所以前一棵树垂下来的藤蔓所在的列会被当成"水深 > 0"而拒绝。而 `TemplateFeature` 自己已经检查锚点和 8 个采样点的流体（`findGround` 遇水停下、`groundAt` 遇水返回 `MIN_VALUE`），原版巨型蘑菇也检查脚下是否泥土。

**修改：** 从 `tree()` 移除，`tree()` 的 Javadoc 说明理由。地被 `cover()` 本来就没有它。

### 1.3 ⏳ 树冠下的地被在枝干和蘑菇伞盖正下方不会生成 — LOW

`MOTION_BLOCKING_NO_LEAVES` 只排除 `LeavesBlock`，原木、蘑菇伞盖仍算"阻挡"，所以这些列的高度图落在枝干/伞盖顶上，地被 patch 在那里找不到泥土。巨橡的水平枝占树冠投影约 8%，大蘑菇伞盖 5×5–7×7。观感影响小，先记录；若要补，可让 `CanopyFilter`/`FLOOR` 改用自定义"穿过 `template_trunk` 与蘑菇方块"的向下扫描（`TemplateFeature.findGround` 已有同样逻辑）。

### 1.4 ⏳ 没有自动化测试 — MEDIUM

唯一的自动检查是 `simulate_layout.py` 对 `GridLayer.point` 的对拍（441 格一致）。放置逻辑（锚点落在原点、8 个朝向下附着物有支撑、坡地根系延伸、结构避让）全靠人工。建议：

1. NeoForge GameTest：一个测试结构（平地 + 一段斜坡），对每个模板调用 `TemplateFeature` 8 次（4 旋转 × 2 镜像），断言 (a) 原点方块是 `#template_trunk`，(b) 每个藤蔓/耳菇/地毯都有支撑（复用 `TemplateBlendProcessor.isSupported` 的规则），(c) 没有悬空原木。
2. 至少按设计文档 §11 的 `/place feature` 清单人工过一遍。

## 2. 可读性

### 2.1 ✅ 生成顺序由静态字段的声明顺序隐式决定

`MagicalForestFeatures.pf()` 原来在创建 key 时顺手 `ORDER.add(key)`，于是**字段声明顺序 = 群系里的生成顺序**，调整一行声明就会悄悄改变世界生成。改为显式的 `ORDER = List.of(...)`，并加注释"树冠层自上而下，最后地被"。

### 2.2 ✅ `cover()` 依赖"第一个 modifier 必须是 count"的隐含约定

签名改为 `cover(ctx, key, feature, PlacementModifier count, PlacementModifier... filters)`。

### 2.3 ✅ 间距和避让距离是散落的魔法数字

`placed()` 里原有 12 / 9 / 8 / 7 / 5 / 4 和 13 / 11 / 6 / 4 / 3 / 2 十几个字面量。归纳为 4 个命名的 `GridExclusionFilter` 常量（`LARGE_SPACING`、`MEDIUM_SPACING`、`MUSHROOM_SPACING`、`UNDERSTORY_SPACING`）和 5 个 `*_CLEARANCE` 常量，并在注释里写明含义。调参时只改一处，也方便和 `simulate_layout.py` 对照。

### 2.4 ✅ `vegetation(4, 4, 6, ...)` 三个无名位置参数

引入 `record Footprint(radius, maxSlope, rootDepth)` 和 `GIANT_FOOTPRINT` 等命名常量。

### 2.5 ✅ `CanopyFilter` 一行里 `>=` 和 `==` 混用

`top - floor >= minGap == under` 能编译且正确（`>=` 先算），但要想一下。拆成两行。

### 2.6 ✅ `TemplateBlendProcessor.finalizeProcessing` 依赖两个 list 的下标对齐

`original.get(i)` 与 `processed.get(i)` 对应同一方块，这是原版 `processBlockInfos` 的构造方式，但没有写出来；而且本方法返回的 list 不再对齐（加了根、丢了附着物），所以它必须是第一个 processor。补了 Javadoc。

### 2.7 ✅ `gen_giant_oak.finish()` 85 行做 5 件事

拆成 `_tuft_bare_wood` / `_hang_vines` / `_moss_carpets` / `_cave_vines` / `_eugune`，随机数调用顺序不变，重跑后 3 个 NBT 逐字节一致（已验证）。

### 2.8 ⏳ `TemplateFeature.place()` 45 行

找地面 → 选模板 → 变换 → 放置 → post features，读起来仍是线性的，暂不拆。若以后加"树苗生长"入口，建议先拆出 `resolveBase()` 与 `placeTemplate()`。

### 2.9 ⏳ `simulate_layout.main()` 约 120 行

规划（网格 → 各层落点）和渲染（盖树冠 → 出图 → 统计）混在一起。是开发工具，先不动；要改建议拆成 `plan_layout()` / `render()` / `report()`。

## 3. 可维护性

### 3.1 ⏳ Java 与模拟器的参数是两份手工同步的常量

`MagicalForestFeatures` 与 `simulate_layout.py` 各自持有网格、间距、计数、林相比例，两处都有注释提醒同步，但没有机制保证。可选做法：datagen 已经把所有 placed feature 写成 JSON，模拟器直接读 `src/generated/.../placed_feature/magical_forest/*.json` 取 `jittered_grid` / `grid_exclusion` / `count` 参数，Python 里就只剩林相比例一处常量。

### 3.2 ⏳ 地被的 configured feature 分在两个类里

草 / 花 / 小蘑菇 patch 和四种圆盘的 configured feature 留在 `GLFeatureGen`（原有位置），它们的 placed feature 在 `MagicalForestFeatures`。当时为减少对原作者文件的改动而保留；如果 `MagicalForestFeatures` 已经是魔法森林植被的唯一入口，把这几个 CF 一并搬过去更顺。

### 3.3 ⏳ 结构 id 是字符串

`STRUCTURES = List.of("marisa_house", ...)` 和 `GLStructureGen` 里 `GensokyoLegacy.loc("marisa_house")` 各写一遍。`GLStructureGen` 没有公开的 `ResourceKey<Structure>` 常量；若加，建议两处共用。

### 3.4 ⏳ 方块分类散在两个类

`TemplateBlendProcessor.isSoft / isPassable` 被 `TemplateFeature` 借用；"什么算软方块、什么算树干、什么算树冠"的规则集中在 processor 里，但被 feature 依赖。可抽成一个小的 `TemplateBlocks` 工具类，processor 与 feature 都从那里取。

### 3.5 ⏳ 模板约定只在导出时校验

锚点 = 底面中心且必须是原木、水平伸展 ≤ 15、不含空气——这些由 `export_templates.validate()` 保证；手工放进 `structure/magical_forest/` 的 NBT 不会被检查，reach > 15 会在世界生成时报 `Detected setBlock in a far chunk` 并少块。建议 `TemplateFeature` 首次取到模板时校验一次尺寸并 `LOGGER.warn`（和现在的 `MISSING` 集合同一位置）。

### 3.6 ⏳ Python 工具的方块分类靠字符串启发式

`nbtio.NON_FULL`、`analyze_world.kind_of` 用子串匹配（`'grass'` 会命中 `grass_block`、`'flower'` 命中 `flowering_azalea_leaves`）。`NON_FULL` 只影响 NBT 里的方块顺序，无害；`kind_of` 影响统计口径。用于开发工具可以接受，但增加新方块时要留意。

### 3.7 ✅ 未使用的常量

`export_templates.NEIGH26`、`mcworld.AIR_NAMES` 已删除。

### 3.8 ⏳ 蓝杉树苗仍长成程序化小树

`TreeType.BLUE_FIR` 现在只被树苗用（世界生成已不放置它），长出的是 4–6 格的云杉形小树，而世界里已经没有这种树。属设计文档 M5：树苗改指向手工模板 feature（`TemplateFeature` 本身支持）。

## 4. 性能（未实测）

- 巨木一棵 ≈ 1100–1900 次 `getBlockState` + `placeInWorld`，`setKnownShape(true)` 免掉了邻居更新和每片叶子的计划刻；密度约 0.23 棵/区块。
- `GridExclusionFilter.isNear`：每个候选点扫描 `(2d / cell + 2)²` 个格子 × 层数，每格一次 Xoroshiro 初始化；每区块几十次调用，可忽略。
- `AvoidStructuresFilter`：每个候选点读 1–4 个区块的引用表，便宜。
- 建议进游戏后用 spark 量一次区块生成耗时，和原版黑森林 / 丛林比。

## 5. 文档与工具

- 设计文档、README、`simulate_layout` / `analyze_world` 的用法都有；本次把评审结论回链进设计文档 §11。
- `runData` 会顺手改写 `src/generated/.../trade/marisa/offer_hyphae.json`（末尾换行），与本分支无关，每次提交前需 `git checkout` 它——这是仓库原有问题，建议原作者统一一下该文件的换行。

## 本次评审的修改

| 文件 | 修改 |
|---|---|
| `AvoidStructuresFilter` | 重写探测逻辑，加 `hasChunk` 保护，探测范围钳在中心 3×3 |
| `MagicalForestFeatures` | 显式 `ORDER`；`cover()` 显式 `count` 参数；`*_SPACING` / `*_CLEARANCE` / `Footprint` 常量；`tree()` 去掉 `SurfaceWaterDepthFilter` |
| `CanopyFilter` | 拆开条件表达式 |
| `TemplateBlendProcessor` | `finalizeProcessing` 下标对齐前提写进 Javadoc |
| `TemplateFeature` | `checkClearance` Javadoc 补上"蘑菇伞盖"也算 |
| `gen_giant_oak.py` | `finish()` 拆成 5 个方法，输出逐字节一致 |
| `export_templates.py` / `mcworld.py` | 删未使用常量 |

生成数据的变化只有两处：所有 `tree()` 注册的 placed feature 少了一个 `surface_water_depth_filter`；其余 JSON 与评审前一致（`ORDER` 显式化后群系里的顺序未变）。
