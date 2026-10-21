package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A serving taken off a dish and not yet finished: what she is carrying, and what it is worth.
 * Held on the module so the un-banked window survives a reload: if she is carrying this, she is
 * carrying food. Both readings are taken <b>before</b> the block is consumed, because they belong
 * to the state it came from.
 *
 * @see "doc/design/party/guest.md"
 */
public record PartyServe(int hunger, int appeal) {

	public static final Codec<PartyServe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("hunger").forGetter(PartyServe::hunger),
			Codec.INT.fieldOf("appeal").forGetter(PartyServe::appeal)
	).apply(instance, PartyServe::new));

	/**
	 * Whether it would credit nothing, and so is not worth banking.
	 */
	public boolean isEmpty() {
		return hunger <= 0 && appeal <= 0;
	}
}