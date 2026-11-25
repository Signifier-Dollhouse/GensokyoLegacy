# Design docs

Every feature's rationale lives here. The code keeps a pointer (`@see "doc/design/..."`) and the long
tables, measurements and worked examples stay in the matching document.

| directory | covers |
|---|---|
| [item/](item/) | standalone items and blocks: tools, weapons, functional blocks, cooldowns and their UIs |
| [biome/](biome/) | the Gensokyo dimension, its climate grid, biomes, surface rules and worldgen features |
| [structure/](structure/) | character homes, beds, the interior graph, placement, terrain fit, visits and reputation |
| [doll/](doll/) | the doll pairing ledger, entities, loadout, control and the glove |
| [party/](party/) | party areas, food, guest selection and the guest state machine |
| [youkai/](youkai/) | the youkai entity facets and its client-side bubbles |
| `area_effect.md` | the chunk/level area-effect framework shared by the sealing pot and the treasure basket |

`doc/quest/` holds the dialog and quest content, and `doc/tools.md` documents the offline generators,
renderers and validators.
