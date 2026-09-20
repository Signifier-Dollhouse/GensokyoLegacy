package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.menu.DollLoadoutProvider;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

	/** Editor-open reach for doll right-click, in blocks (glove.md §2b). */
	public static final double EDITOR_RANGE = 16;

	/** Server-side target cache TTL, in ticks (5 seconds). */
	public static final long TARGET_TTL = 100;

	public abstract ItemStack icon();

	public abstract Component displayName();

	public abstract Component description();

	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		return InteractionResultHolder.pass(stack);
	}

	/**
	 * Whether left-click issues this mode's command. Attack modes only — the
	 * editor open stays right-click only so fighting never pops a menu.
	 */
	public boolean isAttackCommand() {
		return false;
	}

	/**
	 * Left-click path (server-side): the attack-mode command at the cached
	 * ray-trace target, without the editor check.
	 */
	public void performAttack(ServerPlayer sp, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		performAttackOn(sp, resolveTarget(sp), hand, stack, item);
	}

	/**
	 * Attack-mode command at an explicit target (server-side): validity,
	 * feedback, and cooldown mirror the right-click path, minus the editor
	 * check. Non-attack modes ignore the call.
	 */
	public void performAttackOn(ServerPlayer sp, @Nullable LivingEntity target, InteractionHand hand, ItemStack stack, DollGloveItem item) {
	}

	protected static DollAttachment attachment(ServerPlayer sp) {
		return GLMeta.DOLL.type().getOrCreate(sp);
	}

	/**
	 * Right-click editor shortcut, shared by every mode: when the doll under
	 * the crosshair (immediate server-side ray trace, {@code EDITOR_RANGE}
	 * blocks, blocked by blocks) is an owned doll (or the holder is in
	 * creative), open its loadout instead of the mode action. Returns true
	 * when the editor opened. Unlike attack commands, this never consults the
	 * cached ray-trace target — the trace runs fresh on every use.
	 */
	protected static boolean tryOpenEditor(ServerPlayer sp, DollGloveItem item) {
		DollEntity doll = rayTraceDoll(sp);
		if (doll != null && (doll.isOwner(sp) || sp.getAbilities().instabuild)) {
			DollLoadoutProvider.open(sp, doll);
			cooldown(sp, item);
			return true;
		}
		return false;
	}

	/**
	 * The doll under the holder's crosshair right now (server-side, occluded
	 * by blocks), or null. Fresh trace every call — no cache, no TTL.
	 */
	@Nullable
	private static DollEntity rayTraceDoll(ServerPlayer sp) {
		Level level = sp.level();
		Vec3 from = sp.getEyePosition();
		Vec3 dir = sp.getViewVector(1.0F);
		Vec3 to = from.add(dir.scale(EDITOR_RANGE));
		BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, sp));
		double blockDist = block.getType() == HitResult.Type.MISS ? EDITOR_RANGE : from.distanceTo(block.getLocation());
		AABB box = sp.getBoundingBox().expandTowards(dir.scale(EDITOR_RANGE)).inflate(1);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(sp, from, to, box,
				e -> e instanceof DollEntity doll && doll.isAlive() && doll.isPickable(),
				EDITOR_RANGE * EDITOR_RANGE);
		if (hit == null || from.distanceTo(hit.getLocation()) > blockDist) return null;
		return (DollEntity) hit.getEntity();
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
