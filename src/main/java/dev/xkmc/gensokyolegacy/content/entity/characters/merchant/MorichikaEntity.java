package dev.xkmc.gensokyolegacy.content.entity.characters.merchant;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.home.YoukaiRestockShelfTask;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MorichikaEntity extends GeneralYoukaiEntity implements GeoEntity {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public MorichikaEntity(EntityType<? extends GeneralYoukaiEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	protected void constructTaskBoard(TaskBoard board) {
		super.constructTaskBoard(board);
		board.addRandom(new YoukaiRestockShelfTask<>(GLTagGen.MORICHIKA_OFFERS), Activity.WORK);
		board.addRandom(RandomStroll.stroll(0.8f), Activity.WORK);
		board.addScheduledActivity(Activity.WORK, MemoryModuleType.HOME);
		board.setSchedule(new ScheduleBuilder(new Schedule())
				.changeActivityAt(10, GLBrains.AT_HOME.get())
				.changeActivityAt(2000, Activity.WORK)
				.changeActivityAt(9000, Activity.IDLE)
				.changeActivityAt(10000, GLBrains.AT_HOME.get())
				.changeActivityAt(12000, Activity.REST)
				.build());
	}

	@Override
	public boolean mayFly() {
		return false;
	}

	protected <E extends MorichikaEntity> PlayState idleAnimController(final AnimationState<E> event) {
		if (getFlag(YoukaiFlags.FLYING)) {
			return event.setAndContinue(IDLE);
		}
		if (event.isMoving()) {
			return event.setAndContinue(WALK);
		}
		return event.setAndContinue(IDLE);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "Moving", 5, this::idleAnimController));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

}
