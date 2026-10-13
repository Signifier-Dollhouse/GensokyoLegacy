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
 * The second stage of a {@link DaggerGloveMode#HOMING} shot: fired when the straight-flight first
 * stage runs out, aimed at where the target is <em>now</em>.
 * <p>
 * The stage boundary is what turns the dagger — two straight legs at a constant speed rather than
 * one steering entity, so both sides integrate the same path and only the bend is a server
 * decision. Only the server runs this: {@code ItemBulletEntity#terminate} is server-side, so stage 2
 * spawns authoritatively and reaches clients through the normal spawn path.
 * <p>
 * The target is held by UUID rather than network id, because resolution happens server-side only
 * and a UUID cannot be recycled onto a different entity if the target dies mid-flight.
 *
 * @see doc/design/dagger_glove.md
 */
@SerialClass
public class DaggerHomingTrail extends TrailAction {

	/**
	 * The first stage, whose return this trail is responsible for transferring. Not a
	 * {@link SerialField}: a trail that lost it ends the shot with the first stage returning the
	 * dagger itself, which is the correct outcome anyway.
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
	 * Server-side context, deliberately not a {@link SerialField}: it mirrors {@code TrailAction}'s
	 * own unsaved {@code cached} holder. A trail that lost it does not re-aim, which is the same as
	 * its target having vanished.
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
	 * Called by {@code ItemBulletEntity#terminate} when the first stage's life runs out, passing
	 * the position and velocity the first stage ended on.
	 * <p>
	 * The return is transferred, not decided: this claims the first stage's return
	 * ({@link IronDaggerBulletEntity#handOffTo}) only after it has actually spawned, and takes its
	 * own returnability from the outcome of that claim. Every path that spawns nothing leaves the
	 * first stage to return the dagger itself (dagger_glove.md §4, §4a).
	 * <p>
	 * The glove's rune rides the <em>second</em> stage: the first stage's whole life is the
	 * outbound run, which normally hits nothing, so a rune there would fire against whatever the
	 * run happened to clip.
	 */
	@Override
	public void execute(Vec3 pos, Vec3 dir) {
		ServerLevel level = cachedLevel;
		// every early return below leaves the first stage returnable, which is what puts the dagger
		// back: only a turn that really produced a successor claims the return, so a turn that
		// produced nothing costs the thrower nothing
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
		// set after the spawn: the returnability follows the claim, and the entity cannot tick until
		// the next tick anyway. A refused claim means the first stage still owns the dagger, so a
		// successor spawned anyway must not also be returnable — that is two out of one.
		danmaku.setReturnable(firstStage != null && firstStage.handOffTo(true));
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_HIT,
				SoundSource.PLAYERS, 0.5F, 1.5F);
	}

	/**
	 * The holder-carrying form, which a glove dagger never takes: its owner is the thrower, never
	 * a {@code CardHolder}, so {@code terminate} takes its no-holder branch. It exists so that a
	 * holder-carrying path cannot silently do nothing — it adopts the level off the holder when the
	 * trail has lost its own, then defers to the same implementation.
	 */
	@Override
	public void execute(CardHolder holder, Vec3 pos, Vec3 dir) {
		if (cachedLevel == null && holder != null && holder.self().level() instanceof ServerLevel sl) {
			cachedLevel = sl;
		}
		execute(pos, dir);
	}

	/**
	 * The unit direction for the second stage, or null to end the shot. The target is re-read now
	 * rather than reusing the position sampled at the throw: a target that has been walking during
	 * the first stage's flight has usually moved several blocks.
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