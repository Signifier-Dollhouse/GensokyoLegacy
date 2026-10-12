package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A serving taken off a dish and not yet finished: what she is carrying, and what it is
 * worth.
 *
 * <p>
 * Holding both in one memory is what makes the un-banked window durable. Previously the
 * serve was read into the task and credited when the chew ended, which meant the credit
 * only survived because the task's {@code stop()} banked it on the way out — a race with
 * task shutdown. As a memory it is simply state: if she is holding this, she is carrying
 * food, and if she is carrying food this is what it counts for.
 *
 * <p>
 * Both readings are taken <b>before</b> the block is consumed, because a serve's readings
 * belong to the state it was taken from.
 */
public record PartyServe(int hunger, int appeal) {

	public static final Codec<PartyServe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("hunger").forGetter(PartyServe::hunger),
			Codec.INT.fieldOf("appeal").forGetter(PartyServe::appeal)
	).apply(instance, PartyServe::new));

	/** Whether it would credit nothing, and so is not worth banking. */
	public boolean isEmpty() {
		return hunger <= 0 && appeal <= 0;
	}
}