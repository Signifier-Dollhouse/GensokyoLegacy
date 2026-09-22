# TODO — natural / decorative / furniture block consistency

Follow-up issues left open after the consistency pass
(blue fir → spruce planks, stripping, burn time, compostability, loot, tool tags, recipes).

## Loot

- [ ] `donation_box` still has `noLootTable()` + empty loot table. It is a
  two-half block (`DoubleBlockHorizontal`), so plain `dropSelf` would drop one
  item per half. Needs half-aware loot (drop only from the origin half, like
  vanilla doors / beds) before survival players can recover it.
- [ ] `blue_fir_leaves` loot only drops sticks on non-silk touch, never the
  sapling. Vanilla leaves drop saplings (with fortune scaling). Decide the
  intended sapling drop chance and add it to `TreeSet::genLeavesLoot`.

## Recipes

- [ ] `sturdy_teddy_bear` has no recipe. It is a rare gift/toy item — decide
  whether it should stay uncraftable (quest/loot only) or get a wool-based
  recipe.
- [ ] Furniture recipes in `GLRecipeGen::furniture` are invented placeholders.
  Review costs once survival progression is decided (especially plank counts
  for `large_table` / `large_chair`, and the paper cost of windows).
- [ ] `tatami` / `tatami_block` have no furnace fuel value. Decide: straw mats
  burn (like hay, which is not fuel in vanilla either) or stay non-fuel.

## Tool tags

- [ ] `tatami` / `tatami_block` have no tool tag (hand-breakable, carpet-like).
  Confirm this is intended vs. axe.
- [ ] `youkai_bed` variants inherit vanilla bed behavior (no tool tag).
  Confirm intended.
