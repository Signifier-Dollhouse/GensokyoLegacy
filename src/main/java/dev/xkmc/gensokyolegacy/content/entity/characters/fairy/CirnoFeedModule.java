package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.behavior.module.EntityInteraction;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.FairyFeedModule;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

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
@SerialClass
public class CirnoFeedModule extends FairyFeedModule {

	/**
	 * Packed ice per cookie. Four is two feeding rounds short of a full blue ice,
	 * so a couple of cookies plus the crystal a cake pays out is a blue ice block.
	 */
	public static final int COOKIE_ICE = 4;

	/**
	 * Stated once and handed to both halves, so the stack she throws and the stack the page
	 * promises cannot come apart.
	 */
	private static final List<ItemStack> CAKE_GIFT = List.of(GLItems.FAIRY_ICE_CRYSTAL.asStack());
	private static final List<ItemStack> COOKIE_GIFT = List.of(new ItemStack(Blocks.PACKED_ICE, COOKIE_ICE));

	public CirnoFeedModule(CirnoEntity self) {
		super(self);
	}

	/**
	 * The same two dishes as any fairy's, for crystals and packed ice.
	 */
	public static List<EntityInteraction> interactions() {
		return page(CAKE_GIFT, COOKIE_GIFT);
	}

	@Override
	protected ItemStack cakeGift() {
		return CAKE_GIFT.getFirst();
	}

	@Override
	protected ItemStack cookieGift() {
		return COOKIE_GIFT.getFirst();
	}

}
