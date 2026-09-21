# template_export — 植被模板导出与生成（dev-only，不进 jar）

把创造存档里手工搭建的树 / 灌木 / 蘑菇导出为结构 NBT，供 `TemplateFeature` 在魔法森林中放置。
设计背景见 `doc/design/magical_forest_vegetation.md`。

依赖：Python 3.10+、`numpy`、`Pillow`（只有预览图需要）。脚本只**读**存档，不会写入存档目录。

## 用法

```bash
# 全量导出（存档路径取 manifest.json 的 "save"，也可用 --save 覆盖）
python export_templates.py
python export_templates.py --only tree/oak      # 只导出 id 含该子串的模板
python export_templates.py --save "D:/.minecraft/saves/xxx"

# 程序化生成巨橡变体 tree/oak_giant_2..4（固定种子，可复现）
python gen_giant_oak.py

# 任意 NBT 出等距预览图
python preview.py path/to/a.nbt path/to/b.nbt -o out.png

# 不进游戏预览植被布局（俯视图 + 郁闭度 / 间距统计），调密度用
python simulate_layout.py --seed 1 --size 384

# 分析一个已生成的世界：俯视图 + 去掉树冠后的林下图，郁闭度、林下植被覆盖率、悬空树根统计
python analyze_world.py --save "<存档目录>" --center 0 0 --radius 160
```

输出目录：`src/main/resources/data/gensokyolegacy/structure/magical_forest/{tree,bush,mushroom}/`。
gzip 头不带时间戳，内容不变时重新导出得到逐字节相同的文件，不会弄脏 git。

## 新增 / 修改模板

1. 在存档里搭好，记下包围盒。
2. 在 `manifest.json` 的 `templates` 里加一条：`id`、`palette`（该模板由哪些方块组成）、`box = [x0, z0, x1, z1]`、`y = [地表上方第一层, 顶层]`。
3. 跑 `export_templates.py`，看 `preview/exported.png`。

包围盒不必精确：导出器取的是盒内**连通域**（树允许 1 格间隙，以带上悬空的藤蔓和地毯），邻树探进来的枝叶会被丢弃；`palette` 之外的方块（`air`、`cave_air`、超平坦自带的草和花）一律不导出。

## 模板约定（运行时依赖这些约定，无需逐模板配置）

- `y = 0` 层是地表上方第一层，最低一层必须有原木 / 菌柄。
- **锚点 = 底面中心** `(size.x / 2, 0, size.z / 2)`，且该格一定是原木 / 菌柄。导出器取最低层原木质心最近的那块原木作锚点，并补空白把它对到中心。
- 锚点到任一水平边缘 ≤ 15 格（Feature 只能写 3×3 区块），超出时导出器报错。
- 叶片：按原版规则重算 `distance`；`≤ 6` 写 `persistent=false`，`= 7` 保持 `persistent=true`。
- 不含任何空气方块；方块顺序为"完整方块在前、附着物在后"，与原版结构方块保存的顺序一致。

## 文件

| 文件 | 作用 |
|---|---|
| `mcworld.py` | 只读的 NBT + Anvil 区域文件解析（1.18+ 区块格式） |
| `nbtio.py` | 结构模板模型、NBT 写出与读回 |
| `export_templates.py` | 导出器：连通域提取 → 锚点居中 → 叶片修正 → 写出 → 读回校验 |
| `gen_giant_oak.py` | 参照手工巨橡的造型规律程序化生成变体（单干 / 双腿拱 / 分叉双冠） |
| `preview.py` | 等距预览图 / 对比图 |
| `simulate_layout.py` | 布局模拟：逐位移植 `GridLayer.point`（已与 Java 对拍），按 `MagicalForestFeatures` 的规则盖上真实模板的树冠。**常量要和 Java 同步修改**；不模拟地形、结构和原版噪声本身 |
| `analyze_world.py` | 验证用：读取已生成世界的区域文件，出图并统计 |
| `manifest.json` | 模板清单 |
