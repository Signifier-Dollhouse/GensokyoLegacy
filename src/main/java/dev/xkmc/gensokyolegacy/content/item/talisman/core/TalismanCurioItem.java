package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public abstract class TalismanCurioItem extends Item implements ICurioItem {

	public TalismanCurioItem(Properties p) {
		super(p);
	}

	public abstract List<ItemStack> getActiveTalismans(ItemStack stack);

	@Override
	public void curioTick(SlotContext slotContext, ItemStack stack) {
		if (slotContext.entity() instanceof ServerPlayer sp) {
			for (ItemStack paper : getActiveTalismans(stack)) {
				if (paper.getItem() instanceof TalismanPaperItem entry) {
					entry.tickTalisman(paper, sp);
				}
			}
		}
	}

}