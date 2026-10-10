package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.PlateBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.PlateRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Kaleidoscope Cookery plate or platter: a stack of one named food, up to five of it.
 *
 * <p>
 * <b>A plate looks like a container and is not one.</b> Its state records a bare count
 * and nothing about what is on it, so the obvious conclusion — that a plate of spring
 * rolls and a plate of century eggs are the same block, and so cannot be priced
 * separately — is wrong. The mod does not have one generic plate: {@code PlateRegistry}
 * gives each of the eleven a {@code PlateData} of its own, holding the list of items
 * that plate may carry, and {@code PlateBlock} is constructed from that list. So the
 * block state says <i>how many</i> and the registry says <i>of what</i>, and together
 * they answer the question exactly.
 *
 * <p>
 * <b>This one is an ordinary {@link AbstractCountdownFood},</b> and needs none of the
 * special handling {@link BiteFood} does. Its {@code servings} property runs
 * {@code 0..maxCount}, but the constructor registers the default state at
 * {@code maxCount}, not at zero — a freshly placed plate is <em>full</em>, and the count
 * falls as the food is taken. That is the "servings left" convention, the same one a
 * Farmer's Delight feast uses, so {@link AbstractCountdownFood#countsDown} reads it off
 * the block correctly and {@code serves} is the property value verbatim. Reading the
 * default as if it were a fresh-plate-at-zero would have inverted the whole thing.
 *
 * <p>
 * <b>{@code consume} is inherited, and that is deliberate.</b> Taking the last serving
 * steps the count to zero and <em>leaves the empty plate standing</em>, exactly as the
 * mod does — {@code PlateBlock} only removes itself when a player right-clicks an empty
 * plate to pick it up, which is a player gesture the party system has no business
 * imitating. An emptied plate is inert: {@code serves} is 0, so {@link FoodResult} is
 * {@link FoodResult#INVALID} and {@code PartyArea.scan} drops it from the count without
 * deleting anything.
 *
 * <p>
 * One plate, {@code berry_platter}, hands out <b>two</b> items per serving — sweet
 * berries and glow berries — and a serve has no single taste to price it by. It is
 * scored by the first of its serving items that carries food, which undervalues it by
 * roughly half; that is a deliberate approximation on one decorative platter rather
 * than a rule invented to handle it. (The plate is take-only in the mod anyway:
 * {@code canRefill} refuses any plate that is not holding exactly one item, so a
 * two-item platter can never be refilled by hand.)
 *
 * <p>
 * <b>Load order:</b> references {@code PlateBlock}, so it is only ever constructed when
 * Kaleidoscope Cookery is present.
 *
 * @see BiteFood for the sibling Cookery handler, which could not share the countdown base
 */
public class PlateFood extends AbstractCountdownFood {

	@Override
	public boolean handles(Block block) {
		return block instanceof PlateBlock;
	}

	/**
	 * The first of the plate's serving items that carries food.
	 * <p>
	 * This is the plate's whole answer in one method, and the reason it overrides
	 * {@code serving} rather than {@code food}: the value is read off the item, so naming
	 * the item <i>is</i> naming the value, and the two cannot drift apart. The loop
	 * rather than a getFirst, so a plate whose items are not all food still resolves;
	 * and so the two-item berry platter is both priced and handed over by its first item.
	 */
	@Override
	public ItemStack serving(BlockState state) {
		var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		var data = PlateRegistry.PLATE_DATA_MAP.get(id);
		if (data == null) return ItemStack.EMPTY;   // not a registered plate after all
		for (var serving : data.getServingItems()) {
			if (Servings.of(serving.get().getDefaultInstance()) == null) continue;
			return serving.get().getDefaultInstance();
		}
		return ItemStack.EMPTY;
	}
}
