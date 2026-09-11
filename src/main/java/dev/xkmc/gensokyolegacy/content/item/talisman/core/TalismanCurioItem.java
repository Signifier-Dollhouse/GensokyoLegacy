package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import dev.xkmc.gensokyolegacy.compat.curios.CuriosManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Consumer;

public abstract class TalismanCurioItem extends Item implements ICurioItem {

	public TalismanCurioItem(Properties p) {
		super(p);
	}

	public static void iterate(ServerPlayer sp, Consumer<TalismanContext> cons) {
		for (ItemStack curio : CuriosManager.getEquippedTalismans(sp)) {
			if (curio.getItem() instanceof TalismanCurioItem cont) {
				for (var ctx : cont.getActiveTalismans(sp, curio)) {
					if (!sp.getCooldowns().isOnCooldown(ctx.paper())) {
						cons.accept(ctx);
					}
				}
			}
		}
	}

	public abstract List<TalismanContext> getActiveTalismans(ServerPlayer player, ItemStack stack);

	@Override
	public void curioTick(SlotContext slotContext, ItemStack curio) {
		if (slotContext.entity() instanceof ServerPlayer sp) {
			for (TalismanContext ctx : getActiveTalismans(sp, curio)) {
				if (!sp.getCooldowns().isOnCooldown(ctx.paper())) {
					ctx.paper().tickTalisman(ctx);
				}
			}
		}
	}

}