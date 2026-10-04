package dev.xkmc.gensokyolegacy.content.item.character;

import dev.xkmc.gensokyolegacy.compat.curios.CuriosManager;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Strange glasses: worn in the head armor slot (right-click to equip).
 * While worn, sealing-pot borders render and every entity glows white (client side only).
 */
public class StrangeGlassesItem extends Item {

	public StrangeGlassesItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public @Nullable EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return EquipmentSlot.HEAD;
	}

	public static boolean isWearing(LivingEntity le) {
		if (le.getItemBySlot(EquipmentSlot.HEAD).is(GLItems.STRANGE_GLASSES.get()))
			return true;
		if (ModList.get().isLoaded("curios")) {
			return CuriosManager.hasItem(le, GLItems.STRANGE_GLASSES.get(), false);
		}
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemTools.STRANGE_GLASSES_LORE.get());
		list.add(GLLang.ItemTools.STRANGE_GLASSES_USE.get());
	}

}
