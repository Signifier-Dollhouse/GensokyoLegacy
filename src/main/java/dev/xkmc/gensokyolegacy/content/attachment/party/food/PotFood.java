package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.youkaishomecoming.content.block.food.BowlBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;

/**
 * A Youkai's Feasts bowl or pot: two or four servings counting <em>down</em>.
 *
 * <p>
 * {@code PotFoodBlock} extends {@code IntegerProperty.create("serve", 1, 2)} or
 * {@code create("serve", 1, 4)}, so a pot holds two or four servings and counts down —
 * the same direction as a feast and the opposite of a cake.
 *
 * <p>
 * The food is found by <b>permuting the block's id</b> first, which is how these mod's
 * own item forms are named, and only then by reading {@code BowlBlock.food}. That field
 * is {@code protected} with no accessor, so the reflective read is a last resort rather
 * than the primary route: it works today but breaks the day the field is renamed, and
 * reflection against another mod is the kind of thing that fails silently.
 *
 * <p>
 * <b>Load order:</b> references {@code BowlBlock}, so it is only ever constructed when
 * Youkai's Feasts is present.
 */
public class PotFood extends AbstractCountdownFood {

	private static final Field FOOD = foodField();

	@Override
	public boolean handles(Block block) {
		return block instanceof BowlBlock;
	}

	/**
	 * The bowl the pot fills: the mod's own item where it ships one, the block's hidden
	 * field otherwise.
	 * <p>
	 * The permutation first, for the same reason {@link CakeFood} tries it first — it is
	 * the mod's own public naming rather than a private field an update can rename — and
	 * the reflection is a fallback rather than the mechanism. A bowl whose field has gone
	 * serves nothing at all, which is the same answer it gave as food with no properties.
	 */
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