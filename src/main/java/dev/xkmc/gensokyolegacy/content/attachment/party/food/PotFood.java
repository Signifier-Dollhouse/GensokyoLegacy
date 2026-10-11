package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.youkaishomecoming.content.block.food.BowlBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;

/**
 * A Youkai's Feasts bowl or pot: two or four servings counted <em>down</em>.
 *
 * <p>The food is found by permuting the block's id first — the mod's own naming — and only
 * then by reflecting {@code BowlBlock.food}, which is a fallback that breaks silently if the
 * field is ever renamed.
 *
 * @see doc/design/party/food_mods.md
 */
public class PotFood extends AbstractCountdownFood {

	private static final Field FOOD = foodField();

	@Override
	public boolean handles(Block block) {
		return block instanceof BowlBlock;
	}

	/** The bowl the pot fills: the mod's own item where it ships one, the block's hidden field otherwise. */
	@Override
	public ItemStack serving(BlockState state) {
		var block = state.getBlock();
		var permuted = Permutations.itemOf(block);
		if (permuted != null) return new ItemStack(permuted);
		if (FOOD == null || !(block instanceof BowlBlock bowl)) return ItemStack.EMPTY;
		try {
			var food = (ItemLike) FOOD.get(bowl);
			return food == null ? ItemStack.EMPTY : new ItemStack(food);
		} catch (IllegalAccessException | RuntimeException e) {
			return ItemStack.EMPTY;   // the field went away; this bowl serves nothing
		}
	}

	/** {@code BowlBlock.food}, located once. Null when it cannot be read. */
	private static Field foodField() {
		try {
			var field = BowlBlock.class.getDeclaredField("food");
			field.setAccessible(true);
			return field;
		} catch (NoSuchFieldException | RuntimeException e) {
			return null;
		}
	}
}