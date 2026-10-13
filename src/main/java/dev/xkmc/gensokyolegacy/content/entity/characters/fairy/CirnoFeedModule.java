package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.module.FairyFeedModule;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Feeding a Cirno is feeding a fairy, except for what she gives back.
 * <p>
 * Everything the feed actually costs is inherited untouched: same two tiers, same
 * flat reputation, same hundred-point cap, same twenty-second cooldown, same
 * "food is only a gift once she is no longer your enemy" line. Only the thank-you
 * changes, and it changes completely, because she is not a fairy who found a
 * crystal - she is the one who crafts them out of three frozen frogs.
 * <p>
 * So a cake - vanilla or fairy - buys a whole crystal, the thing she is standing
 * three frogs short of, and a cookie buys four packed ice: enough to start the
 * eight-around-a-crystal recipe at the crafting table with one crystal in hand.
 * A single cookie handing back one ice would be a rounding error on that recipe,
 * and four is the number that makes the cheap tier worth carrying.
 * <p>
 * Both gifts are fixed rather than rolled, unlike a plain fairy's die roll for a
 * flower - there is nothing to roll here. That is also why this is a subclass and
 * not two rows of a table in {@link FairyFeedModule}: the tier, the reputation and
 * the cooldown are shared code and the gift is the entire difference, and a
 * subclass is the shape that says so. {@code FairyFeedTask} finds it anyway, since
 * it looks the module up by {@link FairyFeedModule}.
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
