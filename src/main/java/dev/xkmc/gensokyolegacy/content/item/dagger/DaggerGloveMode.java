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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The three firing patterns of the {@link DaggerGloveItem}, one per selector-wheel slot: a mode is pure
 * shape and cost - how many daggers leave the glove, along which lines, and how long the shot takes off
 * cooldown. It never decides <em>whether</em> the shot happens, the ammo and homing target checks belonging
 * to the item.
 * <p>
 * Every mode throws at {@link #SPEED}: homing changes a dagger's <em>direction</em>, never its speed. The
 * three shapes are written out one loop each rather than folded together, because fan and homing agree on
 * nothing but that speed - homing interleaves left and right and gives each pair its own turn tick.
 *
 * @see "doc/design/dagger_glove.md"
 */
public enum DaggerGloveMode {
	/**
	 * One dagger, straight ahead.
	 */
	SINGLE(1, 5, 40, GLLang.ItemDaggerGlove.MODE_SINGLE, GLLang.ItemDaggerGlove.DESC_SINGLE),
	/**
	 * Seven daggers fanned across 30 degrees, 5 apart.
	 */
	FAN(7, 10, 40, GLLang.ItemDaggerGlove.MODE_FAN, GLLang.ItemDaggerGlove.DESC_FAN),
	/**
	 * Ten daggers, five a side, each turning onto the hovered target at its own tick.
	 */
	HOMING(10, 20, 60, GLLang.ItemDaggerGlove.MODE_HOMING, GLLang.ItemDaggerGlove.DESC_HOMING);

	/**
	 * Flight speed of the non-homing modes and of a homing dagger's aimed stage, in blocks per tick.
	 */
	public static final double SPEED = 2;
	/**
	 * How far, in blocks, every homing dagger's outbound stage travels before it turns. A stage's turn tick
	 * says how long it has, not how far, so fixing the distance puts all five turn points on one 16-block
	 * radius and the pairs converge as one volley (dagger_glove.md §2a-1). The aimed stage stays at {@link #SPEED}.
	 */
	private static final double HOMING_REACH = 16;
	/**
	 * Degrees between neighbouring daggers, in both the fan and the homing spread.
	 */
	private static final double ANGLE_STEP = 5;
	/**
	 * Innermost homing dagger's angle, in degrees; the outermost is four more steps out.
	 */
	private static final double HOMING_INNER = 20;
	/**
	 * Ticks of straight flight before the innermost homing dagger turns.
	 */
	private static final int HOMING_TURN = 8;
	/**
	 * Extra ticks of flight before each successive homing dagger turns.
	 */
	private static final int HOMING_TURN_STEP = 3;
	/**
	 * How long the second, aimed stage of a homing dagger may fly.
	 * <p>
	 * Sized to the target range: {@link DaggerGloveItem#TARGET_RANGE} at {@link #SPEED} is 32 ticks, and a
	 * little over that still reaches anything the glove aims at. Must exceed {@link #HOMING_TURN} +
	 * 4·{@link #HOMING_TURN_STEP} = 20, or the outermost dagger would die before it turned.
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

	/**
	 * How many daggers one shot of this mode spends.
	 */
	public int count() {
		return count;
	}

	/**
	 * This mode's own cooldown in ticks, before any rune is added (dagger_glove.md §5).
	 */
	public int cooldown() {
		return cooldown;
	}

	/**
	 * How long a dagger of this mode may exist before it is erased and handed back, in ticks. Only the aimed
	 * modes read this: a homing stage's life is its own turn tick or {@link #HOMING_STAGE_LIFE}.
	 */
	public int life() {
		return life;
	}

	/**
	 * Whether this mode needs a target under the crosshair before it will fire at all.
	 */
	public boolean needsTarget() {
		return this == HOMING;
	}

	/**
	 * Outline tint for this mode's cached target while it is held. Only {@link #HOMING} ever has
	 * one; red matches the doll glove's attack modes (glove.md §2).
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
	 * Throws one shot of this mode, one dagger per {@link #count()} line. Every dagger is a returnable
	 * {@link IronDaggerBulletEntity}, so the whole shot comes back to the thrower - the glove spends daggers
	 * it gets back (dagger_glove.md §3, §4).
	 *
	 * @param target the entity homing daggers turn onto; non-null exactly when this mode {@link #needsTarget()}
	 * @param rune   the glove's rune id, carried onto every dagger; null when the glove has none
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
	 * Throws a single dagger along {@code dir}, turning onto {@code target} after {@code turnTick} ticks of
	 * flight when {@code turnTick} is positive.
	 * <p>
	 * A homing dagger is a <em>pair</em> of entities rather than one steering entity: the straight-flight
	 * first stage expires and a {@link DaggerHomingTrail} spawns the aimed second stage where it stopped.
	 * Both legs are plain delta movement at a constant speed, so client and server integrate the same path
	 * (dagger_glove.md §2).
	 * <p>
	 * Spawned two blocks down the thrower's own ray, the muzzle {@code DanmakuItem} uses, so a glove-held
	 * dagger leaves the hand where a thrown one does instead of clipping the thrower's head.
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
		// no mover: a mover derives its per-tick velocity from tickCount, which the client reads
		// as the entity's age, so a client first seeing the dagger computes a phantom first step (§2a)
		danmaku.setup(DanmakuItems.Bullet.DAGGER.damage(), stageLife, false, DanmakuItems.Bullet.DAGGER.bypass(), dir.scale(speed));
		danmaku.moveTo(from);
		if (turnTick > 0) {
			// the first stage stays returnable: it hands the dagger back itself on every ordinary ending,
			// and the trail claims that return only once it has really spawned stage 2, which takes the rune
			danmaku.setRune(null);
			danmaku.afterExpiry = new DaggerHomingTrail(level, danmaku, target, SPEED,
					HOMING_STAGE_LIFE, dir, dagger, rune);
		}
		level.addFreshEntity(danmaku);
		if (player instanceof ServerPlayer sp) SpellContainer.track(sp, danmaku);
	}

	/**
	 * The stack a glove-fired dagger is drawn as, and is worth when it lands.
	 */
	public static ItemStack dagger() {
		return new ItemStack(GLItems.IRON_DAGGER.get());
	}

}