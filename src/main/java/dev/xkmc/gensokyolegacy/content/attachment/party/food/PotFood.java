package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.youkaishomecoming.content.block.food.BowlBlock;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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

	@Override
	public @Nullable FoodProperties food(BlockState state) {
		var block = state.getBlock();
		var permuted = Permutations.foodOf(block);
		if (permuted != null) return permuted;
		if (FOOD == null || !(block instanceof BowlBlock bowl)) return null;
		try {
			var food = (ItemLike) FOOD.get(bowl);
			return food == null ? null : Servings.of(new ItemStack(food));
		} catch (IllegalAccessException | RuntimeException e) {
			return null;   // the field went away; this bowl simply serves nothing
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