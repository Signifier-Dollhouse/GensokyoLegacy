package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollModule;

/**
 * Transient combat state of the haunting variant. Nothing here survives park/resummon: a ghost
 * that was mid-volley comes back following its owner.
 */
final class HauntingGhostModule implements DollModule {

	int lockTicks;
	int invisibleTicks;
	int attackCooldown;

}
