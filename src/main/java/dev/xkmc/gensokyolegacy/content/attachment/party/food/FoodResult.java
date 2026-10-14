package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Everything needed to know about one food, resolved once and reused.
 * <p>{@code serving} is shared and mutable — use {@link #servingCopy()} to hand it out. {@code appeal} is
 * per food id, not per serving; {@code nutrition} and {@code saturation} are for the whole serving and
 * may exceed one item's worth.
 *
 * @see "doc/design/party/food.md"
 */
public record FoodResult(ItemStack serving, ResourceLocation blockId, int serves,
		int nutrition, float saturation,
		int appeal,
		FoodConsume consume) {

	/** Deliberately not a real id, so a bug printing it is visibly a bug. */
	public static final ResourceLocation INVALID_ID =
			ResourceLocation.fromNamespaceAndPath("gensokyolegacy", "invalid_food");

	/** Everything unservable collapses to this, so callers only ask {@link #valid()}. */
	public static final FoodResult INVALID =
			new FoodResult(ItemStack.EMPTY, INVALID_ID, 0, 0, 0, 0, (level, pos, state) -> 0);

	/** The serving item's id where there is one, the block's otherwise. */
	public ResourceLocation id() {
		if (serving.isEmpty()) return blockId;
		return BuiltInRegistries.ITEM.getKey(serving.getItem());
	}

	public ItemStack servingCopy() {
		return serving.copy();
	}

	/** No hunger value, or poison or wither: not worth serving. */
	private static boolean servable(@Nullable FoodProperties food) {
		return food != null && food.nutrition() > 0 && !Servings.refuses(food);
	}

	public static FoodResult of(ItemStack serving, Block block, int serves,
			@Nullable FoodProperties food, FoodConsume consume) {
		return of(serving, block, serves, food,
				food == null ? 0 : PartyFoodValue.appealOf(food), consume);
	}

	/** As above, but with {@code appeal} stated — for a serving of more than one item. */
	public static FoodResult of(ItemStack serving, Block block, int serves,
			@Nullable FoodProperties food, int appeal, FoodConsume consume) {
		if (serves <= 0 || !servable(food)) return INVALID;
		return new FoodResult(serving, BuiltInRegistries.BLOCK.getKey(block), serves,
				food.nutrition(), food.saturation(), appeal, consume);
	}

	public boolean valid() {
		return serves > 0 && nutrition > 0;
	}

	/**
	 * Takes the state to act on rather than closing over it, so consuming a stale cached
	 * result cannot resurrect a block the world has moved on from.
	 */
	@FunctionalInterface
	public interface FoodConsume {
		/** @return servings remaining */
		int consume(Level level, BlockPos pos, BlockState state);
	}
}