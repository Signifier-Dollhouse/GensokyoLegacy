package dev.xkmc.gensokyolegacy.content.spell.item;

import dev.xkmc.danmakuapi.content.spell.item.ItemSpell;
import dev.xkmc.gensokyolegacy.content.spell.part.SakuyaKnifeStorm;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Sakuya's third move on its own: the knife storm, at a quarter of the rate the spellcard throws it.
 * The twenty knives per tick are what keeps a player-cast version of this readable and survivable —
 * the card's eighty is four times as many knives per tick over the same window, which is a fair fight
 * for a boss and an unfair one for something a player presses once off a cooldown.
 * <p>
 * The window is otherwise identical to the card's, so what a player gets is the same burst the card
 * throws when hit, just thinner: same sphere, same hold-then-launch, same twenty-to-twenty-five tick
 * life.
 */
@SerialClass
public class SakuyaItemSpell extends ItemSpell {

	private static final int PER_TICK = 20;
	private static final int DURATION = 10;

	/**
	 * A storm with no target would spawn nothing at all — every knife is thrown at one — so the item
	 * declares its requirement instead of burning a charge on nothing. This is belt and braces over
	 * {@code SpellItem}'s own {@code requireTarget} check, which covers the right-click but not a
	 * target that dies between it and the first tick.
	 */
	@Override
	public void start(LivingEntity player, @Nullable LivingEntity target) {
		super.start(player, target);
		if (target == null) return;
		addTicker(new SakuyaKnifeStorm<SakuyaItemSpell>(PER_TICK, DURATION));
	}

}