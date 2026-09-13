package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;

/**
 * GeckoLib animation for a doll. Handedness and the instance cache live in
 * {@link DollGeoModule}, fetched per entity — the entity keeps no geo state itself.
 */
public interface DollGeo extends DollBaseImpl, GeoEntity {

	RawAnimation IDLE_L = RawAnimation.begin().thenLoop("hover_idle_l");
	RawAnimation IDLE_R = RawAnimation.begin().thenLoop("hover_idle_r");
	RawAnimation MOVE_L = RawAnimation.begin().thenLoop("hover_move_l");
	RawAnimation MOVE_R = RawAnimation.begin().thenLoop("hover_move_r");

	private DollGeoModule geo() {
		return asDoll().getModule(DollGeoModule.class);
	}

	default boolean isLeftie() {
		return geo().leftie;
	}

	default PlayState dollAnimController(final AnimationState<DollEntity> event) {
		RawAnimation selectedAnim;
		if (event.isMoving()) {
			selectedAnim = isLeftie() ? MOVE_L : MOVE_R;
		} else {
			selectedAnim = isLeftie() ? IDLE_L : IDLE_R;
		}
		return event.setAndContinue(selectedAnim);
	}

	@Override
	default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(asDoll(), "all", 3, this::dollAnimController));
	}

	@Override
	default AnimatableInstanceCache getAnimatableInstanceCache() {
		return geo().cache;
	}

	/**
	 * Handedness plus the lazily built instance cache. The cache needs the entity, which
	 * modules never store (they run before it exists) — it is built on first render.
	 * Synchronized: dolls construct on the server thread and render on the client thread.
	 */
	final class DollGeoModule implements DollModule {

		private final boolean leftie;

		private AnimatableInstanceCache cache;

		public DollGeoModule(DollEntity entity) {
			leftie = new Random().nextBoolean();
			cache = GeckoLibUtil.createInstanceCache(entity);
		}

	}

}
