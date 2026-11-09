package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import com.tterrag.registrate.builders.ItemBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.util.PotionData;
import dev.xkmc.l2library.base.L2Registrate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface HexBrewHandler {

	default boolean isThrowable() {
		return false;
	}

	/**
	 * The entity a doll's brew should credit: the owner. The thrower itself stays the doll so
	 * ownership-based friendly fire (danmaku, explosions) keeps working.
	 */
	@Nullable
	static Entity damageOwner(@Nullable Entity thrower) {
		return thrower instanceof OwnableEntity own && own.getOwner() != null ? own.getOwner() : thrower;
	}

	static boolean isAllied(@Nullable Entity a, @Nullable Entity b) {
		return a != null && b != null && (a.isAlliedTo(b) || b.isAlliedTo(a));
	}

	default void onHit(Level level, Vec3 pos, @Nullable Entity thrower, ItemStack stack) {

	}

	default boolean isDrinkable() {
		return false;
	}

	default void onDrink(LivingEntity user, ItemStack stack, Level level) {
	}

	default int getUseDuration(ItemStack stack) {
		return 32;
	}

	default UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	default boolean copiesPotion() {
		return false;
	}

	default PotionData defaultPotion() {
		return PotionData.EMPTY;
	}

	default Item.Properties modify(Item.Properties p) {
		return p.stacksTo(16);
	}


	default boolean potionTexture() {
		return false;
	}

	default void build(ItemBuilder<HexBrewBottleItem, L2Registrate> builder) {
		builder.tab(GLItems.TAB.key());
	}

}
