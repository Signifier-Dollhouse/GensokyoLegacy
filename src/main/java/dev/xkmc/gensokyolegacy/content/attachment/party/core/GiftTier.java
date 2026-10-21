package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/**
 * One rung of her ladder: the band of appeal that reaches it, and what she hands over over the
 * whole of it. A gift ladder's bands tile without gap or overlap, so exactly one rung ever answers
 * a roll.
 *
 * @param min   appeal that opens the rung, inclusive
 * @param max   appeal that closes it, exclusive; {@code 0} or less when it never closes
 * @param gifts what she is willing to leave on the table while standing in it
 * @see "doc/design/party/send.md"
 */
public record GiftTier(int min, int max, ArrayList<GiftEntry> gifts) {

	/**
	 * the band as a page says it; the top rung reads as a floor, since it never closes
	 */
	public Component bandText() {
		if (max <= 0) return GLLang.JeiExtra.GIFT_BAND_TOP.get(min);
		return GLLang.JeiExtra.GIFT_BAND.get(min, max);
	}

}