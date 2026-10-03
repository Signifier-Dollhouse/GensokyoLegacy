package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import dev.xkmc.gensokyolegacy.compat.curios.CuriosManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class TalismanCurioItem extends Item implements ICurioItem {

	public TalismanCurioItem(Properties p) {
		super(p);
	}

	/**
	 * Every talisman stack this entity is carrying: the Curios charm slots, plus whatever it
	 * keeps outside Curios ({@link TalismanHolder}). To an effect the two are the same thing — a
	 * talisman acts on the entity holding it — so every hook below is indifferent to which slot
	 * it came from, and the doll core needed no change here.
	 */
	public static List<ItemStack> equippedTalismans(LivingEntity entity) {
		List<ItemStack> ans = CuriosManager.getEquippedTalismans(entity);
		if (entity instanceof TalismanHolder holder) ans.addAll(holder.talismanStacks());
		return ans;
	}

	/**
	 * Every talisman context currently armed on this entity, cooldowns already filtered out.
	 * One place to build them, so the tick and the two damage hooks cannot drift apart — they
	 * used to be the same loop written three times.
	 */
	public static List<TalismanContext> activeTalismans(LivingEntity entity) {
		List<TalismanContext> ans = new ArrayList<>();
		for (ItemStack curio : equippedTalismans(entity)) {
			if (!(curio.getItem() instanceof TalismanCurioItem cont)) continue;
			for (var ctx : cont.getActiveTalismans(entity, curio)) {
				if (!ctx.isOnCooldown()) ans.add(ctx);
			}
		}
		return ans;
	}

	public static void iterate(LivingEntity entity, Consumer<TalismanContext> cons) {
		for (var ctx : activeTalismans(entity)) cons.accept(ctx);
	}

	public static boolean testAny(LivingEntity entity, Predicate<TalismanContext> cons) {
		for (var ctx : activeTalismans(entity)) {
			if (cons.test(ctx)) return true;
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

	/**
	 * The tick path for a {@link TalismanHolder}, which Curios does not drive and so has to be
	 * called by the holder itself. Deliberately narrow — only the talismans kept outside Curios,
	 * never {@link #equippedTalismans} — because Curios already runs {@link #curioTick} for its
	 * own slots and a Curios wearer calling this would tick those a second time.
	 */
	public static void tickExtraTalismans(LivingEntity entity) {
		if (!(entity instanceof TalismanHolder holder)) return;
		for (ItemStack curio : holder.talismanStacks()) {
			if (!(curio.getItem() instanceof TalismanCurioItem cont)) continue;
			for (var ctx : cont.getActiveTalismans(entity, curio)) {
				if (!ctx.isOnCooldown()) {
					ctx.paper().tickTalisman(ctx);
				}
			}
		}
	}

}
