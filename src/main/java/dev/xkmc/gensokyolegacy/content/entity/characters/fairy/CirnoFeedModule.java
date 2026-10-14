package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.module.FairyFeedModule;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Feeding a Cirno is feeding a fairy, except for what she gives back: she crafts the
 * crystals out of three frozen frogs. A cake - vanilla or fairy - buys a whole crystal,
 * the thing she is three frogs short of, and a cookie buys four packed ice: enough to
 * start the eight-around-a-crystal recipe with one crystal in hand, and the number that
 * makes the cheap tier worth carrying. Both gifts are fixed rather than rolled, unlike
 * a plain fairy's die roll for a flower - there is nothing to roll here. A subclass
 * says that the gift is the entire difference, and {@code FairyFeedTask} still finds it
 * by looking the module up as a {@link FairyFeedModule}.
 */
public class CirnoFeedModule extends FairyFeedModule {

	/**
	 * Packed ice per cookie. Four is two feeding rounds short of a full blue ice,
	 * so a couple of cookies plus the crystal a cake pays out is a blue ice block.
	 */
	public static final int COOKIE_ICE = 4;

	public CirnoFeedModule(CirnoEntity self) {
		super(self);
	}

	@Override
	protected ItemStack cakeGift() {
		return GLItems.FAIRY_ICE_CRYSTAL.asStack();
	}

	@Override
	protected ItemStack cookieGift() {
		return new ItemStack(Blocks.PACKED_ICE, COOKIE_ICE);
	}

}
