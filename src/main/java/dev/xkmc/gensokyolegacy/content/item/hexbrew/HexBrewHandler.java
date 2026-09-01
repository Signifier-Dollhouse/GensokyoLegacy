package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import com.tterrag.registrate.builders.ItemBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
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

	default <T extends MutableDataComponentHolder> T withDefaultDataForDisplay(T stack) {
		return stack;
	}

	default boolean potionTexture() {
		return false;
	}

	default void build(ItemBuilder<HexBrewBottleItem, L2Registrate> builder) {
		builder.tab(GLItems.TAB.key());
	}

}
