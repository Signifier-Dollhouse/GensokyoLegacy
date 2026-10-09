package dev.xkmc.gensokyolegacy.content.entity.youkai;

import com.mojang.serialization.Dynamic;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeBlockKind;
import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.SmartBrain;
import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.NearbyLivingEntitySensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.NearbyPlayerSensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.YoukaiHomeBlocksSensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.YoukaiUpdateHomeSensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.combat.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.core.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.home.*;
import dev.xkmc.gensokyolegacy.content.ui.dialog.FirstDialogSession;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SerialClass
public class SmartYoukaiEntity extends YoukaiEntity {

	private TaskBoard board;

	public SmartYoukaiEntity(EntityType<? extends YoukaiEntity> pEntityType, Level pLevel, int maxSize) {
		super(pEntityType, pLevel, maxSize);
	}

	public boolean hasPlayerNearby() {
		return getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS)
				.map(List::size).orElse(0) > 0;
	}

	public Activity getActivity() {
		return getBrain().getActiveNonCoreActivity().orElse(Activity.IDLE);
	}

	// setup

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		getBrain().tick((ServerLevel) level(), Wrappers.cast(this));
		// Stale-sleep watchdog: sleep is owned by YoukaiSleepTask, which only runs
		// under REST. If the entity still sleeps without it (e.g. unloaded asleep
		// at night, reloaded during the day), the task never stops to wake it and
		// it would wander around in sleeping pose. Wake in that case.
		if (isSleeping() && getActivity() != Activity.REST) {
			stopSleeping();
		}
	}

	@Override
	protected Brain<?> makeBrain(Dynamic<?> dynamic) {
		checkBoard();
		return SmartBrain.construct(board, dynamic);
	}

	protected void addFightTasks(TaskBoard board) {
		board.addAlways(new YoukaiUpdateTargetTask<>(), Activity.FIGHT);
		board.addAlways(new YoukaiAttackTask<>(16), Activity.FIGHT);
		board.addAlways(new StrafeTarget<>(), Activity.FIGHT);
	}

	protected void constructTaskBoard(TaskBoard board) {
		board.addAlways(new YoukaiLookAtTarget(40, 300), Activity.CORE);
		board.addAlways(new YoukaiMoveTask<>(), Activity.CORE);
		board.addAlways(new YoukaiSwimTask(0.8f), Activity.CORE);
		board.addAlways(new YoukaiSmartDoorTask<>(), Activity.CORE);
		addFightTasks(board);
		board.addAlways(new YoukaiFetchTargetTask<>(), GLBrains.TALK.get(), GLBrains.AT_HOME.get(), Activity.REST);
		board.addAlways(new YoukaiSearchTargetTask<>(), Activity.IDLE, Activity.PLAY);
		board.addAlways(new YoukaiVanishTask(), Activity.IDLE, Activity.PLAY);
		board.addExclusive(0, new YoukaiSleepTask(), Activity.REST);
		board.addExclusive(0, new YoukaiTalkTask<>(), GLBrains.TALK.get());
		board.addExclusive(100, new YoukaiGoHomeTask<>(), Activity.IDLE, GLBrains.AT_HOME.get());
		board.addExclusive(200, new YoukaiRepairHouseTask<>(), GLBrains.AT_HOME.get());
		board.addExclusive(1100, SetEntityLookTarget.create(EntityType.PLAYER, 32),
				Activity.IDLE, Activity.PLAY, GLBrains.AT_HOME.get(), GLBrains.VISITING.get());
		board.addExclusive(1200, SetEntityLookTarget.create(24), Activity.IDLE, Activity.PLAY);

		board.addRandom(RandomStroll.stroll(0.8f), Activity.IDLE, Activity.PLAY);
		board.addRandom(new YoukaiStayInRoomTask<>().speedModifier(0.8f), GLBrains.AT_HOME.get());
		board.addRandom(new YoukaiStayNearHouseTask<>().speedModifier(0.8f)
				.cooldownFor(e -> e.getRandom().nextInt(200, 400)), Activity.IDLE);
		board.addRandom(new YoukaiSitTask<>(100, 200).speedModifier(0.8f)
				.cooldownFor(e -> e.getRandom().nextInt(200, 400)), GLBrains.AT_HOME.get());
		board.addRandom(new YoukaiDoNothing<>(30, 60),
				Activity.IDLE, Activity.PLAY, GLBrains.AT_HOME.get());
		// A visitor keeps to the host's premises, so the stay tasks that key off
		// her own home are useless to her; stroll and idle are what is left.
		// FetchTargetTask is not repeated here: TALK outranks VISITING, so it is
		// already running whenever there is somebody to talk to.
		board.addRandom(RandomStroll.stroll(0.8f), GLBrains.VISITING.get());
		board.addRandom(new YoukaiDoNothing<>(30, 60), GLBrains.VISITING.get());

		board.addSensor(new NearbyPlayerSensor<SmartYoukaiEntity>().setRadius(32, 32).setScanRate(e -> 5));
		board.addSensor(new NearbyLivingEntitySensor<SmartYoukaiEntity>().setRadius(32, 16)
				.setScanRate(self -> self.isAggressive() || self.hasPlayerNearby() ? 10 : 20));
		board.addSensor(new YoukaiUpdateHomeSensor<SmartYoukaiEntity>());
		board.addSensor(new YoukaiHomeBlocksSensor<SmartYoukaiEntity>(HomeBlockKind.CHAIR));

		board.addScheduledActivity(Activity.REST, MemoryModuleType.HOME);
		board.addScheduledActivity(GLBrains.AT_HOME.get(), MemoryModuleType.HOME);
		board.addScheduledActivity(Activity.PLAY, null);
		board.addScheduledActivity(Activity.IDLE, null);
		board.addPrioritizedActivity(GLBrains.TALK.get(), GLBrains.MEM_TALK.get(), 100);
		// 150 is deliberate: TaskBoard injects a prioritized activity's memory as
		// VALUE_ABSENT into every less prioritized activity, so this has to sit
		// below TALK (a visitor must stay able to talk and trade) and above the
		// scheduled activities (a visitor must never sleep or go home).
		board.addPrioritizedActivity(GLBrains.VISITING.get(), GLBrains.MEM_VISIT.get(), 150);

		board.setSchedule(new ScheduleBuilder(new Schedule())
				.changeActivityAt(10, GLBrains.AT_HOME.get())
				.changeActivityAt(2000, Activity.IDLE)
				.changeActivityAt(4000, Activity.PLAY)
				.changeActivityAt(8000, Activity.IDLE)
				.changeActivityAt(10000, GLBrains.AT_HOME.get())
				.changeActivityAt(12000, Activity.REST)
				.build());
	}

	private void checkBoard() {
		if (board == null) {
			board = new TaskBoard();
			constructTaskBoard(board);
			board.build();
		}
	}

	// misc

	public String getBrainDebugInfo() {
		var behaviors = getBrain().getRunningBehaviors();
		StringBuilder ans = new StringBuilder();
		for (var e : behaviors) {
			if (e instanceof GateBehavior<?> g) {
				for (var sub : g.behaviors) {
					if (sub.getStatus() == Behavior.Status.RUNNING)
						ans.append("\n-").append(sub.debugString());
				}
			} else ans.append("\n-").append(e.debugString());
		}
		return getBrain().getActiveNonCoreActivity().map(Activity::getName).orElse("") + ans;
	}

	@Override
	public boolean mayInteract(Player player) {
		if (!super.mayInteract(player)) return false;
		if (level().isClientSide()) {
			return !isSleeping() && !isAggressive();
		}
		if (getActivity() == Activity.REST) return false;
		// FIGHT is priority 0, so this covers it as a case rather than a special
		// one: nothing she would rather be doing than a conversation may be
		// interrupted for a conversation
		return !outrankedBy(GLBrains.TALK.get());
	}

	@Override
	public boolean mayInteract(Player player, Activity activity) {
		return mayInteract(player) && !outrankedBy(activity);
	}

	/**
	 * Whether whatever she is doing now is more urgent than {@code activity}, in which
	 * case nothing belonging to that activity may be started on her.
	 * <p>
	 * Same activity is never an outranking: a character already doing the thing is the
	 * one case where the interaction should go ahead. Everything else is a straight
	 * comparison - FEAST(50) while eating outranks TALK(100), so talking is refused
	 * until the cake is gone; FEAR(25) outranks both, so neither is offered at all
	 * until she has stopped running.
	 */
	protected boolean outrankedBy(Activity activity) {
		var current = getActivity();
		if (current == activity) return false;
		return priorityOf(current) < priorityOf(activity);
	}

	/**
	 * The number an activity was registered with, lowest wins. Falls back to sorting
	 * below everything before the board exists, so that a character asked during
	 * construction is never wrongly considered busy.
	 */
	protected int priorityOf(Activity activity) {
		return board == null ? Integer.MAX_VALUE : board.priorityOf(activity);
	}

	@Override
	public void setTalkTo(@Nullable ServerPlayer player, int time) {
		if (player == null) {
			BrainUtils.clearMemory(this, GLBrains.MEM_TALK.get());
			return;
		}
		getNavigation().stop();
		BrainUtils.clearMemory(this, MemoryModuleType.WALK_TARGET);
		if (time < 0)
			BrainUtils.setMemory(this, GLBrains.MEM_TALK.get(), player);
		else getBrain().setMemoryWithExpiry(GLBrains.MEM_TALK.get(), player, time);
		FirstDialogSession.open(player, this);
		YoukaiAnim.GREET.play(this);
	}

	@Override
	public boolean isTalkingTo(ServerPlayer sp) {
		return BrainUtils.getMemory(this, GLBrains.MEM_TALK.get()) == sp;
	}

	@Override
	public boolean isTarget(LivingEntity e) {
		return targets.contains(e);
	}

	@Override
	public AABB getBoundingBoxForDanmaku() {
		return getBoundingBox();
	}

}
