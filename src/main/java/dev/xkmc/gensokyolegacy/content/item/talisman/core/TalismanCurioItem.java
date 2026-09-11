package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import dev.xkmc.gensokyolegacy.compat.curios.CuriosManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.BiConsumer;

public abstract class TalismanCurioItem extends Item implements ICurioItem {

	public TalismanCurioItem(Properties p) {
		super(p);
	}

	public static void iterate(ServerPlayer sp, BiConsumer<TalismanPaperItem, ItemStack> cons) {
		for (ItemStack curio : CuriosManager.getEquippedTalismans(sp)) {
			if (curio.getItem() instanceof TalismanCurioItem cont) {
				for (var stack : cont.getActiveTalismans(curio)) {
					var item = GLTalismans.DC_TALISMAN_PAPER.get(stack);
					if (item != null && item.value() instanceof TalismanPaperItem paper && !sp.getCooldowns().isOnCooldown(paper)) {
						cons.accept(paper, stack);
					}
				}
			}
		}
	}

	public abstract List<ItemStack> getActiveTalismans(ItemStack stack);

	@Override
	public void curioTick(SlotContext slotContext, ItemStack curio) {
		if (slotContext.entity() instanceof ServerPlayer sp) {
			for (ItemStack stack : getActiveTalismans(curio)) {
				var item = GLTalismans.DC_TALISMAN_PAPER.get(stack);
				if (item != null && item.value() instanceof TalismanPaperItem paper && !sp.getCooldowns().isOnCooldown(paper)) {
					paper.tickTalisman(stack, sp);
				}
			}
		}
	}

}