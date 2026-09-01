package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface HexBrewHandler {

	default boolean isThrowable() {
		return false;
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

	default List<DataComponentType<?>> getComponentsToCopy() {
		return List.of();
	}

	default Item.Properties modify(Item.Properties p) {
		return p.stacksTo(16);
	}

}
