package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Polymorphic behavior for glove modes, mirroring {@code UmbrellaMode}.
 * Each enum constant holds an instance of this class, eliminating switch/if
 * on {@code DollGloveMode}. All commands run server-side; clients return
 * success without effect.
 */
public abstract class DollGloveHandler {

	/** Ray-trace reach for glove targeting, in blocks (glove.md §2). */
	public static final double TARGET_RANGE = 48;

	/** Server-side target cache TTL, in ticks (5 seconds). */
	public static final long TARGET_TTL = 100;

	public abstract ItemStack icon();

	public abstract Component displayName();

	public abstract Component description();

	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		return InteractionResultHolder.pass(stack);
	}

	protected static DollAttachment attachment(ServerPlayer sp) {
		return GLMeta.DOLL.type().getOrCreate(sp);
	}

	/**
	 * The cached ray-trace target, re-validated server-side: alive, same level,
	 * within range. The client cache is a hint, never authority. Refreshes the
	 * cache timestamp on success, so repeated uses keep the target alive.
	 */
	@Nullable
	protected static LivingEntity resolveTarget(ServerPlayer sp) {
		var commands = attachment(sp).commands;
		UUID id = commands.gloveTarget;
		if (id == null) return null;
		long now = sp.level().getGameTime();
		if (now - commands.gloveTargetTime > TARGET_TTL) return null;
		if (!(sp.serverLevel().getEntity(id) instanceof LivingEntity target) || !target.isAlive()) return null;
		if (sp.distanceTo(target) > TARGET_RANGE) return null;
		commands.gloveTargetTime = now;
		return target;
	}

	/** Ally check via ownership: the holder plus anything they own (dolls, pets). */
	protected static boolean isAlly(ServerPlayer sp, LivingEntity target) {
		if (target == sp) return true;
		return target instanceof OwnableEntity own && sp.getUUID().equals(own.getOwnerUUID());
	}

	/**
	 * Attack-target validity: allies, any doll (own, stray, another player's,
	 * block-hosted), spectators, and anything unattackable are all excluded.
	 */
	protected static boolean isValidAttackTarget(ServerPlayer sp, LivingEntity target) {
		if (isAlly(sp, target)) return false;
		if (target instanceof BaseDollEntity) return false;
		return target.isAttackable() && !target.isSpectator();
	}

	protected static void cooldown(ServerPlayer sp, DollGloveItem item) {
		if (!sp.isCreative()) sp.getCooldowns().addCooldown(item, 10);
	}

}
