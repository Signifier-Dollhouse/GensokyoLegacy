package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeBlockKind;
import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.YoukaiCombatManager;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.NearbyItemsSensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.YoukaiFindPreySensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.YoukaiHomeBlocksSensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.combat.YoukaiSearchTargetTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.home.YoukaiCraftTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.play.ItemPickupTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.play.YoukaiHuntTask;
import dev.xkmc.gensokyolegacy.content.entity.module.*;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
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
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.sensing.TemptingSensor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

@SerialClass
public class CirnoEntity extends FairyEntity implements GeoYoukaiAnim {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("待机");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("坐下");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("睡觉");
	/**
	 * Her hovering loop, which is what an airborne fairy wants instead of standing.
	 * <p>
	 * TODO walk cycle: this rig has no {@code 走路}/{@code 跑步} clip, so walking plays this
	 * too. It cannot stay a missing name: GeckoLib resolves an unknown clip to {@code null},
	 * {@code buildAnimationQueue} then hands the controller an empty queue, and the next
	 * {@code process()} tick stops it without writing any bones — which pinned her in the
	 * last pose written, i.e. frozen mid-hover for the whole walk, silently. Point the
	 * {@code isMoving} branch below back at a {@code WALK} constant once the clip is
	 * exported from Blockbench.
	 */
	private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("漂浮");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");
	private static final RawAnimation GREET = RawAnimation.begin().thenPlay("招呼(性格外向）");
	private static final RawAnimation TALK_01 = RawAnimation.begin().thenPlay("交流_01");
	private static final RawAnimation TALK_02 = RawAnimation.begin().thenPlay("交流_02");
	private static final RawAnimation TALK_03 = RawAnimation.begin().thenPlay("交流_03");
	private static final RawAnimation AGREE = RawAnimation.begin().thenPlay("肯定");
	private static final RawAnimation DECLINE = RawAnimation.begin().thenPlay("拒绝");

	/** Her own entity data, over the generic youkai/spell-data layers. */
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
	 * nothing reads it but {@link CirnoModel#getTextureResource}, so the variant
	 * changes no stats and no behavior. Synced and NBT-backed through
	 * {@link #CIRNO_DATA}, like the rest of her entity data.
	 */
	public boolean isTanned() {
		return entityData.get(DATA_TANNED) != 0;
	}

	public void setTanned(boolean tanned) {
		entityData.set(DATA_TANNED, tanned ? 1 : 0);
	}

	/**
	 * Right-clicking her with a sunflower tans her. One-way and free: the sunflower is
	 * a trigger, not an ingredient, and nothing is consumed or offered back. Hooked on
	 * {@code mobInteract}, which is the mob-interaction slot vanilla leaves open —
	 * {@code Mob#interact} is final and spends itself on name tags and spawn eggs.
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
				new FeedModule(this),
				new TalkModule(this),
				new CountPickupModule(this, e -> e.getItem() instanceof FrozenFrogItem)
		);
	}

	@Override
	protected void constructTaskBoard(TaskBoard board) {
		super.constructTaskBoard(board);
		board.addExclusive(50, new FollowTemptation(e -> 1f), Activity.IDLE, Activity.PLAY, GLBrains.AT_HOME.get());
		board.addExclusive(200, new ItemPickupTask(), Activity.IDLE, Activity.PLAY);
		board.addExclusive(250, new YoukaiHuntTask(6), GLBrains.HUNT.get());

		board.addRandom(new YoukaiCraftTask<>(this::doCraft, 60, 12000), GLBrains.AT_HOME.get());
		board.addSensor(new YoukaiHomeBlocksSensor<>(HomeBlockKind.CONTAINER));

		board.addBehaviorActivity(YoukaiSearchTargetTask.class, GLBrains.HUNT.get());

		//TODO cirno food
		board.addSensor(new TemptingSensor(stack -> stack.is(Items.CAKE)));

		board.addSensor(new NearbyItemsSensor<CirnoEntity>().setRadius(18, 6).setScanRate(e -> e.playOrHunt() ? 20 : 60));
		board.addSensor(new YoukaiFindPreySensor<>(CirnoEntity::playOrHunt));

		board.addPrioritizedActivity(GLBrains.HUNT.get(), GLBrains.MEM_PREY.get(), 200);
	}

	private boolean playOrHunt() {
		var a = getActivity();
		return a == Activity.PLAY || a == GLBrains.HUNT.get();
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

	protected <E extends CirnoEntity> PlayState idleAnimController(final AnimationState<E> event) {
		if (event.getController().isPlayingTriggeredAnimation()) {
			return PlayState.CONTINUE;
		}
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
			// this rig has no thinking clip; 疑惑（baka专属）is a baffled head tilt,
			// not a thinking beat, so the slot stays empty
			case THINK -> Optional.empty();
			case AGREE -> Optional.of(AGREE);
			case DECLINE -> Optional.of(DECLINE);
			case OUTDOOR_IDLE -> Optional.empty();
		};
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Wink first: controllers tick in registration order and overwrite shared
		// bones, so the always-looping blink yields to the main controller.
		controllers.add(new AnimationController<>(this, WINK_CONTROLLER, 0, e -> e.setAndContinue(BLINK)));
		var main = new AnimationController<>(this, ANIM_CONTROLLER, 5, this::idleAnimController);
		addDialogAnims(main);
		controllers.add(main);
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
