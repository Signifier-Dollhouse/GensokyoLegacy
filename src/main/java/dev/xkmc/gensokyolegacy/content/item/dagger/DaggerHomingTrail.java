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
 * stage runs out, in the direction of where the target is <em>now</em>.
 * <p>
 * This is the shape {@code ReimuPart} uses (content/spell/part/ReimuPart.java), and the shape
 * danmaku_api's own {@code HomingSpellForm.Stage} uses: a danmaku flies a fixed line for a fixed
 * number of ticks and then, the instant it expires, spawns a second danmaku aimed at a freshly
 * sampled target position. The stage boundary is what turns the dagger — there is no mover
 * involved, and that is the point.
 * <p>
 * <b>Why a trail action rather than a steering mover.</b> This is the whole reason the second stage
 * exists instead of a mover that turns the first one. A mover can only ask the level where an
 * entity <em>is</em>, and the client's copy of that entity is not the server's: entity positions
 * arrive late, they are interpolated, and under load the gap is large enough that a mover steering
 * on a live target evaluates a visibly different path on each side. The daggers would then be drawn
 * along a trajectory they are not flying, and any client-side collision would disagree with the
 * server's about where they are.
 * <p>
 * Re-aiming exactly once, on the server, and expressing both halves of the flight as plain
 * two straight legs, each a constant velocity, makes the whole trajectory a fixed geometric path:
 * two line segments meeting at one server-chosen point, with one server-chosen direction. Neither
 * side consults a live entity — and the sampled direction reaches the client as part of the second
 * stage's spawn packet, so there is no separate sync to get wrong.
 * The cost is honest and worth stating: the turn is aimed at where the target is at the turn tick
 * and does not track it afterwards, so a target that keeps running sidesteps the second stage
 * instead of being followed to the end.
 * <p>
 * <b>Only the server runs this.</b> {@code ItemBulletEntity#terminate}, the hook that calls a trail
 * action, is reached only on the server, so the second-stage danmaku is spawned authoritatively
 * and reaches clients as an ordinary entity through the normal spawn path. There is no client-side
 * counterpart that could fall out of step, and no need for one.
 * <p>
 * The target is held by UUID rather than network id, unlike the spellcard's tickers: resolution
 * here happens exclusively server-side, where {@code ServerLevel#getEntity(UUID)} exists, and a
 * UUID cannot be silently recycled onto a different entity if the target dies mid-flight.
 */
@SerialClass
public class DaggerHomingTrail extends TrailAction {

	/**
	 * The first stage, whose return this trail is responsible for transferring.
	 * <p>
	 * Not a {@link SerialField}: like {@link #cachedLevel}, this only has to survive the ten to
	 * twenty ticks between the throw and the turn, and a trail that lost it simply ends the shot
	 * with the first stage returning the dagger itself, which is the correct outcome anyway.
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
	 * own unsaved {@code cached} holder, and a trail action only ever runs within a second or two
	 * of being set up. A dagger that outlives the level reference (saved mid-flight, reloaded
	 * elsewhere) simply does not re-aim, which is the same as its target having vanished.
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
	 * <b>The return is transferred, not decided.</b> Both stages are flagged returnable, and this
	 * one claims the first stage's return ({@link IronDaggerBulletEntity#handOffTo}) only after it has
	 * actually spawned. That ordering is the whole correctness of a homing shot's ammo: whichever
	 * way the shot ends, exactly one dagger comes back.
	 * <p>
	 * The alternative — flagging the first stage non-returnable up front and relying on this trail
	 * always producing a successor — loses the dagger whenever it does not: the target died during
	 * the outbound flight, the level reference was gone, or the first stage hit a wall or an entity
	 * before its life ran out (which discards it outright, never reaching {@code terminate} at all).
	 * All of those leave no returnable entity behind and the dagger was simply gone.
	 *
	 * <p>
	 * The glove's rune rides the <em>second</em> stage, not the first. A homing first stage spends
	 * its whole life on the outbound run and normally hits nothing — its job is to be a fixed
	 * geometric segment ending at the turn point — so a rune carried there would almost never fire,
	 * and firing on whatever the outbound run happened to clip would not be what "on hit" should
	 * mean for a mode that is meant to land. The stage that reaches the target is the one holding
	 * the effect.
	 */
	@Override
	public void execute(Vec3 pos, Vec3 dir) {
		ServerLevel level = cachedLevel;
		// every early return below leaves the first stage returnable, which is what puts the dagger
		// back: it expires on the same tick and hands it over itself. Only the path that actually
		// spawns a successor claims the return, so a turn that produced nothing costs the thrower
		// nothing. (This is the bug that lost daggers on the first homing test.)
		if (level == null || level.isClientSide) return;
		Vec3 heading = reAim(level, pos);
		if (heading == null) return;
		var danmaku = new IronDaggerBulletEntity(GLEntities.IRON_DAGGER.get(), cachedOwner, level);
		danmaku.setItem(dagger);
		danmaku.setReturnable(true);
		danmaku.setRune(rune);
		// no mover, same as the first stage: the aimed leg's speed and heading are both fixed, so
		// constant delta movement carries it. The heading change is done here, once, by seeding the
		// new stage's velocity rather than by a mover that turns it tick by tick.
		danmaku.setup(DanmakuItems.Bullet.DAGGER.damage(), life, false, false, heading.scale(speed));
		danmaku.moveTo(pos);
		level.addFreshEntity(danmaku);
		// claim the first stage's return only now that a successor exists to carry it
		if (firstStage != null) firstStage.handOffTo(true);
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_HIT,
				SoundSource.PLAYERS, 0.5F, 1.5F);
	}

	/**
	 * The holder-carrying form, which a glove dagger never actually takes.
	 * <p>
	 * A glove dagger's owner is the thrower, never a {@code CardHolder}, so
	 * {@code ItemBulletEntity#terminate} takes its no-holder branch and calls
	 * {@link #execute(Vec3, Vec3)}. This overload exists only so that a holder-carrying path cannot
	 * silently do nothing: it adopts the level off the holder when the trail has lost its own, which
	 * is what would happen to a trail that was saved mid-flight and reloaded, and then defers to the
	 * same implementation.
	 */
	@Override
	public void execute(CardHolder holder, Vec3 pos, Vec3 dir) {
		if (cachedLevel == null && holder != null && holder.self().level() instanceof ServerLevel sl) {
			cachedLevel = sl;
		}
		execute(pos, dir);
	}

	/**
	 * The unit direction for the second stage, or null to end the shot.
	 * <p>
	 * The target is re-read now rather than reusing the position sampled when the glove was
	 * thrown. A target that has been walking during the ten to eighteen ticks the first stage lasts
	 * has usually moved several blocks, and aiming at where it <em>was</em> would be ten daggers
	 * thrown slightly wrong rather than a homing volley. A target that has died, unloaded or left
	 * the level is not re-aimed at, and the first stage's own heading carries on instead — which is
	 * what the spellcard does when its target disappears.
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