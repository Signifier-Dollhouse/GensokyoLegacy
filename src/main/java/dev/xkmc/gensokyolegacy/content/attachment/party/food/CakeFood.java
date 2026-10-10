package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A cake: several slices, each with its own food values.
 *
 * <p>
 * Handled by {@code instanceof CakeBlock}, so Youkai's Feasts' {@code YHCakeBlock} and
 * {@code YHCandleCakeBlock} both land here with no registration of their own.
 *
 * <p>
 * A cake's food is the awkward case. Vanilla {@code CakeBlock} has no
 * {@link FoodProperties} anywhere: {@code CakeBlock.eat} feeds the player with
 * {@code foodData.eat(2, 0.1F)} per slice from code, and the block item is foodless. So
 * one slice's values are built here as a fallback.
 *
 * <p>
 * Before falling back, the block's own id is permuted to look for a slice item — the
 * convention Youkai's Feasts uses, so its cakes are read from their real slice rather
 * than from vanilla's numbers. That matters: with no other mods installed the fallback
 * is all there is, and a cake that scored at nothing would mean no character ever came
 * to a party, but when a slice item does exist it is authoritative.
 *
 * <p>
 * Vanilla cake is {@code IntegerProperty.create("bites", 0, 6)} counting <em>up</em>, so
 * a fresh cake sits at 0 and still has all seven slices; {@link AbstractCountdownFood}
 * handles that.
 *
 * <p>
 * {@code 0.4} is not arbitrary: {@code FoodConstants.saturationByModifier} is
 * {@code foodLevel * saturationModifier * 2.0}, so one slice of {@code eat(2, 0.1F)}
 * carries {@code 2 * 0.1 * 2.0 = 0.4} saturation.
 */
public class CakeFood extends AbstractCountdownFood {

	/** Nutrition and saturation of one slice, from {@code CakeBlock.eat}. */
	public static final float SLICE_NUTRITION = 2f;
	public static final float SLICE_SATURATION = 0.4f;

	/** Vanilla cake's own numbers, used when no slice item can be found. */
	private final FoodProperties fallback = new FoodProperties.Builder()
			.nutrition((int) SLICE_NUTRITION)
			.saturationModifier(SLICE_SATURATION / (SLICE_NUTRITION * 2f))
			.build();

	@Override
	public boolean handles(Block block) {
		return block instanceof CakeBlock;
	}

	@Override
	public @Nullable FoodProperties food(BlockState state) {
		var permuted = Permutations.foodOf(state.getBlock());
		return permuted == null ? fallback : permuted;
	}

	/**
	 * Named by the slice item when there is one, and by the cake otherwise.
	 * <p>
	 * The slice is what the guest is handed, so it is what the food is — and when a mod
	 * supplies a real slice the block's id would hide which one it was. Vanilla cake has
	 * no slice item, so it falls back to the block, which is the honest answer there.
	 */
	@Override
	public ResourceLocation id(BlockState state) {
		var slice = Permutations.itemOf(state.getBlock());
		return slice == null
				? BuiltInRegistries.BLOCK.getKey(state.getBlock())
				: BuiltInRegistries.ITEM.getKey(slice);
	}
}