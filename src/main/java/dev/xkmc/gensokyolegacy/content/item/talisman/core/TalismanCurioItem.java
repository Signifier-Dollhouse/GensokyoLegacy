package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import dev.xkmc.gensokyolegacy.compat.curios.CuriosManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class TalismanCurioItem extends Item implements ICurioItem {

	public TalismanCurioItem(Properties p) {
		super(p);
	}

	public static void iterate(LivingEntity entity, Consumer<TalismanContext> cons) {
		for (ItemStack curio : CuriosManager.getEquippedTalismans(entity)) {
			if (curio.getItem() instanceof TalismanCurioItem cont) {
				for (var ctx : cont.getActiveTalismans(entity, curio)) {
					if (!ctx.isOnCooldown()) {
						cons.accept(ctx);
					}
				}
			}
		}
	}

	public static boolean testAny(LivingEntity entity, Predicate<TalismanContext> cons) {
		for (ItemStack curio : CuriosManager.getEquippedTalismans(entity)) {
			if (curio.getItem() instanceof TalismanCurioItem cont) {
				for (var ctx : cont.getActiveTalismans(entity, curio)) {
					if (!ctx.isOnCooldown()) {
						if (cons.test(ctx)) return true;
					}
				}
			}
		}
		return false;
	}

	public abstract List<TalismanContext> getActiveTalismans(LivingEntity entity, ItemStack stack);

	@Override
	public void curioTick(SlotContext slotContext, ItemStack curio) {
		if (slotContext.entity() instanceof LivingEntity le) {
			for (TalismanContext ctx : getActiveTalismans(le, curio)) {
				if (!ctx.isOnCooldown()) {
					ctx.paper().tickTalisman(ctx);
				}
			}
		}
	}

}