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
	 * Every talisman stack this entity carries: the Curios charm slots plus whatever it keeps outside
	 * Curios ({@link TalismanHolder}) — one list to an effect, so every hook ignores the slot.
	 */
	public static List<ItemStack> equippedTalismans(LivingEntity entity) {
		List<ItemStack> ans = CuriosManager.getEquippedTalismans(entity);
		if (entity instanceof TalismanHolder holder) ans.addAll(holder.talismanStacks());
		return ans;
	}

	/**
	 * Every talisman context currently armed on this entity, cooldowns already filtered out. One
	 * place to build them, so the tick and the two damage hooks cannot drift apart.
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

	/**
	 * Announce that a damage hook just spent a use, so a {@link TalismanHolder} can refresh what mirrors
	 * its talismans — the hooks cannot know whether they spent anything, and syncing costs nothing.
	 */
	public static void syncSpentTalismans(LivingEntity entity) {
		if (entity instanceof TalismanHolder holder) holder.onTalismansSpent();
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
	 * The tick path for a {@link TalismanHolder}, which Curios does not drive. Deliberately narrow —
	 * only the talismans outside {@link #equippedTalismans} — Curios already runs {@link #curioTick}.
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
