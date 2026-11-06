package dev.xkmc.gensokyolegacy.mixin;

import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The cooldown clock's counter, opened up so a player's cooldowns can run faster than once per
 * tick.
 * <p>
 * Vanilla stores every cooldown as an absolute end time read against this counter, which
 * advances once per player tick, so the whole clock is what a faster cooldown means.
 *
 * @see "doc/design/cooldown.md"
 */
@Mixin(ItemCooldowns.class)
public interface ItemCooldownsAccessor {

	@Accessor("tickCount")
	int gensokyolegacy$getTickCount();

	@Accessor("tickCount")
	void gensokyolegacy$setTickCount(int tickCount);

}
