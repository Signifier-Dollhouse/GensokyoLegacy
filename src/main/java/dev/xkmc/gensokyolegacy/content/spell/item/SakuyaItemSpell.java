package dev.xkmc.gensokyolegacy.content.spell.item;

import dev.xkmc.danmakuapi.content.spell.item.ItemSpell;
import dev.xkmc.gensokyolegacy.content.spell.part.SakuyaKnifeStorm;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Sakuya's third move on its own: the knife storm, at a quarter of the rate the spellcard throws it - twenty knives
 * per tick, where the card's eighty is four times as many, a fair fight for a boss and not for a player press.
 */
@SerialClass
public class SakuyaItemSpell extends ItemSpell {

	private static final int PER_TICK = 20;
	private static final int DURATION = 10;

	/**
	 * A storm with no target would spawn nothing — every knife is thrown at one — so the item declares its requirement
	 * instead of burning a charge. Belt and braces over {@code requireTarget}, which misses a target dying mid-cast.
	 */
	@Override
	public void start(LivingEntity player, @Nullable LivingEntity target) {
		super.start(player, target);
		if (target == null) return;
		addTicker(new SakuyaKnifeStorm<SakuyaItemSpell>(PER_TICK, DURATION));
	}

}