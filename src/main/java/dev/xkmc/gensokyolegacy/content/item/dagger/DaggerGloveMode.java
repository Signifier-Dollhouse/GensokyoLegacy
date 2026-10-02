package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The three firing patterns of the {@link DaggerGloveItem}, one per selector-wheel slot.
 * <p>
 * A mode is pure shape and cost: how many daggers leave the glove, along which lines, and how long
 * the shot takes to come back off cooldown. It never decides <em>whether</em> the shot happens —
 * the ammo check and the homing target check both belong to the item, because both apply to every
 * mode the same way ({@code doc/design/dagger_glove.md} §2, §3).
 * <p>
 * Every mode throws at the same speed ({@link #SPEED}), because that is the promise the glove
 * makes: homing changes a dagger's <em>direction</em>, never its speed.
 * <p>
 * The one exception is a homing dagger's outbound stage, whose speed is {@link #HOMING_REACH} over
 * its own turn tick. Its delay says how long it has to reach the turn point, not how far, so fixing
 * the distance is what keeps the five pairs converging as one volley rather than spreading out to
 * five different turn radii.
 * <p>
 * The three shapes are written out one loop each rather than folded into a single parameterised
 * loop, because fan and homing do not actually agree on anything except the speed — homing
 * interleaves left and right daggers and gives each pair its own turn tick, and a shared loop
 * would have to encode that as sign arithmetic to no end.
 * <p>
 * All firing is server-side and takes a {@link ServerLevel} outright rather than a general
 * {@code Level}: a homing shot registers a {@link DaggerHomingTrail}, which only ever fires on the
 * server, and there is no client-side path that could disagree with it about where the target is.
 */
public enum DaggerGloveMode {
	/** One dagger, straight ahead. */
	SINGLE(1, 5, 40, GLLang.ItemDaggerGlove.MODE_SINGLE, GLLang.ItemDaggerGlove.DESC_SINGLE),
	/** Seven daggers fanned across 30 degrees, 5 apart. */
	FAN(7, 10, 40, GLLang.ItemDaggerGlove.MODE_FAN, GLLang.ItemDaggerGlove.DESC_FAN),
	/** Ten daggers, five a side, each turning onto the hovered target at its own tick. */
	HOMING(10, 20, 60, GLLang.ItemDaggerGlove.MODE_HOMING, GLLang.ItemDaggerGlove.DESC_HOMING);

	/** Flight speed of the non-homing modes and of a homing dagger's aimed stage, in blocks per tick. */
	public static final double SPEED = 2;
	/**
	 * How far, in blocks, every homing dagger's outbound stage travels before it turns.
	 * <p>
	 * A homing dagger's first stage exists only to place it at a point from which the aimed stage
	 * can reach the target, and its own turn tick says how long it has to get there — not how far.
	 * Flying all five at {@link #SPEED} therefore spread the turn points from 16 blocks out (the
	 * 8-tick pair) to 40 (the 20-tick pair), so the wider pairs spent most of their flight further
	 * from the target than the narrower ones and the volley lost its shape. Dividing the distance by
	 * the delay instead gives every dagger the same 16-block turn radius: the longer-delayed pairs
	 * simply fly slower to get there, which is also what makes the staggered convergence read as one
	 * throw rather than five.
	 * <p>
	 * Not applied to the aimed stage, whose job is to close on a target and whose life is already
	 * sized to the target range (§ {@code HOMING_STAGE_LIFE}).
	 */
	private static final double HOMING_REACH = 16;
	/** Degrees between neighbouring daggers, in both the fan and the homing spread. */
	private static final double ANGLE_STEP = 5;
	/** Innermost homing dagger's angle, in degrees; the outermost is four more steps out. */
	private static final double HOMING_INNER = 20;
	/** Ticks of straight flight before the innermost homing dagger turns. */
	private static final int HOMING_TURN = 8;
	/** Extra ticks of flight before each successive homing dagger turns. */
	private static final int HOMING_TURN_STEP = 3;
	/**
	 * How long the second, aimed stage of a homing dagger may fly.
	 * <p>
	 * Sizing this to the target range rather than picking a round number: {@link
	 * DaggerGloveItem#TARGET_RANGE} at {@link #SPEED} is 32 ticks, and a little over that lets a
	 * second stage reach anything the glove is willing to aim at, while a target that dies mid-
	 * flight still has its stage expire rather than live forever.
	 * <p>
	 * Must exceed {@link #HOMING_TURN} + 4·{@link #HOMING_TURN_STEP} = 20, the latest a first stage
	 * turns, or the whole shot would die before the outermost dagger got to turn.
	 */
	private static final int HOMING_STAGE_LIFE = 40;

	private final int count, cooldown, life;
	private final GLLang.ItemDaggerGlove name, desc;

	DaggerGloveMode(int count, int cooldown, int life, GLLang.ItemDaggerGlove name, GLLang.ItemDaggerGlove desc) {
		this.count = count;
		this.cooldown = cooldown;
		this.life = life;
		this.name = name;
		this.desc = desc;
	}

	/** How many daggers one shot of this mode spends. */
	public int count() {
		return count;
	}

	/**
	 * This mode's own cooldown in ticks, before any rune is added
	 * ({@code doc/design/dagger_glove.md} §5).
	 */
	public int cooldown() {
		return cooldown;
	}

	/**
	 * How long a dagger of this mode may exist before it is erased and handed back, in ticks.
	 * Homing needs more than the plain {@link #SINGLE} forty: its outermost dagger flies straight
	 * for {@link #HOMING_TURN} + 4·{@link #HOMING_TURN_STEP} = 18 ticks before it even starts
	 * closing on a target that may be at the far end of the glove's range.
	 */
	public int life() {
		return life;
	}

	/** Whether this mode needs a target under the crosshair before it will fire at all. */
	public boolean needsTarget() {
		return this == HOMING;
	}

	/**
	 * Outline tint for this mode's cached target while it is held (vanilla formatting palette).
	 * Only {@link #HOMING} ever has one — the aimed modes mark nothing, so
	 * {@link DaggerGloveItem#targetGlow} returns null for them rather than a color nothing uses.
	 * Red matches the doll glove's attack modes (glove.md §2): both mean "this is what you hit".
	 */
	public int glowColor() {
		return switch (this) {
			case SINGLE, FAN -> 0x000000;
			case HOMING -> 0xFF5555;
		};
	}

	public Component displayName() {
		return name.get();
	}

	public Component description() {
		return desc.get();
	}

	/**
	 * Throws one shot of this mode, one dagger per {@link #count()} line.
	 * <p>
	 * Every dagger is an {@link IronDaggerBulletEntity} carrying {@code dagger} and flagged
	 * returnable, so the whole shot comes back to the thrower as it lands — the glove spends
	 * daggers it gets back, it does not spend them (dagger_glove.md §3, §4).
	 *
	 * @param target the entity homing daggers turn onto; must be non-null exactly when this mode
	 *               {@link #needsTarget()}, which the item has already checked
	 * @param rune   the glove's rune id, carried onto every dagger so it can act on the hit; null
	 *               when the glove has no rune
	 */
	public void fire(ServerLevel level, Player player, ItemStack dagger, @Nullable ResourceLocation rune, @Nullable Entity target) {
		Vec3 look = RayTraceUtil.getRayTerm(Vec3.ZERO, player.getXRot(), player.getYRot(), 1);
		switch (this) {
			case SINGLE -> throwDagger(level, player, dagger, rune, look, null, 0);
			case FAN -> {
				var orientation = DanmakuHelper.getOrientation(look);
				int half = (count - 1) / 2;
				for (int i = 0; i < count; i++) {
					throwDagger(level, player, dagger, rune, orientation.rotateDegrees((i - half) * ANGLE_STEP), null, 0);
				}
			}
			case HOMING -> {
				var orientation = DanmakuHelper.getOrientation(look);
				int perSide = count / 2;
				// pair 0 is the innermost, turning first; pair 4 the outermost, turning last
				for (int pair = 0; pair < perSide; pair++) {
					double angle = HOMING_INNER + pair * ANGLE_STEP;
					int turn = HOMING_TURN + pair * HOMING_TURN_STEP;
					throwDagger(level, player, dagger, rune, orientation.rotateDegrees(-angle), target, turn);
					throwDagger(level, player, dagger, rune, orientation.rotateDegrees(angle), target, turn);
				}
			}
		}
	}

	/**
	 * Throws a single dagger along {@code dir}, turning onto {@code target} after {@code turnTick}
	 * ticks of flight if {@code turnTick} is positive.
	 * <p>
	 * A homing dagger is a <em>pair</em> of entities, not one steering entity: this spawns the
	 * straight-flight first stage with a life of exactly {@code turnTick} and a
	 * {@link DaggerHomingTrail} that spawns the aimed second stage when that life runs out. Both
	 * stages fly a straight line at a constant speed on plain delta movement, so the path is a
	 * fixed geometry that client and server evaluate identically — only the point where it bends,
	 * and the direction it bends toward, are server decisions (dagger_glove.md §2).
	 * <p>
	 * <b>Both</b> stages are returnable, and the trail transfers the first one's return to the
	 * second only once it exists — see {@link IronDaggerBulletEntity#handOffTo} and glove.md §4. The
	 * alternative (flagging the first stage non-returnable here and trusting the trail to always
	 * produce a successor) loses the dagger on every path where it does not, which is most of them.
	 * <p>
	 * Spawned two blocks down the thrower's own ray, the same muzzle {@code DanmakuItem} uses, so a
	 * glove-held dagger leaves the hand exactly where a thrown one does instead of clipping the
	 * thrower's head.
	 */
	private void throwDagger(ServerLevel level, Player player, ItemStack dagger, @Nullable ResourceLocation rune, Vec3 dir, @Nullable Entity target, int turnTick) {
		Vec3 from = RayTraceUtil.getRayTerm(player.getEyePosition(), player.getXRot(), player.getYRot(), 2);
		// the outbound stage covers a fixed distance in whatever time it has; everything else flies
		// at the plain speed
		double speed = turnTick > 0 ? HOMING_REACH / turnTick : SPEED;
		var danmaku = new IronDaggerBulletEntity(GLEntities.IRON_DAGGER.get(), player, level);
		danmaku.setItem(dagger);
		danmaku.setReturnable(true);
		danmaku.setRune(rune);
		int stageLife = turnTick > 0 ? turnTick : life();
		// no mover: velocity is constant for a stage's whole life, so the entity integrates its own
		// delta movement. A RectMover would work on the server, but its per-tick velocity is derived
		// from tickCount, which the client reconstructs as the entity's *age* rather than a step
		// count, so a client seeing the dagger mid-flight computes a huge phantom first step.
		danmaku.setup(DanmakuItems.Bullet.DAGGER.damage(), stageLife, false, DanmakuItems.Bullet.DAGGER.bypass(), dir.scale(speed));
		danmaku.moveTo(from);
		if (turnTick > 0) {
			// The first stage stays returnable: it returns the dagger itself on every ordinary
			// ending (wall, entity, expiry), and DaggerHomingTrail claims that return only once it
			// has really spawned the second stage. Flagging it non-returnable here instead loses the
			// dagger whenever the turn produces nothing.
			// The rune moves to the second stage instead: the first stage's whole life is the
			// outbound run, so a rune there would fire against whatever the run happened to clip.
			danmaku.setRune(null);
			danmaku.afterExpiry = new DaggerHomingTrail(level, danmaku, target, SPEED,
					HOMING_STAGE_LIFE, dir, dagger, rune);
		}
		level.addFreshEntity(danmaku);
		if (player instanceof ServerPlayer sp) SpellContainer.track(sp, danmaku);
	}

	/** The stack a glove-fired dagger is drawn as, and is worth when it lands. */
	public static ItemStack dagger() {
		return new ItemStack(GLItems.IRON_DAGGER.get());
	}

}