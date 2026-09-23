# 魔法森林植被生成方案 — 手工造景模板 × 分层确定性分布

> 状态：**M1–M4 已实现并通过编译与 `runData`**（分支 `magical_forest_vegetation`）；**尚未在游戏内验证**，见 §11。目标版本 NeoForge 1.21.1。
> 模板来源：存档 `东方建筑结构`，主世界，玩家坐标 `(-299, 1, -128)` 周围的展示平台。
> 工具：`tools/template_export/`（用法见其 README）。开放问题已于 2026-09-22 全部确认，见 §12。

## 0. 摘要

1. 存档里识别出 **9 个树/灌木模板 + 12 个蘑菇模板**（平台上）和西南角 3×3 蘑菇阵列 9 个，已全部导出为 32 个 NBT；另参照手工巨橡**程序化生成了 3 个巨橡变体**，合计 35 个模板。
2. 用**离线脚本**把它们从区域文件直接导出为结构 NBT（自动裁剪、去 `cave_air`、锚点居中、修正叶片 `persistent`），不需要在游戏里逐个摆结构方块。
3. 新增一个通用 **`TemplateFeature`**（原版 `FossilFeature` 的同类做法）：随机旋转/镜像放置 NBT，带地面/坡度/净空检查、根系下延、只填空气的混合规则。它是普通 Feature，所以树苗和蘑菇催熟也能复用。
4. 分布不再是"每区块 12 次随机抽签"，而是**按树冠层级分 5 层**：巨木 → 大树 → 中小树 → 灌木/蘑菇 → 地被。巨木与大树用**确定性抖动网格**放置（纯 `seed + 坐标` 函数，与区块生成顺序无关，和 `FlatCheckStructure` 同一思路），保证间距、跨层互斥；再用低频噪声把森林分成 **混交林 / 蓝杉老林 / 菌谷 / 林窗** 四种林相。
5. 地被改用 `MOTION_BLOCKING_NO_LEAVES` 高度图，让草、蕨、花、小蘑菇、落叶能生成在巨型树冠**下面**（否则树冠下会是秃地）。

---

## 1. 现状

### 1.1 模板清单

展示平台：`grass_block` 铺在 `Y=0`，范围 `x[-336,-262] z[-161,-95]`，模板从 `Y=1` 起。
俯视标注图：`magical_forest_vegetation/templates_overview.png`，等距总览：`magical_forest_vegetation/iso_platform.png`。

**树与灌木**

| # | 建议 ID | 世界包围盒 (x / z / y) | 尺寸 x×y×z | 主要方块 | 基部原木 | 备注 |
|---|---|---|---|---|---|---|
| T1 | `tree/oak_giant_1` | -316..-295 / -138..-115 / 1..23 | 22×23×24 | 橡树叶 1408、深色橡木(wood) 252、藤蔓 133、苔藓地毯 30、洞穴藤蔓 16、耳菇 12 | 28 块，板根铺开 11×10 | 树干净高 14，树冠 y16–22 |
| T2 | `tree/oak_large_1` | -323..-302 / -111..-94 / 1..18 | 22×18×18 | 橡树叶 1389、深色橡木 119、藤蔓 150 | 15 块 5×5 | |
| T3 | `tree/blue_fir_giant_1` | -286..-265 / -158..-138 / 1..19 | 22×19×21 | 蓝杉叶 921、蓝杉木 170、常青垂藤 55、苔藓地毯 14、耳菇 17 | 19 块 5×5 | 层云状平展树冠 |
| T4 | `tree/blue_fir_giant_2` | -295..-276 / -120..-100 / 1..19 | 20×19×21 | 蓝杉叶 834、蓝杉木 160 | 15 块 5×4 | |
| T5 | `tree/blue_fir_large_1` | -339..-321 / -159..-137 / 1..15 | 19×15×23 | 蓝杉叶 775、蓝杉木 91 | 13 块 4×4 | |
| T6 | `tree/blue_fir_large_2` | -318..-299 / -158..-140 / 1..15 | 20×15×19 | 蓝杉叶 658、蓝杉木 86 | 12 块 4×5 | |
| T7 | `tree/blue_fir_medium_1` | -334..-325 / -116..-103 / 1..9 | 10×9×14 | 蓝杉叶 244、蓝杉木 28 | 4 块 | |
| T8 | `tree/blue_fir_medium_2` | -335..-325 / -128..-119 / 1..11 | 11×11×10 | 蓝杉叶 193、蓝杉木 19 | 4 块 | |
| T9 | `bush/azalea_1..3` | -324..-317 / -124..-114 / 1..2 | 8×2×11 | 杜鹃叶 47、盛开杜鹃叶 13、橡木(wood) 3 | — | 实为 3 丛矮灌木，导出时按最近原木拆成 3 个 |

**蘑菇（平台上）**

| # | 建议 ID | 位置 (x / z) | 尺寸 | 形态 |
|---|---|---|---|---|
| M1 | `mushroom/ghost_fire_medium_1` | -293..-287 / -143..-137 | 7×6×7 | 柄高 4，圆盘伞盖 + 7×7 裙边 |
| M2 | `mushroom/demonic_miasma_medium_1` | -295..-291 / -135..-131 | 5×6×5 | 3 层穹顶 |
| M3 | `mushroom/dream_medium_1` | -292..-286 / -129..-123 | 7×6×7 | 同 M1 形 |
| M4 | `mushroom/ghost_fire_large_1` | -285..-279 / -136..-130 | 7×9×7 | 弯柄 + 根，19 块菌柄 |
| M5 | `mushroom/ghost_fire_large_2` | -275..-269 / -131..-125 | 7×8×7 | 三层塔状伞盖 |
| M6 | `mushroom/demonic_miasma_large_1` | -275..-266 / -121..-113 | 10×9×9 | 分叉双伞（菱形伞盖） |
| M7 / M11 / M12 | `mushroom/dream_small_1..3` | 见标注图 | 3×3~4×3 | 小型 |
| M8 | `mushroom/ghost_fire_small_1` | -274..-272 / -137..-135 | 3×3×3 | 小型 |
| M9 | `mushroom/demonic_miasma_small_1` | -279..-277 / -124..-122 | 3×2×3 | 小型平顶 |
| M10 | `mushroom/dream_small_4` | -277..-275 / -110..-108 | 3×2×3 | 梦蘑菇伞盖 + 红色菌柄（已确认是有意的混搭） |

**西南角 3×3 阵列（平台外，基于 `Y=0`，`x[-354,-339] z[-103,-84]`，已确认纳入）**

| # | ID | 尺寸 | 形态 |
|---|---|---|---|
| S1 / S2 / S3 | `mushroom/demonic_miasma_large_2`、`dream_large_1`、`ghost_fire_large_3` | 7×8×7、7×6×7、7×10×7 | 大型，**2×2 菌柄**，菌谷主景 |
| S4 / S5 / S6 | `mushroom/demonic_miasma_medium_2`、`dream_medium_2`、`ghost_fire_medium_2` | 5×5×5、5×4×5、5×7×5 | 中型（S6 与现有 `GhostFireMushroomFeature` 形状一致） |
| S7 / S8 / S9 | `mushroom/demonic_miasma_small_2`、`dream_small_5`、`ghost_fire_small_2` | 3×3~4×3 | 小型 |

**程序化生成的巨橡（`gen_giant_oak.py`，见 §4.1）**

| ID | 原型 | 尺寸 | 方块 | 远端叶片（保持 persistent） |
|---|---|---|---|---|
| `tree/oak_giant_2` | 单干板根，高冠 | 25×22×23 | 1789（叶 1333、木 275、藤蔓 156） | 28（2.1%） |
| `tree/oak_giant_3` | 双腿拱（T1 的构思，换一套几何） | 27×23×25 | 1914（叶 1341、木 364、藤蔓 154、洞穴藤蔓 27） | 9（0.7%） |
| `tree/oak_giant_4` | 树干分叉，高低两个树冠 | 19×22×27 | 1841（叶 1351、木 311、藤蔓 157） | 1 |

对比图：`tools/template_export/preview/generated_oaks.png`（等距）、`side_compare.png`（侧视轮廓）。

**从数据里读出的几个关键事实**

- 模板周围填充的是 **`cave_air`**（应是粘贴进来的）。若用结构方块保存再放置，`BlockIgnoreProcessor.STRUCTURE_AND_AIR` 只忽略 `air`，`cave_air` 会被当成实体方块放进世界，在地形和别的树冠上挖洞。→ 导出时必须剔除。
- 所有叶片都是 `persistent=true`，`distance` 值是正确的。按原版规则重算：**蓝杉 6 个模板 0% 叶片会腐烂**（最远 distance=6）；**两棵橡树各约 4%（51/55 片）distance ≥ 7**。
- 所有模板"锚点到边缘"的最大距离 ≤ 14 格 < 16 → 满足 Feature 只能写 3×3 区块的硬限制（见 §5.3）。
- 耳菇 `SideBushBlock` 已实现 `rotate/mirror`；藤蔓、巨型蘑菇方块、原木原版支持 → 模板可安全旋转/镜像，每个模板得到 8 个朝向变体。
- 平台 `Y=1` 层混有超平坦自带的草/花（`short_grass` 等），导出时按白名单过滤。

### 1.2 当前生成逻辑与问题

`GLFeatureGen.MAGICAL_FOREST_VEGETATION`：每区块 12 次 `RANDOM_SELECTOR`（顺序抽签，实际约 40% 程序化蓝杉、20% 深色橡树、20% 丛林灌木、20% 各类巨型蘑菇），`HEIGHTMAP_OCEAN_FLOOR`。

| 问题 | 原因 |
|---|---|
| 没有层次，树都差不多高 | 只有一层均匀随机 |
| 椒盐式混杂，没有"林相"变化 | 无空间相关的密度/种类控制 |
| 程序化蓝杉是云杉型尖塔，而手工蓝杉是层云状平冠 | 两者看起来像两个树种（处理方式见 §12 第 4 条） |
| 直接把 24 格宽的巨树塞进这套逻辑会：树干互相穿插、长在房子上、树冠下寸草不生 | 无间距保证；无结构避让；地被用的高度图会落在树冠顶上 |

---

## 2. 设计原则

1. **造景归造景，分布归分布**：形态完全来自手工模板，代码只负责"放在哪、怎么落地"。
2. **确定性**：大树的位置是 `(seed, 坐标)` 的纯函数，不读世界状态、不依赖区块生成顺序（沿用 `FlatCheckStructure`/`MultiSpreadPlacement` 的风格）。
3. **数据驱动**：模板列表、密度、噪声阈值全部进 datagen 的 JSON，调参不改 Java。
4. **可增量**：每个里程碑都能进游戏看效果；MVP 只需要 1 个 Feature + 1 个 Processor。
5. **给造景者留接口**：新增模板 = 往清单里加一行 + 重新跑导出脚本。

---

## 3. 总体架构

```
存档 region/*.mca
   │  tools/template_export/export_templates.py + manifest.json      （离线，dev-only）
   ▼
data/gensokyolegacy/structure/magical_forest/{tree,bush,mushroom}/*.nbt
   │
   ▼
TemplateFeature(config: 模板加权表 / processor_list / 地面与坡度 / 根系下延 / post_features)
   │          ▲
   │          └─ TemplateBlendProcessor（只填空气、支撑检查、根系下延）
   ▼
PlacedFeature = [JitteredGrid | Count] → [NoiseBand] → [GridExclusion] → [AvoidStructures] → 高度图(NO_LEAVES) → Biome
   ▼
GLBiomes.MAGICAL_FOREST：L1 巨木 → L2 大树 → L3 中小树/大蘑菇 → L4 灌木/小蘑菇 → L5 地被
```

---

## 4. 模板导出管线（离线脚本）

已实现，位于 `tools/template_export/`（不进 jar）：区域文件解析（NBT + Anvil + 调色板解包）、连通域提取、NBT 写出与读回校验、等距预览。

`manifest.json`（入库，造景者可编辑）每条：`id`、`palette`（该模板由哪些方块组成）、水平包围盒、`y` 范围。

导出规则：

1. **按连通域取块**而不是按包围盒取块 → 邻树探进包围盒的叶子不会混入（T1 的包围盒里就有 T4 的 2 片蓝杉叶和半个蘑菇）。
2. **剔除** `air / cave_air / void_air`，NBT 里不写 → 放置时天然"不覆盖"。
3. **白名单过滤**：原木/木、叶、藤蔓、洞穴藤蔓、常青垂藤、苔藓地毯、耳菇、蘑菇方块/菌柄。过滤掉平台上的杂草野花。
4. **锚点居中**：取最低层原木中离质心最近的那一块为锚点（保证锚点一定是原木——T1 是双腿拱，质心落在两腿之间的空气里），给模板补空白使锚点恰为底面中心 `(⌊sx/2⌋, 0, ⌊sz/2⌋)`。于是运行时**不需要任何逐模板配置**，锚点永远是底面中心。`y=0` 层 = 地表上方第一层。
5. **叶片**：重算 distance；`distance ≤ 6` 写 `persistent=false`（砍树后正常腐烂、掉树苗），`distance = 7` 保留 `persistent=true`。脚本输出报告（如"T1：51 片保持 persistent"），造景者可据此在树冠里补隐藏枝干消掉它们。
6. **校验**：锚点到任一水平边缘 > 15 格直接报错；输出每个模板的方块统计与等距预览图，方便 review。
7. `DataVersion = 3955`（1.21.1），gzip 头不带时间戳 → 重新导出逐字节一致，不弄脏 git。
8. 写出后立即读回校验，并用第三方解析器（nbtlib）核对过顶层布局与原版写出的 `marisa_house.nbt` 一致。

导出结果：32 个模板共 39 KB；最大水平伸展 14（`blue_fir_giant_2`，树干偏在一侧），其余 ≤ 13。

> 为什么不用结构方块手存：20+ 个模板 × 手动框选；会带上 `cave_air`；锚点无法自动对齐；造景者每改一次树都要重存。脚本是一条命令全量重导。

### 4.1 程序化巨橡 `gen_giant_oak.py`

手工巨橡只有 1 棵，重复感会最明显，因此参照 T1/T2 量出的造型规律生成变体（离线生成、产物是普通 NBT，运行时与手工模板无差别）：

- **树干**：逐层圆盘光栅化，半径按关键点分段收细（4×4 去角 → 3×3 → 2×2），中心随高度漂移，截面加角向噪声；基部 4–6 条短板根。
- **枝**：树干顶一圈 6–7 条 1 格粗的近水平主枝（长 6.5–9），每条带一根侧向小枝填补两枝之间的冠缘；上方 2 层再一圈短枝，加顶梢。
- **树冠**：按 T1 实测的逐层半径 `[8.8, 10.4, 10.5, 10.0, 9.0, 7.6, 5.6, 3.3]` 做扁穹顶；边缘角向噪声 + 随机侵蚀，约 10% 孔洞，主枝周围掏空（从树下抬头能看到枝干，这是手工树的特征），首层只留一圈破碎的裙边，探出树冠的木头补一簇叶。
- **附着物**：藤蔓贴在最低几层冠缘的侧面并以同一朝向下垂 1–6 格（占叶量约 10%，与 T1/T2 一致），冠底零星 `up` 藤蔓；裸露的木头顶面铺苔藓地毯；树干侧面挂耳菇（`facing` 背离树干）；悬空的木头下方挂洞穴藤蔓。朝向约定已用手工模板反向验证（藤蔓 303 个面、耳菇 20 个全部有支撑）。
- 三个原型：**单干** / **双腿拱** / **分叉双冠**。固定种子，可复现；想换造型改 `VARIANTS` 里的种子即可。

---

## 5. 运行时：`TemplateFeature`

### 5.1 配置（`TemplateFeatureConfig`，record + Codec）

```json
{
  "type": "gensokyolegacy:template",
  "config": {
    "templates": [
      { "data": "gensokyolegacy:magical_forest/tree/blue_fir_giant_1", "weight": 1 },
      { "data": "gensokyolegacy:magical_forest/tree/blue_fir_giant_2", "weight": 1 }
    ],
    "processors": "gensokyolegacy:magical_forest/tree",
    "ground": "#gensokyolegacy:template_ground",
    "footprint_radius": 4,
    "max_slope": 4,
    "y_offset": 0,
    "root_depth": 6,
    "post_features": [ "gensokyolegacy:magical_forest/floor_blue_fir" ]
  }
}
```

### 5.2 放置流程

1. **找地面**：从 origin 向下穿过空气/叶/原木/可替换植物扫描到第一个地面方块（≤ 24 格）。自己扫而不依赖某一张高度图：FINAL 高度图会落在树冠/枝干上，而 `*_WG` 高度图在邻区块已生成完毕后不再可靠（存盘时被丢弃，再取用会按含树的当前方块重算）。
2. **检查**（任一失败则放弃，返回 false）：
   - 锚点地面 ∈ `ground` 标签（草方块/泥土/灰化土/菌丝/苔藓/砂土/缠根泥土）；
   - 锚点 + `footprint_radius` 处 8 个采样点：无流体，高差 ≤ `max_slope`；
   - **树干净空**：树干柱体范围内没有已有的 `#logs` / 菌柄（防止树干互穿，MVP 阶段靠它兜底）；
   - **顶部净空**：树冠高度处若干采样点须为空气或叶片（悬崖/突出岩下不放）。
3. 随机取模板、`Rotation`（4）、`Mirror`（2）。起点 = `地面锚点 − StructureTemplate.transform(模板锚点, mirror, rotation, ZERO)`——镜像是绕原点、旋转绕 pivot，这样算能保证任何朝向下树干都落在 origin。
4. `StructurePlaceSettings`：`setKnownShape(true)`（模板状态已自洽，跳过逐块邻居更新，也避免上千片叶子各排一个计划刻）、`setIgnoreEntities(true)`、`LiquidSettings.IGNORE_WATERLOGGING`、processor 列表；在 `WorldGenRegion` 中按 `FossilFeature` 的做法 `setBoundingBox(本区块 ±16)`。
5. `template.placeInWorld(...)`。
6. 依次放置 `post_features`（以树干基部为 origin）：灰化土/砂土圆盘、落叶、小蘑菇群等，全部复用原版 Feature 类型。

### 5.3 硬约束：3×3 区块写入半径

FEATURES 阶段 `WorldGenRegion` 的写半径是 1 个区块，越界会报 `Detected setBlock in a far chunk` 并丢块。origin 在本区块内，故**锚点到模板任一水平边缘必须 ≤ 16 格**（建议 ≤ 14）。当前最大 14（`blue_fir_giant_2`），合规；以后做更大的树要注意这条线，超过就只能走 Structure。

### 5.4 `TemplateBlendProcessor`（注册到 `GLWorldGen.PROCESSORS`）

`processBlock` 按模板方块分类决定是否落块（返回 `null` = 跳过）：

| 模板方块 | 允许替换的世界方块 |
|---|---|
| 原木/木、菌柄 | 空气、可替换植物、叶、藤蔓，以及 `#dirt`（上坡侧的根自然埋进土里）；永不替换 `#features_cannot_replace` |
| 叶、蘑菇伞盖 | 仅空气/可替换植物/藤蔓（树冠交叠时原木赢、叶子不覆盖别人的原木） |
| 装饰（藤蔓、垂藤、洞穴藤蔓、苔藓地毯、耳菇） | 仅空气，且须通过支撑检查 |

`finalizeProcessing`（能拿到整棵树的方块表）：

- **根系下延**：对 `y=0` 层的每个原木/菌柄，向下复制同种方块直到碰到实心地面，最多 `root_depth` 格 → 下坡侧形成板根而不是悬空。
- **支撑检查**：苔藓地毯下方、耳菇背后若既不在本次方块表里也不是世界里的实心块，则丢弃。
- （M5 可选）**垂挂物修剪**：每列藤蔓/垂藤从底部随机截短 0–n 格，让同一模板每次垂挂长度不同。只从底部截，避免中间断开留下悬浮段。

---

## 6. 分布设计

### 6.1 五个层级

| 层 | 内容 | 放置方式 | 初始参数（需进游戏调） |
|---|---|---|---|
| L1 巨木 | T1、`oak_giant_2..4`、T3、T4（冠幅 20–24，高 19–23） | 抖动网格 | `cell=28, margin=5, chance=0.85` → 约 0.23 棵/区块，树干间距 ≥ 10，最近邻均值 ≈ 22 |
| L2 大树 | T2、T5、T6（冠幅 18–23，高 15–18） | 抖动网格 + 对 L1 互斥 | `cell=18, margin=3, chance=0.8`（老林 0.95），距 L1 网格点 ≥ 12 |
| L3 中树 | T7、T8 | `Count 3` + 对 L1/L2 互斥 | 距 L1 ≥ 9、L2 ≥ 8，落在大树冠缘与冠隙 |
| L4 灌木/蘑菇 | 杜鹃丛 ×3、中/小蘑菇模板 | `Count` / `Rarity` | 灌木 4/区块；距 L1 ≥ 5、L2 ≥ 4，可长在树冠**下方**（实测冠底：巨橡 +15，蓝杉 +7~10）；中蘑菇每 3 区块 1 个，小蘑菇簇每 2 区块 1 簇（菌谷内各 2/区块） |
| L5 地被 | 现有 disk ×4、草、蕨、花、小蘑菇 patch；新增落叶、苔藓地毯、菌丝(hyphae) | 见 §7 | |

混交林里橡 : 蓝杉 = 1 : 1（`RANDOM_SELECTOR` 在"橡树模板组"和"蓝杉模板组"之间各 50%；两个树种各自是一个 configured feature，这样树下的地表处理可以按树种区分）。

以上数值用 `tools/template_export/simulate_layout.py` 调出：混交林 / 老林郁闭度 ≈ 78%，菌谷 ≈ 44%。模拟器里的网格算法是 `GridLayer.point` 的逐位移植，已与 Java 实现对拍（3 个种子 × 3 层 × 49 格，441/441 一致）。改密度时两边的常量要一起改。

### 6.2 确定性抖动网格 `JitteredGridPlacement`

```java
// PlacementModifier；pos 为本区块最小角
for (每个与本区块相交的 cell (cx, cz)) {            // cell ≥ 16 时最多 4 个
    RandomSource r = cellRandom(level.getSeed(), salt, cx, cz);   // 纯函数
    float u = r.nextFloat();                        // 固定抽取顺序：u, x, z
    int x = cx * cell + margin + r.nextInt(cell - 2 * margin);
    int z = cz * cell + margin + r.nextInt(cell - 2 * margin);
    if (u < chance && 点落在本区块内) emit(x, pos.getY(), z);
}
```

性质：每个激活的 cell **恰好被一个区块发射一次**；相邻 cell 的点距 ≥ `2*margin`；与生成顺序无关；`u < chance` 的写法让不同 `chance` 的点集互相嵌套，方便不同林相共用一张网格、只改密度。

`GridExclusionFilter(layers[], minDistance)`：对候选点重算所引用网格在周围 cell 的点，太近则拒绝。全程不读世界。只看网格点是否激活、不看那棵树最终是否放成——放失败的地方自然成为小空地。

### 6.3 林相分区 `NoiseBandFilter`

datagen 注册两个 `worldgen/noise`（`magical_forest_type`：firstOctave −7、振幅 [1, 0.5]；`magical_forest_clearing`：firstOctave −5），Filter 里经 `ServerChunkCache.randomState().getOrCreateNoise(key)` 采样（随种子变化）。阈值按实测分位数取：用真实的 `NormalNoise` 采样 128 万点，标准差只有 0.29（比直觉小），据此定出 **菌谷 F < −0.28（≈17%）、老林 F ≥ 0.20（≈25%）、林窗 G ≥ 0.48（≈7%）**。下表是各林相的配置：

- **F**（低频）决定林相：

| F | 林相 | L1 | L2 | L3 | 蘑菇 | 地表 |
|---|---|---|---|---|---|---|
| < −0.28 | **菌谷** | chance 0.4，仅蓝杉 | 关 | 关 | 大蘑菇网格 `cell=12, chance=0.7`（6 个大型模板）；中 2、小蘑菇簇 2/区块 | 蘑菇下铺菌丝土 |
| −0.28 ~ 0.20 | **混交林**（默认） | 0.85，橡:杉 = 1:1 | 0.8，橡:杉 = 1:1 | 3 | 大蘑菇每 6 区块 1 个，中每 3 区块 1 个 | 橡树下：灰化土/砂土/缠根泥土/苔藓 + 苔藓地毯 |
| ≥ 0.20 | **蓝杉老林** | 0.85，仅蓝杉 | 0.95，仅蓝杉 | 3 | 同上 | 蓝杉下：灰化土为主 + 青杉落叶 |

- **G**（中频）：`G ≥ 0.48` 为**林窗**——关闭 L1/L2/L3，只留灌木、花、草。提供透光的空地和可通行性，也让密林更显密。


### 6.4 为什么不用别的方案

- **每棵树一个 Structure/Jigsaw**：structure start/reference 存储膨胀、污染 `/locate`、一个 structure set 每格只能出一个、无法表达"每区块若干棵"。
- **把手工树翻译成 TrunkPlacer/FoliagePlacer**：工作量大且丢失手工形态。程序化只保留给幼树填充。
- **整片"森林地块"模板（48×48 含多棵树）**：重复感强、无法贴合地形。

---

## 7. 地被与林下

**核心问题**：巨树放完后，`WORLD_SURFACE` / `MOTION_BLOCKING` / `OCEAN_FLOOR` 都落在树冠顶上（叶片阻挡运动），现有草/花/蘑菇 patch 会因"下方不是泥土"全部失败 → 一棵巨木下方约 380 格² 的秃地。

**解法**（已对照 1.21.1 字节码确认）：`CARVERS` 状态的 `heightmapsAfter` 已是 FINAL 四件套，所以 FEATURES 阶段 `MOTION_BLOCKING_NO_LEAVES` 会随 `setBlock` 实时更新，可用来"穿过树叶落到地面"。

1. 群系内 VEGETAL_DECORATION 顺序：L1 → L2 → L3 → L4 → L5。
2. L5 全部改用 `HeightmapPlacement.onHeightmap(MOTION_BLOCKING_NO_LEAVES)`：
   - 自有的 `MAGICAL_FOREST_GRASS / FLOWERS / MUSHROOMS` 直接改；
   - 原版的 `PATCH_LARGE_FERN / PATCH_GRASS_FOREST / PATCH_GRASS_TAIGA` 换成自有 PlacedFeature 包同一个原版 ConfiguredFeature——顺带去掉对原版 placed feature 顺序的依赖（消除 feature order cycle 隐患）。
3. 新增 `CanopyFilter(min_gap)`：`WORLD_SURFACE − MOTION_BLOCKING_NO_LEAVES ≥ min_gap` 视为"在树冠下"。用于：
   - 树冠下：青杉落叶（`cedar_fallen_leaves`，1–2 层）、苔藓地毯、蕨、小蘑菇加密；
   - 树冠外/林窗：星星花等花类加密、扫帚草。
4. 巨木/大树的 `post_features`：树干周围 r=3–6 的灰化土/砂土/缠根泥土圆盘；50% 概率伴生一簇小蘑菇模板；蓝杉下落叶 patch。
5. 菌谷：大/中蘑菇模板的 `post_features` 在菌柄周围铺菌丝土圆盘。`hyphae`（菌丝）方块会困住实体并关联感染效果，属于玩法决定，**没有**放进世界生成，留给作者定。

---

## 8. 与结构的协调

魔理沙的家 / 香霖堂在 `SURFACE_STRUCTURES` 先于植被生成。巨木冠幅 20–24、冠底 +7~15，而房子高 13–18 → 不处理的话树冠会灌进屋里（叶片会填满模板树冠体积内的所有空气，包括室内），树干会贴墙。

`AvoidStructuresFilter(structures: TagKey<Structure>, margin)`：在 `WorldGenRegion` 上用 `structureManager().forWorldGenRegion(region)` 取相关 start，候选点落在任一 piece 包围盒外扩 `margin` 内则拒绝。`margin`：L1 = 13，L2 = 11，L3 = 5，灌木/地被不限 → 房子周围自然形成一圈林间空地，外圈树冠可以探进空地上空但不压房顶。标签同时包含博丽神社（防止魔法森林边缘的巨木探进樱花林里的神社）。

---

## 9. 代码与文件清单

```
content/worldgen/feature/template/   TemplateFeature, TemplateFeatureConfig, TemplateBlendProcessor
content/worldgen/placement/          GridLayer, JitteredGridPlacement, GridExclusionFilter, NoiseBandFilter,
                                     AvoidStructuresFilter, CanopyFilter
init/registrate/GLWorldGen           feature "template"；processor "template_blend"；5 个 placement modifier type
init/data/biome/MagicalForestFeatures 全部 configured / placed feature、两个 noise、群系里的生成顺序
init/data/biome/GLFeatureGen         只保留 disk 与三种地被 patch 的 configured feature，并调用上面的类
init/data/biome/GLBiomes             MAGICAL_FOREST 改用 MagicalForestFeatures.addVegetation
init/data/GLTagGen                   block tag template_trunk（#logs + 四种菌柄）
content/worldgen/feature/TreeFeatures BLUE_FIR 压到 4–6 格高（只剩树苗使用，世界生成不再放置它）
resources/.../structure/magical_forest/**.nbt
tools/template_export/               导出器、巨橡生成器、预览、布局模拟器
```

实现上与设计稿的出入：

- `TemplateBlendProcessor` 由 Feature 内置添加（参数只有 `root_depth`），config 里的 `processors` 是可选的**额外**处理器，默认为空；因此不需要再注册 processor_list。
- 需避让的结构用 id 列表（`HolderSet.direct`）而不是 structure tag，省掉一个 tag provider。
- 橡树 / 蓝杉各自是一个 configured feature，再用 `RANDOM_SELECTOR` 混合（为了让树下地表按树种区分）。
- `TemplateFeature` 自己向下找地面；若 origin 落在地形里（patch 的随机偏移会这样），先向上爬出地表再找。

遵守 AGENTS.md：不写内联 FQN、每个新包带 `package-info.java`、`runData` 产物随代码一起提交。

**树苗/催熟复用（M5，已实现）**：`TemplateFeature` 是普通 ConfiguredFeature，可直接作为 `TreeGrower` 目标，非 worldgen 环境下不设 bounding box 即可。蓝杉树苗 1 棵长 `blue_fir_medium`、2×2 长 `blue_fir_large`（`TreeGrower` mega 槽位）；原版没有 3×3 槽位，9 棵长 `blue_fir_giant` 由 `BlueFirSaplingBlock` 在 fallback 到 grower 之前自行检查。蘑菇用 `WeightedMushroomBlock` 按 50% / 40% / 10% 长出同种小 / 中 / 大模板（`ghost_fire_small/medium/large` 等 9 个 growth-only configured feature，不带地表；混种的 worldgen 版不能复用，否则会串种）。

---

## 10. 风险与注意事项

| 风险 | 对策 |
|---|---|
| 越 3×3 区块写入 | 导出器硬校验锚点到边缘 ≤ 15；`setBoundingBox` 兜底裁剪 |
| 区块生成耗时 | 巨木 1100–1900 块/棵 × 约 0.2 棵/区块 + 大树 800–1700 块 × 约 0.4，量级与丛林/黑森林相当；`knownShape=true` 省掉邻居更新与叶片计划刻。M2 起用 spark 量一次 |
| 橡树 4% 远端叶片 | 导出时保持 persistent（砍树后会残留少量浮空叶）；或造景者补隐藏枝干 |
| 魔法森林在 erosion 2–3，坡地多 | `max_slope` + 根系下延；巨木放不下时该网格点留空，由 L3 补位。若拒绝率过高再放宽到 5 |
| 模板数量少导致重复感（巨橡只有 1 个） | 已用 3 个程序化巨橡补足（§4.1）；另有 8 向变体 + 垂挂物修剪 |
| feature order cycle | 新 PlacedFeature 只在本模组群系使用，且各群系内相对顺序一致 |
| 与将来 `multiplex_worldgen` 分支冲突 | Feature/Placement 都是独立新类，群系接线集中在 `GLBiomes`/`GLFeatureGen` 两处，易于 rebase |

---

## 11. 里程碑

| 阶段 | 内容 | 验收 |
|---|---|---|
| M0 ✅ | 存档解析、模板分割与清单、叶片腐烂分析、API/字节码可行性验证 | 本文档 + 标注图 |
| M1 ✅ | 导出脚本 + manifest + 32 个手工模板 NBT + 3 个程序化巨橡 + 预览图 | 读回校验通过；游戏内 `/place template` 逐个过目（待做） |
| M2 ✅ 代码 | `TemplateFeature` + `TemplateBlendProcessor`；群系接线 | 编译、`runData` 通过。**待游戏内验证**：单群系世界飞一圈；坡地无悬空根；日志无 far chunk 警告 |
| M3 ✅ 代码 | `JitteredGrid` + `GridExclusion` + `NoiseBand` + `AvoidStructures` | 网格算法已与 Python 移植对拍一致、布局模拟通过。**待游戏内验证**：房子周围有空地；四种林相可辨 |
| M4 ✅ 代码 | 地被重做（`NO_LEAVES` 高度图、`CanopyFilter`、落叶、`post_features`） | **待游戏内验证**：树冠下不秃 |
| M5 | 垂挂物修剪、树苗/催熟复用、补充模板、调参 | 用本次的渲染脚本对生成结果出俯视图，量化郁闭度（目标 75–85%）与巨木最近邻间距 |

---

### 代码评审

2026-09-22 对本分支全部代码做了一次可读性 / 可维护性评审，结论与修改清单见 `doc/pending/magical_forest_vegetation_review.md`。

### 游戏内验证清单

1. 超平坦创造世界里逐个放：`/place feature gensokyolegacy:magical_forest/oak_giant`（还有 `blue_fir_giant`、`oak_large`、`blue_fir_large`、`blue_fir_medium`、`azalea_bush`、`mushroom_large`、`mushroom_medium_template`、`mushroom_small`）。看：树干是否落在脚下那一格、8 个朝向下藤蔓和耳菇是否都贴着树、树下地表圆盘和地毯/落叶是否出现。
2. 在坡地上同样放一次：上坡侧的根应埋进土里，下坡侧的根应向下延伸到地面，没有悬空的原木。
3. 新建"单一生物群系"世界选 `gensokyolegacy:magical_forest`，旁观模式飞一圈：巨木间距、四种林相、林窗、树冠下有草/蕨/落叶；找一处魔理沙的家或香霖堂看周围是否留出空地。
4. 日志里搜 `Detected setBlock in a far chunk`（应为 0 条）和 `Missing vegetation template`（应为 0 条）。
5. 用 spark 或 `/neoforge tps` 看一下跑图时的区块生成耗时。

## 12. 已确认的决定（2026-09-22）

1. **西南角 3×3 蘑菇阵列（S1–S9）纳入。** 已导出；2×2 菌柄的大型版作菌谷主景，`dream_large_1` 补上了梦蘑菇的大型空缺。
2. **叶片策略保持现状**：`distance ≤ 6` 可腐烂，`= 7` 保持 `persistent`，不补隐藏枝干。手工橡树 T1/T2 各有 51/55 片保持 persistent。
3. **M10（梦蘑菇伞盖 + 红菌柄）是有意的**，按原样导出为 `mushroom/dream_small_4`。
4. **程序化蓝杉压到 4–6 格高只当幼树**（M2 时改 `TreeType.BLUE_FIR` 的 trunk 参数）；不再承担成树角色。
5. **巨橡变体由程序化生成补足**（§4.1），不再等手工补模板。倒木、树桩、枯立木等小景仍建议后续补充，走同一个 `TemplateFeature` 放在 L4。

6. **去掉原版深色橡树和原版丛林灌木（橡树叶树丛）**（进游戏看过后的决定）。每区块的放置次数不变（中树 3 次、灌木 2 次），原先分给它们的 35% / 40% 份额改由蓝杉中树模板、杜鹃丛模板承担，所以密度不变；魔法森林里现在不再有任何原版树。

7. **矮草加密、扫帚草减量**（进游戏看过后的决定）。按每区块的放置尝试次数（patch 数 × 32）计：矮草 ≈110 → ≈240，扫帚草 96 → 32，蕨菜（bracken）保持 32，蕨 ≈180 不变。改法：扫帚草 patch 的权重 6:2 → 1:1、每区块 4 → 2 个；原版矮草 patch 每区块 2 → 6 个。

8. **去掉程序化的云杉形蓝杉小树**（`TreeType.BLUE_FIR` 用 `SpruceFoliagePlacer`，是群系里最后一种程序化生成的树；用户称之为"云杉木"）。它原本是 L4 层每区块 2 次的幼树；为保持密度，杜鹃丛由每区块 2 次改为 4 次（幼树 3–5 格宽、杜鹃 5 格宽，占地相当）。该 configured feature 保留给蓝杉树苗生长用。

尚未决定：要不要让蓝杉树苗长成手工模板树（现在树苗仍长成 4–6 格的程序化小树，而世界里已经没有这种树）、蘑菇催熟用手工模板（M5）；原版红/棕巨型蘑菇目前仍在中型蘑菇里各占 8% 作点缀。中树层现在全是蓝杉（没有中型橡树模板），如需平衡树种，可以像巨橡那样程序化生成 2–3 个中型橡树。
