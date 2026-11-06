package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeBlockKind;
import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.YoukaiCombatManager;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.NearbyItemsSensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.YoukaiFindPreySensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.YoukaiHomeBlocksSensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.home.YoukaiCraftTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.play.ItemPickupTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.play.YoukaiHuntTask;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiFlags;
import dev.xkmc.gensokyolegacy.content.item.ingredient.FrozenFrogItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.base.entity.SyncedData;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

@SerialClass
public class CirnoEntity extends FairyEntity implements GeoYoukaiAnim {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("sit");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("sleep");
	/**
	 * Her hovering loop, which is what an airborne fairy wants instead of standing.
	 * <p>
	 * It cannot stay a missing name: GeckoLib resolves an unknown clip to {@code null},
	 * {@code buildAnimationQueue} hands the controller an empty queue, and the next
	 * {@code process()} tick stops writing any bones, pinning her frozen mid-hover for
	 * the whole walk. Point the {@code isMoving} branch below back at a {@code WALK}
	 * constant once the clip is exported from Blockbench.
	 */
	private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("fly");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("swing");
	private static final RawAnimation GREET = RawAnimation.begin().thenPlay("hello");
	private static final RawAnimation TALK_01 = RawAnimation.begin().thenPlay("talk_01");
	private static final RawAnimation TALK_02 = RawAnimation.begin().thenPlay("talk_02");
	private static final RawAnimation TALK_03 = RawAnimation.begin().thenPlay("talk_03");
	private static final RawAnimation TALK_04 = RawAnimation.begin().thenPlay("talk_04");
	private static final RawAnimation TALK_05 = RawAnimation.begin().thenPlay("talk_05");
	private static final RawAnimation THINK = RawAnimation.begin().thenPlay("think_01");
	private static final RawAnimation THINK_02 = RawAnimation.begin().thenPlay("think_02");
	private static final RawAnimation CONFUSED = RawAnimation.begin().thenPlay("puzzle");
	private static final RawAnimation AGREE = RawAnimation.begin().thenPlay("agree");
	private static final RawAnimation DECLINE = RawAnimation.begin().thenPlay("refuse");
	/**
	 * The idle flourish the current export added as {@code idle_animation}.
	 */
	private static final RawAnimation IDLE_FIDGET = RawAnimation.begin().thenPlay("idle_animation");

	/**
	 * Her own entity data, over the generic youkai/spell-data layers.
	 */
	protected static final SyncedData CIRNO_DATA = new SyncedData(CirnoEntity::defineId, SPELL_DATA);

	private static final EntityDataAccessor<Integer> DATA_TANNED = CIRNO_DATA.define(SyncedData.INT, 0, "tanned");

	private static <T> EntityDataAccessor<T> defineId(EntityDataSerializer<T> ser) {
		return SynchedEntityData.defineId(CirnoEntity.class, ser);
	}

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public static AttributeSupplier.Builder createAttributes() {
		return GeneralYoukaiEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 40)
				.add(Attributes.ATTACK_DAMAGE, 6);
	}

	@Override
	protected SyncedData data() {
		return CIRNO_DATA;
	}

	public CirnoEntity(EntityType<? extends CirnoEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean canFreeze() {
		return false;
	}

	// ---------- tanned variant ----------

	/**
	 * Whether she wears the sun-tanned sheet instead of the pale one. Cosmetics only:
	 * nothing reads it but {@link CirnoModel#getTextureResource}. Synced and NBT-backed
	 * through {@link #CIRNO_DATA}, like the rest of her entity data.
	 */
	public boolean isTanned() {
		return entityData.get(DATA_TANNED) != 0;
	}

	public void setTanned(boolean tanned) {
		entityData.set(DATA_TANNED, tanned ? 1 : 0);
	}

	/**
	 * Right-clicking her with a sunflower tans her. One-way and free: the sunflower is
	 * a trigger, not an ingredient. Hooked on {@code mobInteract}, the slot vanilla leaves
	 * open — {@code Mob#interact} is final and spends itself on name tags and spawn eggs.
	 */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).is(Items.SUNFLOWER) && !isTanned()) {
			if (!level().isClientSide) setTanned(true);
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	protected YoukaiCombatManager createCombatManager() {
		return new CirnoCombatManager(this);
	}

	@Override
	protected List<AbstractYoukaiModule> createModules() {
		return List.of(
				new HomeModule(this),
				new VisitModule(this),
				new CirnoFeedModule(this),
				new PartyModule(this),
				new GiftModule(this),
				new CountPickupModule(this, e -> e.getItem() instanceof FrozenFrogItem)
		);
	}

	/**
	 * Her interaction page. Stated on the entity rather than on the module, so {@code GLEntities}
	 * can say which character answers a hand-out without knowing which module does it.
	 */
	public static List<EntityInteraction> interactions() {
		return CirnoFeedModule.interactions();
	}

	@Override
	protected void constructTaskBoard(TaskBoard board) {
		super.constructTaskBoard(board);
		board.addExclusive(50, new FairyFollowTask(), Activity.IDLE, Activity.PLAY, GLBrains.AT_HOME.get());
		board.addExclusive(200, new ItemPickupTask(), Activity.IDLE, Activity.PLAY);
		board.addExclusive(250, new YoukaiHuntTask(6), GLBrains.HUNT.get());

		board.addRandom(new YoukaiCraftTask<>(this::doCraft, 60, 12000), GLBrains.AT_HOME.get());
		board.addSensor(new YoukaiHomeBlocksSensor<>(HomeBlockKind.CONTAINER));

		board.addAlways(new FairyFeedTask(), GLBrains.FEAST.get());
		board.addPrioritizedActivity(GLBrains.FEAST.get(), GLBrains.MEM_FEED.get(), 200);
		board.addAlways(new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS), Activity.CORE);
		board.addSensor(new FairyInterestSensor());

		board.addSensor(new NearbyItemsSensor<CirnoEntity>().setRadius(18, 6).setScanRate(e -> e.playOrHunt() ? 20 : 60));
		board.addSensor(new YoukaiFindPreySensor<>(CirnoEntity::playOrHunt));

		// 700: the lowest rung on GLBrains' priority scale, so a hunt is the least
		// that can be talked out of.
		board.addPrioritizedActivity(GLBrains.HUNT.get(), GLBrains.MEM_PREY.get(), 700);
	}

	private boolean playOrHunt() {
		var a = getActivity();
		return a == Activity.PLAY || a == GLBrains.HUNT.get();
	}

	/**
	 * A hunt is her chasing the prey the sensor found, which is not in the target
	 * container - so that activity has to be allowed to look around for one, as play is.
	 */
	@Override
	public boolean shouldSearchTarget(Activity activity) {
		return super.shouldSearchTarget(activity) || activity == GLBrains.HUNT.get();
	}

	private ItemStack doCraft(boolean simulate) {
		var module = getModule(CountPickupModule.class);
		if (module.isEmpty()) return ItemStack.EMPTY;
		if (module.get().getCount() < 3) return ItemStack.EMPTY;
		if (!simulate) {
			module.get().consume(3);
		}
		return GLItems.FAIRY_ICE_CRYSTAL.asStack();
	}

	@Override
	public String getBrainDebugInfo() {
		int frogPickup = getModule(CountPickupModule.class)
				.map(CountPickupModule::getCount).orElse(0);
		if (frogPickup == 0) return super.getBrainDebugInfo();
		return super.getBrainDebugInfo() + "\n" + frogPickup + " frogs";
	}

	// ---------- geckolib ----------

	public PlayState stateAnimController(AnimationState<?> event) {
		if (isSleeping()) {
			return event.setAndContinue(SLEEP);
		}
		if (isPassenger()) {
			return event.setAndContinue(SIT);
		}
		if (getFlag(YoukaiFlags.FLYING)) {
			return event.setAndContinue(FLOAT);
		}
		if (event.isMoving()) {
			return event.setAndContinue(FLOAT);
		}
		return event.setAndContinue(IDLE);
	}

	@Override
	public Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return switch (anim) {
			case USE_MAINHAND -> Optional.of(USE_MAINHAND);
			case GREET -> Optional.of(GREET);
			case TALK_01 -> Optional.of(TALK_01);
			case TALK_02 -> Optional.of(TALK_02);
			case TALK_03 -> Optional.of(TALK_03);
			case TALK_04 -> Optional.of(TALK_04);
			case TALK_05 -> Optional.of(TALK_05);
			// this rig had no thinking clip until the current export, which added the
			// think_* pair along with a plain puzzle to replace the old baffled baka-only clip
			case THINK -> Optional.of(THINK);
			case THINK_02 -> Optional.of(THINK_02);
			case CONFUSED -> Optional.of(CONFUSED);
			case AGREE -> Optional.of(AGREE);
			case DECLINE -> Optional.of(DECLINE);
			case LIKE -> Optional.of(LIKE);
			case DISLIKE -> Optional.of(DISLIKE);
			case EAT -> Optional.of(EAT);
			case HURT -> Optional.of(HURT);
			case OUTDOOR_IDLE -> Optional.of(IDLE_FIDGET);
			default -> Optional.empty();
		};
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide() && handleAnimEvent(id)) return;
		super.handleEntityEvent(id);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

}
