package dev.xkmc.gensokyolegacy.content.block.pot.stage;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

import java.util.List;

@SerialClass
public class PotionStageRecipe extends AlchemyStageRecipe<PotionStageRecipe> {

	public PotionStageRecipe() {
		super(GLRecipes.ALCHEMY_STAGE_POTION.get());
	}

	@Override
	public int getPriority() {
		return 12;
	}

	@Override
	public void removeConsumed(List<ItemStack> list) {
		for (int i = 0; i < list.size(); i++) {
			ItemStack stack = list.get(i);
			if (isPotion(stack)) {
				list.set(i, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public int getColor(AlchemyInv inv, HolderLookup.Provider access) {
		int rSum = 0, gSum = 0, bSum = 0, count = 0;
		for (ItemStack stack : inv.list()) {
			if (stack.isEmpty()) continue;
			if (!isPotion(stack)) continue;
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			if (contents == null) continue;
			int col = getPotionColor(contents);
			if (col == -1) continue;
			rSum += (col >> 16) & 0xFF;
			gSum += (col >> 8) & 0xFF;
			bSum += col & 0xFF;
			count++;
		}
		if (count == 0) return -1;
		int r = rSum / count;
		int g = gSum / count;
		int b = bSum / count;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (!super.matches(inv, level)) return false;
		if (inv.list().isEmpty()) return false;
		for (ItemStack stack : inv.list()) {
			if (stack.isEmpty()) return false;
			if (!isPotion(stack)) return false;
		}
		return true;
	}

	public static boolean isPotion(ItemStack stack) {
		return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
	}

	private static int getPotionColor(PotionContents contents) {
		// PotionContents.getColor() exists in 1.21; fallback to first effect color if -1
		try {
			int c = contents.getColor();
			if (c != -1) return c;
		} catch (Exception ignored) {
		}
		// fallback: average of effect colors (if any)
		var effects = contents.getAllEffects();
		boolean empty = true;
		for (var ignored : effects) {
			empty = false;
			break;
		}
		if (empty) return -1;
		int r = 0, g = 0, b = 0, n = 0;
		for (var e : effects) {
			int col = e.getEffect().value().getColor();
			r += (col >> 16) & 0xFF;
			g += (col >> 8) & 0xFF;
			b += col & 0xFF;
			n++;
		}
		if (n == 0) return -1;
		return (r / n << 16) | (g / n << 8) | (b / n);
	}
}
