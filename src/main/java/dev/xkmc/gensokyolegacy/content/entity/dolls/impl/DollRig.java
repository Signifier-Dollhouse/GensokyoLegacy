package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The animation plumbing every doll kind has: the three controller names, the blend length, the
 * entity event ids, and the instance cache. What plays on them is the kind's own — {@link DollGeo}
 * is the plain doll's clip set, a gadget its own (entity.md §9.1).
 * <p>
 * Deliberately not a {@link DollBaseImpl}: this is the one impl interface a kind without a loadout
 * also has, so it must not drag in the typed handle only {@link DollEntityImpl} can narrow.
 */
public interface DollRig extends GeoEntity {

	String ANIM_CONTROLLER = "all";
	String ONE_SHOT_CONTROLLER = "one_shot";
	String WINK_CONTROLLER = "wink";

	/** Blend length of the hover and of the one-shots alike, in ticks. */
	int ANIM_TRANSITION = 3;

	/**
	 * Entity event ids: vanilla {@code EntityEvent} uses up to 65, so 66+ are safe. Dolls
	 * take 66-70; characters start after them.
	 */
	byte EVENT_ATTACK = 66;
	byte EVENT_BOMB = 67;
	byte EVENT_BOW = 68;
	byte EVENT_SKILL = 69;
	byte EVENT_SLAP = 70;

	private DollRigModule rig() {
		return ((BaseDollEntity) this).getModule(DollRigModule.class);
	}

	@Override
	default AnimatableInstanceCache getAnimatableInstanceCache() {
		return rig().cache;
	}

	/**
	 * The instance cache. It needs the entity, which the module takes here and nothing else does:
	 * construction runs before any instance state exists. Synchronized: dolls construct on the
	 * server thread and render on the client thread.
	 */
	final class DollRigModule implements DollModule {

		private final AnimatableInstanceCache cache;

		public DollRigModule(BaseDollEntity doll) {
			cache = GeckoLibUtil.createInstanceCache(doll);
		}

	}

}