# Fallen Leaves — petals out of the canopy, litter under the tree

> Status: implemented. Cherry, maple and locust trees shed their own petals or leaves, and the
> litter under the tree grows into a carpet of fallen leaves on the ground or on still water.

The vanilla cherry tree drops cherry petals out of its crown (`CherryLeavesBlock.animateTick` →
`ParticleTypes.CHERRY_LEAVES`). The mod's cherry, maple and locust trees do the same with their own
colours, and they also pile up the leaves they drop on the ground.

## 1. Pieces

| Class | File | Role |
|---|---|---|
| `SheddingLeavesBlock` | `content/block/nature/SheddingLeavesBlock.java` | The leaves of the cherry, the maple and the locust. Sheds a petal or a leaf under the crown, and lets a carpet of fallen leaves grow below. Extends the vanilla `LeavesBlock`, so the distance, the decay and the rain behave as usual. |
| `FallenLeavesBlock` | `content/block/nature/FallenLeavesBlock.java` | The carpet on the ground or on still water, shallow or dense. Owns the rules of §3. |
| `FallenLeavesItem` | `content/block/nature/FallenLeavesItem.java` | The item: places the carpet on the ground as usual, or on the surface of a still water source the way a lily pad does. |
| `FallenLeafKind` | `content/block/nature/FallenLeafKind.java` | The species of a carpet: maple, cherry, locust or cedar, and the block of the carpet of that species. |
| `FallingLeafParticle` | `content/particle/FallingLeafParticle.java` | The particle itself: the drift of the vanilla cherry petal, ten variants of the petal (eight for the locust). |
| `GLParticles` | `init/registrate/GLParticles.java` | One particle type per species. |

The kind of a block is read off its registry name, so `<id>_leaves` and `<id>_fallen_leaves` are the
same species without a second table to keep in sync: `orange_maple_leaves` and
`orange_maple_fallen_leaves` are both `ORANGE_MAPLE`.

## 2. The falling petals and leaves

`SheddingLeavesBlock.animateTick` spawns one particle below the leaf, the way the vanilla cherry
leaves do: every tick with a chance of one in ten, and only when the block below has no full top
face, so the petal falls through the crown instead of resting on the leaves under it. The locust
spawns one in twenty instead, half as many particles as the other trees.

| Tree | Particle | Textures |
|---|---|---|
| Pink cherry | `gensokyolegacy:pink_cherry_blossom` | `pink_cherry_blossom_01..10` |
| Purple cherry | `gensokyolegacy:purple_cherry_blossom` | `purple_cherry_blossom_01..10` |
| Orange maple | `gensokyolegacy:orange_maple_leaf` | `orange_maple_leaf_01..10` |
| Red maple | `gensokyolegacy:red_maple_leaf` | `red_maple_leaf_01..10` |
| Yellow maple | `gensokyolegacy:yellow_maple_leaf` | `yellow_maple_leaf_01..10` |
| Locust | `gensokyolegacy:locust_leaf` | `locust_leaf_01..08` |

The textures are the artwork of the species, cut down to 6×6 particles; they use the same palette as
the leaves of the tree and as the carpet of fallen leaves below it. The textures of a species are
variants of the same petal, not the frames of an animation: every particle picks one of them at random
and keeps it for its whole fall.

The butterfly danmaku does not reuse a canopy petal. `ButterflyTrailParticle` draws from the same pink
and purple blossom sprites through `gensokyolegacy:pink_butterfly_trail` and
`gensokyolegacy:purple_butterfly_trail`, but lives 10 ticks and shrinks to nothing, so the trail reads
as a wisp behind the bullet instead of a petal drifting down.

## 3. The carpet under the tree

Every leaf of a cherry, maple or locust tree has a chance per random tick to drop a leaf: one in ten
for the cherry and the maple, one in twenty for the locust, which litters half as fast.
The leaf falls straight down: the search goes down at most eight blocks, and stops at the first
block with a full top face — the ground the carpet lies on. Air, plants and the leaves of the tree
itself are passed through. A still water source is a surface too: the carpet floats on the top water
block, the way a lily pad does, and never sinks into the lake. Flowing water and any other block stop
the search, so no carpet forms on a stream or a waterfall.

A carpet is either **shallow** (`layers=2`, a thin layer of leaves lying on the ground) or **dense**
(`layers=1`, the leaves piled up on each other). A carpet that meets the ground is shallow, and it
grows denser as more leaves fall on it.

A carpet met on the way is not covered by a new one, it grows instead:

| The carpet below | Result |
|---|---|
| shallow, the same species | it grows denser |
| dense, the same species | nothing |
| shallow, another species | it is taken over by the species of the leaf, still shallow |
| dense, another species | it is taken over by the species of the leaf, still dense, with a chance of 30% |
| shallow, another maple | it becomes a mixed maple carpet, still shallow |
| dense, another maple | it becomes a mixed maple carpet, still dense |
| shallow, a mixed maple | it grows denser, still mixed |
| dense, a mixed maple | nothing |
| shallow, a carpet the locust leaf mixes with | it becomes a mixed carpet, still shallow |
| dense, a carpet the locust leaf mixes with | it becomes a mixed carpet, still dense |
| shallow, a locust carpet, leaf of any species | it becomes a mixed carpet, still shallow |
| dense, a locust carpet, leaf of any species | it becomes a mixed carpet, still dense |
| shallow, a mixed carpet | it grows denser, still mixed |
| dense, a mixed carpet | nothing |

Two maples of different colours share one carpet, because they are the same tree in three autumn
colours and grow side by side; the mixed carpet keeps the mix and never turns back into a single
colour. Two cherries do not mix: the pink cherry takes the carpet of the purple cherry over.

The locust mixes with every species: a leaf of it falling on any other carpet turns it into a mixed
carpet, and a leaf of any species falling on a locust carpet does the same. A mixed carpet keeps the
mix whatever falls on it after that.

A carpet also drifts on its own: each random tick a single-colour maple carpet has a 5% chance to
become a mixed maple carpet, and a locust carpet a 5% chance to become a mixed carpet. The layers
and the orientation stay as they were. A mixed carpet is already mixed and never changes back.

The carpet that a leaf falls on keeps its orientation, so the patch under the tree does not turn
around when one species takes it over.

## 4. Decisions

- **Every leaf random ticks, not only the outermost ones.** The vanilla leaves only tick when their
  distance to the log is at the maximum, so only the shell of the crown would drop leaves and the
  carpet would be a ring around the trunk. `SheddingLeavesBlock.isRandomlyTicking` returns true for
  every state, and the litter fills the whole area under the crown.
- **The search stops at the first solid ground or still water surface.** A carpet on a slab or on a
  wall would float, and the leaves of the tree itself are not ground either.
- **Only still water carries a carpet.** A source block is a surface, so a pond keeps the leaves;
  flowing water is not, so a stream or a waterfall washes them away instead of holding them. The item
  raycasts for a water source the way a lily pad does, so the player cannot place a carpet into
  flowing water either.
- **The carpet is placed on any ground with a full top face** — grass, dirt, stone, sand, planks.
  A plant standing there is broken, and the carpet takes its place.
- **The kind comes from the registry name.** A second table mapping blocks to species would have to
  be kept in sync with the registrations by hand.
- **A lone maple or locust carpet mixes itself.** A tree that stands on its own would still litter
  a single colour, but the maples and the locust are meant to grow side by side, so their carpets
  drift to the mixed variant rather than staying one species forever.
