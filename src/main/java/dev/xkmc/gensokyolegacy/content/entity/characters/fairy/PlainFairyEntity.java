package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.CombatFlyingControl;
import dev.xkmc.gensokyolegacy.content.entity.module.*;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFeatureSet;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import dev.xkmc.l2core.base.entity.SyncedData;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

/**
 * A plain fairy: no named character behind her, just the shared fairy rig. {@link FairyEntity}
 * itself stays the vanilla-placeholder mob for the three named three-star fairies, so this
 * subclass is what carries the Blockbench rig and the four texture sheets.
 */
@SerialClass
public class PlainFairyEntity extends FairyEntity implements GeoYoukaiAnim {

	/**
	 * How many sheets the rig ships, i.e. the range of {@link #DATA_VARIANT}. The four
	 * {@code fairy_*.png} sheets are the same recolour of one mesh, chosen per entity.
	 */
	public static final int VARIANT_COUNT = 4;

	/** Ticks she keeps running for after a hit before the scare wears off. */
	private static final int FLEE_DURATION = 100;

	/** The clearance she wants under her feet: hovering, not standing. */
	private static final double HOVER_CLEARANCE = 1.0;
	/** How close she is willing to come when following someone. */
	private static final double CLOSE_ENOUGH = 2.5;
	/** How far down a floor is looked for; past that there is nothing to hover over. */
	private static final double HOVER_SCAN = 4.0;
	/** Spring rate of the hover, and the fastest it will climb back to her spot. */
	private static final double HOVER_STIFFNESS = 0.2;
	private static final double HOVER_MAX_RISE = 0.25;
	/**
	 * Air friction she drifts on when nothing else is asking for speed. Never on the
	 * ground, the airborne branch of the friction calculation is what governs her
	 * horizontal movement, and that branch reads this rather than her walking speed -
	 * vanilla's 0.02 is a bat's glide and would leave her inching along at a third of
	 * a block per second.
	 */
	private static final float HOVER_DRIFT = 0.06f;

	/** How many times her movement speed she covers while panicking. */
	private static final double FLEE_SPEED_MULTIPLIER = 3.0;
	/**
	 * Share of the gap to the panic speed closed each tick, i.e. roughly half a second
	 * to top out. She is meant to bolt, not to snap to full tilt mid-swing.
	 */
	private static final double FLEE_ACCELERATION = 0.25;

	/**
	 * Her own tunables, matching {@code YoukaiFeatureSet.NONE} except for the velocity
	 * ceiling, which has to clear her panic speed - see {@link #getFeatures()}.
	 */
	private static final YoukaiFeatureSet FEATURES = YoukaiFeatureSet.builder().limit(5).maxSpeed(1.2).build();

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("待机");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("坐下");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("睡觉");
	/**
	 * Her hovering loop, which is what an airborne fairy wants instead of standing.
	 * <p>
	 * Also covers walking: this rig has no {@code 走路}/{@code 跑步} clip, like Cirno's.
	 * It cannot stay a missing name — GeckoLib resolves an unknown clip to {@code null},
	 * {@code buildAnimationQueue} then hands the controller an empty queue, and the next
	 * {@code process()} tick stops it without writing any bones — which would pin her in
	 * the last pose written, i.e. frozen mid-hover for the whole walk, silently.
	 */
	private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("漂浮");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");
	/**
	 * {@code 吃} played and held, rather than played once: the chew has to sit there
	 * until she decides to reach for a flower, and holding the last pose means a
	 * slightly-off timing reads as her still chewing instead of snapping back to
	 * hovering mid-bite.
	 */
	private static final RawAnimation EAT = RawAnimation.begin().thenPlayAndHold("吃");

	/**
	 * Her eating slot, kept out of {@link YoukaiAnim}. That enum is the vocabulary of
	 * conversation - its slots are addressed by trigger name from dialog data - and an
	 * appetite is not dialogue. Putting it there would also have cost every other
	 * character a dead {@code case} in an already exhaustive switch, for a clip only
	 * this rig has. {@code GeoYoukaiAnim} owns 71-79 and the dolls 66-70, so the next
	 * id up is free.
	 */
	private static final String EAT_TRIGGER = "eat";
	private static final byte EAT_EVENT = 80;

	/**
	 * Start the chew. Server-side; the play itself is an entity event, so the client
	 * sees the same animation the server timed the flower throw against.
	 */
	public void broadcastEat() {
		if (!level().isClientSide()) {
			level().broadcastEntityEvent(this, EAT_EVENT);
		}
	}

	/** Her own entity data, layered over the generic youkai/spell-data layers. */
	protected static final SyncedData PLAIN_FAIRY_DATA = new SyncedData(PlainFairyEntity::defineId, SPELL_DATA);

	private static final EntityDataAccessor<Integer> DATA_VARIANT = PLAIN_FAIRY_DATA.define(SyncedData.INT, 0, "variant");

	private static <T> EntityDataAccessor<T> defineId(EntityDataSerializer<T> ser) {
		return SynchedEntityData.defineId(PlainFairyEntity.class, ser);
	}

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public PlainFairyEntity(EntityType<? extends FairyEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected SyncedData data() {
		return PLAIN_FAIRY_DATA;
	}

	// ---------- texture variant ----------

	/**
	 * Which of the four sheets to draw, {@code 0..VARIANT_COUNT-1}. Cosmetics only: nothing
	 * reads it but {@link PlainFairyModel#getTextureResource}, so the variant changes no
	 * stats and no behavior. Synced and NBT-backed through {@link #PLAIN_FAIRY_DATA}, like
	 * the rest of her entity data, so it survives a save/load.
	 */
	public int getVariant() {
		return entityData.get(DATA_VARIANT);
	}

	public void setVariant(int variant) {
		entityData.set(DATA_VARIANT, Math.floorMod(variant, VARIANT_COUNT));
	}

	/**
	 * Rolls the sheet on spawn. In {@code finalizeSpawn} rather than the constructor so
	 * the roll is not re-rolled on every chunk reload, and only on the server so the
	 * client takes the synced value instead of rolling its own.
	 */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
		if (!pLevel.isClientSide()) setVariant(getRandom().nextInt(VARIANT_COUNT));
		return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
	}

	// ---------- hovering ----------

	/**
	 * She wants a block of air under her, and hovering is a movement problem
	 * rather than a brain one: {@code serverAiStep} runs before the entity is
	 * actually moved, and this is the only hook the client side gets as well, so
	 * pinning her here is what keeps both sides from sliding off the mark.
	 */
	@Override
	public void travel(Vec3 input) {
		boolean hover = !isSleeping();
		// before super, so this tick's move is gravity-free too; the flying move
		// control drops the flag on every idle tick, so it has to be re-asserted
		if (hover) {
			if (!isNoGravity()) setNoGravity(true);
			// and the flying navigation has to stay installed: a walk route needs
			// solid ground under the feet, hers is a block of air, so the walking
			// one can path her nowhere. Set on every awake tick because loading an
			// entity puts the control back on the ground.
			if (!navCtrl.isFlying()) navCtrl.setFlying();
		}
		super.travel(input);
		if (hover) hoverStep();
		if (hover && isFleeing()) fleeStep();
	}

	/**
	 * She hovers for a living, so she never touches down and never gets walked.
	 */
	@Override
	public boolean mayLand() {
		return false;
	}

	/**
	 * Spring the vertical velocity toward the spot she wants: one block of clearance
	 * above whatever is directly under her. With no floor within {@link #HOVER_SCAN}
	 * there is nothing to hold station over - a cliff edge, say - so she keeps
	 * whatever height she had rather than being yanked down to nothing.
	 */
	private void hoverStep() {
		var hit = level().clip(new ClipContext(position(), position().add(0.0, -HOVER_SCAN, 0.0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if (hit.getType() != HitResult.Type.BLOCK) return;
		var v = getDeltaMovement();
		double diff = hit.getLocation().y + HOVER_CLEARANCE - getY();
		if (Math.abs(diff) < 0.02) {
			setDeltaMovement(v.x, 0.0, v.z);
			return;
		}
		setDeltaMovement(v.x, Mth.clamp(diff * HOVER_STIFFNESS, -HOVER_MAX_RISE, HOVER_MAX_RISE), v.z);
	}

	/**
	 * Air friction she drifts on when nothing else is asking for speed. Never on the
	 * ground, the airborne branch of the friction calculation is what governs her
	 * horizontal movement, and that branch reads this rather than her walking speed -
	 * vanilla's 0.02 is a bat's glide and would leave her inching along at a third of
	 * a block per second.
	 * <p>
	 * The panic deliberately does not go through here - see {@link #fleeStep()}.
	 *
	 * @see #FLEE_SPEED_MULTIPLIER
	 */
	/** @see #HOVER_DRIFT */
	@Override
	protected float getFlyingSpeed() {
		return HOVER_DRIFT;
	}

	/**
	 * Whether she is currently running from something. Synced rather than tracked
	 * server-side because {@link #fleeStep()} reads it on both sides, and the client
	 * has to agree with the server on how fast she is going or she rubber-bands
	 * straight out of the panic.
	 */
	public boolean isFleeing() {
		return getFlag(YoukaiFlags.FLEEING);
	}

	public void setFleeing(boolean fleeing) {
		setFlag(YoukaiFlags.FLEEING, fleeing);
	}

	/**
	 * Cover ground at a panic speed while she is running, easing in over about half a
	 * second rather than snapping to it. Speed is a target, not a force: the vector is
	 * rescaled along its current heading, so she still turns and climbs exactly as the
	 * navigation asks while the pace stays put.
	 * <p>
	 * This deliberately bypasses the friction chain her ordinary movement goes through.
	 * {@code getFrictionInfluencedSpeed} picks between two entirely different formulas
	 * on {@code onGround()}, and {@code FlyingMoveControl} swaps its own speed source on
	 * that same flag - so anything routed through there is quietly reverted by a single
	 * landing or bounce over rough ground. Holding the number here is the only way a
	 * headline behaviour survives that; {@link CombatFlyingControl} already does the same
	 * thing to her vertical axis.
	 * <p>
	 * With no heading there is nothing to rescale, and the move control supplies one
	 * next tick anyway, so this stands aside rather than guessing a direction.
	 */
	private void fleeStep() {
		var v = getDeltaMovement();
		double speed = Math.sqrt(v.x * v.x + v.z * v.z);
		if (speed < 1.0E-4) return;
		double want = getAttributeValue(Attributes.MOVEMENT_SPEED) * FLEE_SPEED_MULTIPLIER;
		double next = speed + (want - speed) * FLEE_ACCELERATION;
		double scale = next / speed;
		setDeltaMovement(v.x * scale, v.y, v.z * scale);
	}

	/**
	 * She hovers for a living, so she moves on air friction rather than on legs, and her
	 * panic is three times her movement speed. {@link #FLEE_SPEED_MULTIPLIER} alone
	 * would not get her there: the stock 0.5 ceiling on velocity would clip the panic
	 * back to barely above her normal drift, so the multiplier would look like it did
	 * nothing. This ceiling has to clear her top speed or it is taken straight back off.
	 * <p>
	 * The cost is that knockback can now fling her further than any other youkai, which
	 * for a creature that flies is defensible but is a real widening.
	 */
	@Override
	public YoukaiFeatureSet getFeatures() {
		return FEATURES;
	}

	// ---------- a beating ----------

	/**
	 * A hit is the whole of her answer to being hurt: she runs. It drops a
	 * {@link GLBrains#MEM_FEAR} memory naming whoever landed it, which is what puts
	 * her in {@link GLBrains#FEAR} and what {@link FairyFleeTask} runs off. She never
	 * targets anyone back - see {@link #vanishOnDislike()} for why she stays in the
	 * world at all.
	 */
	@Override
	protected void actuallyHurt(DamageSource source, float amount) {
		super.actuallyHurt(source, amount);
		if (level().isClientSide()) return;
		if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
			BrainUtils.setForgettableMemory(this, GLBrains.MEM_FEAR.get(), attacker, FLEE_DURATION);
		}
	}

	/**
	 * She mends herself in the open, at a trickle, for as long as she is still
	 * running. {@link YoukaiEntity#tickTargeting} would instead snap her back to
	 * full on the tick she is hit, which would leave the flee nothing to flee from
	 * - and it would run the give-up-on-the-player watchdog out from under her
	 * while she is mid-run.
	 */
	@Override
	protected void tickTargeting() {
		if (BrainUtils.getMemory(this, GLBrains.MEM_FEAR.get()) == null) {
			super.tickTargeting();
		}
	}

	/**
	 * A plain fairy answers a beating by leaving, not by ceasing to exist. The
	 * dislike threshold would otherwise delete her on the very tick the flee is
	 * meant to begin; she still ends up a stranger-or-worse to the player, so the
	 * reputation cost of hitting her stands.
	 */
	@Override
	public boolean vanishOnDislike() {
		return false;
	}

	// ---------- cake ----------

	@Override
	protected List<AbstractYoukaiModule> createModules() {
		return List.of(
				new HomeModule(this),
				new VisitModule(this),
				new FairyCakeModule(this),
				new TalkModule(this)
		);
	}

	/**
	 * The whole scale she resolves against, lowest number wins:
	 * {@code FIGHT(0) - FEAR(25) - FEAST(50) - TALK(100) - VISITING(150) - HUNT(200)}.
	 * <p>
	 * The part that matters to her: a fight outranks everything, because being
	 * cornered beats every reason she had for standing still. Running beats eating,
	 * so a cake handed to a panicking fairy is <em>not</em> enough to stop her - it
	 * waits, and she eats it once she has put ground between them. Eating beats
	 * talking, because she cannot be drawn into a conversation with her mouth full,
	 * and beats the hunt for the same reason it beats the stroll. 25 and 50 rather
	 * than two round numbers because TALK already holds 100.
	 */
	@Override
	protected void constructTaskBoard(TaskBoard board) {
		super.constructTaskBoard(board);
		// its own activity rather than CORE: eating is the one thing she stops for,
		// and CORE would leave the stroll and stay-tasks running alongside it
		board.addAlways(new FairyCakeTask(), GLBrains.FEAST.get());
		board.addPrioritizedActivity(GLBrains.FEAST.get(), GLBrains.MEM_FEED.get(), 50);
		board.addExclusive(0, new FairyFleeTask(), GLBrains.FEAR.get());
		board.addPrioritizedActivity(GLBrains.FEAR.get(), GLBrains.MEM_FEAR.get(), 25);

		// Being drawn to someone: cake first, and failing that anyone she is not
		// hostile towards. FollowTemptation is only half of it - without the
		// countdown the cooldown it sets on stopping is never cleared and she can
		// only ever be tempted once in her life.
		// The 1.0 is the standoff: vanilla's 2.5 is an allay's idea of personal space
		// and leaves a fairy standing in the player's chest.
		board.addAlways(new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS), Activity.CORE);
		board.addExclusive(50, new FollowTemptation(e -> 1f, e -> CLOSE_ENOUGH),
				Activity.IDLE, Activity.PLAY, GLBrains.AT_HOME.get());
		board.addSensor(new FairyInterestSensor());
	}

	// ---------- geckolib ----------

	protected <E extends PlainFairyEntity> PlayState idleAnimController(final AnimationState<E> event) {
		if (event.getController().isPlayingTriggeredAnimation()) {
			return PlayState.CONTINUE;
		}
		if (isSleeping()) {
			return event.setAndContinue(SLEEP);
		}
		if (isPassenger()) {
			return event.setAndContinue(SIT);
		}
		// She hovers by construction, so "off the ground" is the honest test for
		// the floating clip - the nav FLYING flag only covers the stretches where
		// she is actually being flown somewhere.
		if (!onGround()) {
			return event.setAndContinue(FLOAT);
		}
		return event.setAndContinue(IDLE);
	}

	/**
	 * Only {@code 使用主手物品} is mapped: this rig has no {@code 交流_*}, greeting, agree,
	 * decline or thinking clips, so those slots resolve empty and the dialog system simply
	 * plays nothing for them rather than asking GeckoLib for a clip that is not there.
	 */
	@Override
	public Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return switch (anim) {
			case USE_MAINHAND -> Optional.of(USE_MAINHAND);
			case GREET, TALK_01, TALK_02, TALK_03, THINK, AGREE, DECLINE, OUTDOOR_IDLE -> Optional.empty();
		};
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Blink first: controllers tick in registration order and overwrite shared
		// bones, so the always-looping blink yields to the main controller.
		controllers.add(new AnimationController<>(this, WINK_CONTROLLER, 0, e -> e.setAndContinue(BLINK)));
		var main = new AnimationController<>(this, ANIM_CONTROLLER, 5, this::idleAnimController);
		main.triggerableAnim(EAT_TRIGGER, EAT);
		addDialogAnims(main);
		controllers.add(main);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide()) {
			if (id == EAT_EVENT) {
				triggerAnim(ANIM_CONTROLLER, EAT_TRIGGER);
				return;
			}
			if (handleAnimEvent(id)) return;
		}
		super.handleEntityEvent(id);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

}