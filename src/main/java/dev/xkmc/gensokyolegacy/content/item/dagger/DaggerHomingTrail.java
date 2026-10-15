package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.TrailAction;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The second stage of a {@link DaggerGloveMode#HOMING} shot: fired when the straight-flight first stage runs out,
 * aimed at where the target is <em>now</em>. The stage boundary is what turns the dagger — two straight legs at
 * constant speed, so only the bend is a server decision. The target is held by UUID, never a recyclable network id.
 *
 * @see "doc/design/dagger_glove.md"
 */
@SerialClass
public class DaggerHomingTrail extends TrailAction {

	/**
	 * The first stage, whose return this trail must transfer. Not a {@link SerialField}: a trail that lost
	 * it ends the shot with the first stage returning the dagger itself, which is correct anyway.
	 */
	private transient IronDaggerBulletEntity firstStage = null;
	@SerialField
	private UUID targetId = null;
	@SerialField
	private double speed = 2;
	/** The first stage's own heading, so a target that vanishes mid-flight still has somewhere to go. */
	@SerialField
	private Vec3 fallback = Vec3.ZERO;
	@SerialField
	private ItemStack dagger = ItemStack.EMPTY;
	/** The glove's rune id, put on the second stage rather than the first. */
	@SerialField
	private ResourceLocation rune = null;
	@SerialField
	private int life = 40;

	/**
	 * Server-side context, deliberately not a {@link SerialField}: it mirrors {@code TrailAction}'s own
	 * unsaved {@code cached} holder. A trail that lost it does not re-aim, as if the target had vanished.
	 */
	private transient ServerLevel cachedLevel = null;
	private transient LivingEntity cachedOwner = null;

	@Deprecated
	public DaggerHomingTrail() {
	}

	public DaggerHomingTrail(ServerLevel level, IronDaggerBulletEntity firstStage, Entity target,
			double speed, int life, Vec3 fallback, ItemStack dagger, @Nullable ResourceLocation rune) {
		this.cachedLevel = level;
		this.cachedOwner = firstStage.getOwner() instanceof LivingEntity le ? le : null;
		this.firstStage = firstStage;
		this.targetId = target.getUUID();
		this.speed = speed;
		this.life = life;
		this.fallback = fallback;
		this.dagger = dagger.copy();
		this.rune = rune;
	}

	/**
	 * Called by {@code ItemBulletEntity#terminate} when the first stage's life runs out. The return is transferred,
	 * not decided: it is claimed ({@link IronDaggerBulletEntity#handOffTo}) only once a successor spawns, so a path
	 * spawning nothing leaves the first stage to return the dagger (§4, §4a). The rune rides the second stage.
	 */
	@Override
	public void execute(Vec3 pos, Vec3 dir) {
		ServerLevel level = cachedLevel;
		// every early return below leaves the first stage returnable: only a turn that really produced
		// a successor claims the return, so a turn that produced nothing costs the thrower nothing
		if (level == null || level.isClientSide) return;
		Vec3 heading = reAim(level, pos);
		if (heading == null) return;
		var danmaku = new IronDaggerBulletEntity(GLEntities.IRON_DAGGER.get(), cachedOwner, level);
		danmaku.setItem(dagger);
		danmaku.setRune(rune);
		// no mover, same as the first stage: the aimed leg's speed and heading are both fixed, so
		// constant delta movement carries it. The heading change is seeded here, once.
		danmaku.setup(DanmakuItems.Bullet.DAGGER.damage(), life, false, false, heading.scale(speed));
		danmaku.moveTo(pos);
		level.addFreshEntity(danmaku);
		// set after the spawn: the entity cannot tick until the next tick anyway, and a refused claim
		// means the first stage still owns the dagger, so a successor must not also be returnable
		danmaku.setReturnable(firstStage != null && firstStage.handOffTo(true));
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_HIT,
				SoundSource.PLAYERS, 0.5F, 1.5F);
	}

	/**
	 * The holder-carrying form, which a glove dagger never takes: its owner is the thrower, never a
	 * {@code CardHolder}, so {@code terminate} takes its no-holder branch. It adopts the level off the holder.
	 */
	@Override
	public void execute(CardHolder holder, Vec3 pos, Vec3 dir) {
		if (cachedLevel == null && holder != null && holder.self().level() instanceof ServerLevel sl) {
			cachedLevel = sl;
		}
		execute(pos, dir);
	}

	/**
	 * The unit direction for the second stage, or null to end the shot. The target is re-read now rather
	 * than reusing the position sampled at the throw: it has usually moved several blocks.
	 */
	@Nullable
	private Vec3 reAim(ServerLevel level, Vec3 pos) {
		Entity target = targetId == null ? null : level.getEntity(targetId);
		Vec3 goal = target == null || target.isRemoved() ? null
				// body centre, matching how a spellcard samples its target
				: target.getBoundingBox().getCenter();
		Vec3 heading = (goal == null ? fallback : goal.subtract(pos));
		return heading.lengthSqr() > 1e-4 ? heading.normalize() : null;
	}

	/** The level this trail runs in, or null if it is no longer in one. */
	@Nullable
	public ServerLevel level() {
		return cachedLevel;
	}

	}