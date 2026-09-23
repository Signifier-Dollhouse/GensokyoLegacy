# TODO — natural / decorative / furniture block consistency

Follow-up issues left open after the consistency pass
(blue fir → spruce planks, stripping, burn time, compostability, loot, tool tags, recipes).

## Loot

- [ ] `blue_fir_leaves` loot only drops sticks on non-silk touch, never the
  sapling. Vanilla leaves drop saplings (with fortune scaling). Decide the
  intended sapling drop chance and add it to `TreeSet::genLeavesLoot`.

## Tool tags

- [ ] `tatami` / `tatami_block` have no tool tag (hand-breakable, carpet-like).
  Confirm this is intended vs. axe.
- [ ] `youkai_bed` variants inherit vanilla bed behavior (no tool tag).
  Confirm intended.

## other
- huge mushroom surface
- mob quest fix
- sealing border render
