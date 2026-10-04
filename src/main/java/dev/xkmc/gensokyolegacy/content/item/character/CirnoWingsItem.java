package dev.xkmc.gensokyolegacy.content.item.character;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Cirno's wings. No longer draws anything: the wings are bones of her own geo rig
 * (see {@code CirnoModel}), flapped by her idle clip, so there is no separate model
 * or texture left for this item to point at. Kept as a curio so existing collections
 * and loadouts keep working.
 */
public class CirnoWingsItem extends TouhouWingsItem {

	public CirnoWingsItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		//list.add(GLLang.ItemCommon.USAGE_FAIRY_WINGS.get(GLMechanics.ICE_FAIRY.get().getName()));
	}

}
