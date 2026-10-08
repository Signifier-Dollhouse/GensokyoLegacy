package dev.xkmc.gensokyolegacy.content.attachment.party.item;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartySend;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/**
 * Player-facing text for a send attempt, shared by the letter and the command so
 * a refusal reads the same either way.
 */
public class PartyMessage {

	/** Tell the player what happened, and why not if it did not happen. */
	public static void report(ServerPlayer sp, PartySend.Outcome outcome) {
		if (outcome.ok()) {
			sp.sendSystemMessage(GLLang.Party.SENT.get(outcome.plan().roster().size()));
			return;
		}
		var refusal = outcome.refusal();
		if (refusal == null) return;
		switch (refusal) {
			case NO_FOOD -> sp.sendSystemMessage(GLLang.Party.NO_FOOD.get());
			case NO_SEATS -> sp.sendSystemMessage(GLLang.Party.NO_SEATS.get());
			case PARTY_EXISTS -> sp.sendSystemMessage(GLLang.Party.PARTY_EXISTS.get());
			case NOBODY_CAME -> {
				// name the character the player just missed, and whether to fix it
				// with more food or a better relationship
				var miss = outcome.miss();
				if (miss == null) {
					sp.sendSystemMessage(GLLang.Party.NOBODY_CAME.get());
					return;
				}
				var who = name(miss.getKey().type());
				switch (miss.getValue()) {
					case TOO_PLAIN -> sp.sendSystemMessage(GLLang.Party.NEAR_TOO_PLAIN.get(who));
					case NO_SEAT -> sp.sendSystemMessage(GLLang.Party.NEAR_NO_SEAT.get(who));
					case OTHER -> sp.sendSystemMessage(GLLang.Party.NOBODY_CAME.get());
				}
			}
		}
	}

	/** The character name for a type, falling back to its registry id. */
	public static Component name(EntityType<?> type) {
		return type.getDescription();
	}

	/** A short roster line: "Reimu, Marisa". */
	public static Component roster(Iterable<EntityType<?>> types) {
		var sb = new StringBuilder();
		for (var e : types) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(e.getDescription().getString());
		}
		return sb.isEmpty() ? GLLang.Party.PREVIEW_NONE.get() : Component.literal(sb.toString());
	}

	/** Styled header for the preview dump. */
	public static Component header(Component text) {
		return text.copy().withStyle(ChatFormatting.AQUA);
	}

	private PartyMessage() {
	}

}
