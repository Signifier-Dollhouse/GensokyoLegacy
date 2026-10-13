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
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

/**
 * A plain fairy: no named character behind her, just the shared fairy rig. {@link FairyEntity}
 * itself stays the vanilla-placeholder mob for the three named three-star fairies, so this
 * subclass is what carries the Blockbench rig and the texture sheets.
 */
@SerialClass
public class PlainFairyEntity extends FairyEntity implements GeoYoukaiAnim {

	public static final int VARIANT_COUNT = 5;

	/**
	 * How likely a spawn is to come out wearing a flower crown, out of {@code 100}.
	 * <p>
	 * A minority on purpose: the crown is the same sheet with one part painted in
	 * rather than left transparent, so a crowd of them all wearing one would read as
	 * everyone having the same face. Roughly a third of the range is one in three.
	 */
	private static final int FLOWER_CHANCE = 33;

	/**
	 * Ticks she keeps running for after a hit before the scare wears off.
	 */
	private static final int FLEE_DURATION = 100;

	/**
	 * The clearance she wants under her feet: hovering, not standing.
	 */
	private static final double HOVER_CLEARANCE = 1.0;
	/**
	 * How far down a floor is looked for; past that there is nothing to hover over.
	 */
	private static final double HOVER_SCAN = 4.0;
	/**
	 * Spring rate of the hover, and the fastest it will climb back to her spot.
	 */
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

	/**
	 * How many times her movement speed she covers while panicking.
	 */
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

	private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("漂浮");
	private static final RawAnimation CRYING_FLOAT = RawAnimation.begin().thenLoop("哭泣_漂浮");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");

	protected static final SyncedData PLAIN_FAIRY_DATA = new SyncedData(PlainFairyEntity::defineId, SPELL_DATA);

	private static final EntityDataAccessor<Integer> DATA_VARIANT = PLAIN_FAIRY_DATA.define(SyncedData.INT, 0, "variant");
	private static final EntityDataAccessor<Integer> DATA_FLOWER = PLAIN_FAIRY_DATA.define(SyncedData.INT, 0, "flower");

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
	 * Which of the sheets to draw, {@code 0..VARIANT_COUNT-1}. Cosmetics only: nothing
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
	 * Whether she wears the flower-crowned sheet. Cosmetics only, and independent of
	 * {@link #getVariant()}: every recolour ships both, so this picks which of a pair
	 * to draw rather than being a variant of its own.
	 * <p>
	 * It needs no geometry to match it either - the {@code Flower} bone is in the one
	 * mesh unconditionally and the crown's texels are simply left transparent in the
	 * bare sheets, so this is a sheet choice and nothing else.
	 */
	public boolean hasFlower() {
		return entityData.get(DATA_FLOWER) != 0;
	}

	public void setFlower(boolean flower) {
		entityData.set(DATA_FLOWER, flower ? 1 : 0);
	}

	/**
	 * Rolls both sheet choices on spawn. In {@code finalizeSpawn} rather than the
	 * constructor so the rolls are not re-rolled on every chunk reload, and only on the
	 * server so the client takes the synced values instead of rolling its own.
	 */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
		if (!pLevel.isClientSide()) {
			setVariant(getRandom().nextInt(VARIANT_COUNT));
			setFlower(getRandom().nextInt(100) < FLOWER_CHANCE);
		}
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
		// mayFly rather than a bare awake test: a party guest is awake but must not hover,
		// and this is the tick that would otherwise reinstall her flying navigation
		boolean hover = !isSleeping() && mayFly();
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
		double hity = hit.getType() != HitResult.Type.BLOCK ? getY() - HOVER_SCAN : hit.getLocation().y;
		var v = getDeltaMovement();
		double diff = hity + HOVER_CLEARANCE - getY();
		if (!getNavigation().isDone() && diff < 0) return;
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
	/**
	 * @see #HOVER_DRIFT
	 */
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

	// ---------- being fed ----------

	/**
	 * Order matters: {@code mobInteract} stops at the first module that does not
	 * pass, so {@link FairyFeedModule} sits ahead of {@link GiftModule} and a dish
	 * she recognises is eaten rather than read as a gift. That is the opposite of
	 * {@link GiftModule}'s own suggestion for a character that has both a generic
	 * {@code FeedModule} and a gift list, and it is deliberate here - for a fairy
	 * the food is the whole point of feeding her, and the gift list is the fallback
	 * for everything she will not eat.
	 */
	@Override
	protected List<AbstractYoukaiModule> createModules() {
		return List.of(
				new HomeModule(this),
				new VisitModule(this),
				new FairyFeedModule(this),
				new GiftModule(this)
		);
	}

	/**
	 * The whole scale she resolves against, lowest number wins:
	 * {@code FIGHT(0) - FEAR(100) - FEAST(200) - NAP(500) - VISITING(600) - HUNT(700)}.
	 * <p>
	 * The part that matters to her: a fight outranks everything, because being
	 * cornered beats every reason she had for standing still. Running beats eating,
	 * so a snack handed to a panicking fairy is <em>not</em> enough to stop her - it
	 * waits, and she eats it once she has put ground between them. She never naps, so
	 * NAP is in the list only as a rung she skips over. TALK is absent along with the
	 * talk module itself: she has no conversation to hold, so the rung is skipped
	 * rather than merely outranked.
	 */
	@Override
	protected void constructTaskBoard(TaskBoard board) {
		super.constructTaskBoard(board);
		board.addAlways(new FairyFeedTask(), GLBrains.FEAST.get());
		board.addPrioritizedActivity(GLBrains.FEAST.get(), GLBrains.MEM_FEED.get(), 200);
		board.addExclusive(0, new FairyFleeTask(), GLBrains.FEAR.get());
		board.addPrioritizedActivity(GLBrains.FEAR.get(), GLBrains.MEM_FEAR.get(), 100);
		board.addAlways(new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS), Activity.CORE);
		board.addExclusive(50, new FairyFollowTask(), Activity.IDLE, Activity.PLAY, GLBrains.AT_HOME.get());
		board.addSensor(new FairyInterestSensor());
	}

	// ---------- geckolib ----------

	public PlayState stateAnimController(AnimationState<?> event) {
		if (isSleeping() || isPassenger()) {
			return event.setAndContinue(SIT);
		}
		if (getActivity() == GLBrains.FEAR.get()) {
			event.setAndContinue(CRYING_FLOAT);
		}
		if (event.isMoving()) {
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
			case GREET, TALK_01, TALK_02, TALK_03, TALK_04, TALK_05, THINK, THINK_02,
				 CONFUSED, AGREE, DECLINE, OUTDOOR_IDLE -> Optional.empty();
		};
	}

	/**
	 * This rig has no {@code 受伤} clip, so the flinch must not be named at all — see
	 * {@link GeoYoukaiAnim#hasHurtAnim}.
	 */
	@Override
	public boolean hasHurtAnim() {
		return false;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide()) {
			if (handleAnimEvent(id)) return;
		}
		super.handleEntityEvent(id);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	@Override
	public boolean vanishOutOfSight() {
		return false;
	}

	public static boolean checkFairySpawnRules(
			EntityType<? extends FairyEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
	) {
		boolean flag = MobSpawnType.ignoresLightRequirements(spawnType) || level.getRawBrightness(pos, 0) > 8;
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && flag;
	}

}