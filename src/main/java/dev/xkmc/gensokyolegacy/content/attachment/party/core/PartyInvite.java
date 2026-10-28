package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;
import dev.xkmc.gensokyolegacy.content.attachment.character.PartyModuleData;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.server.level.ServerPlayer;

/**
 * A letter aimed at one character instead of a table: she is put at the front of the next
 * party's draw, provided she would come anyway.
 *
 * @see "doc/design/party/send.md"
 */
public class PartyInvite {

	public enum Result {
		INVITED, ALREADY, IN_PARTY, LOW_REPUTATION, NO_TASTE
	}

	/**
	 * Whether this character can be personally invited, and why not.
	 */
	public static Result check(ServerPlayer sp, YoukaiEntity target) {
		if (target.isPartying()) return Result.IN_PARTY;
		var taste = PartyTaste.of(sp.level().registryAccess(), target.getType());
		// no taste row means she never attends, whatever the reputation says
		if (taste == null || taste.weight() <= 0) return Result.NO_TASTE;
		var data = CharDataHolder.getUnbounded(sp, target.getType()).data();
		if (data.partyData().invited()) return Result.ALREADY;
		if (data.reputation < PartyModuleData.INVITE_MIN_REPUTATION) return Result.LOW_REPUTATION;
		// a taste she has not met yet is not an invitation either
		if (data.reputation < taste.minReputation()) return Result.NO_TASTE;
		return Result.INVITED;
	}

	/**
	 * Record the invitation. The letter is spent by the caller only on {@link Result#INVITED}.
	 */
	public static Result invite(ServerPlayer sp, YoukaiEntity target) {
		var result = check(sp, target);
		if (result != Result.INVITED) return result;
		var holder = CharDataHolder.getUnbounded(sp, target.getType());
		holder.data().partyData().invite();
		holder.sync();
		return Result.INVITED;
	}

	private PartyInvite() {
	}

}
