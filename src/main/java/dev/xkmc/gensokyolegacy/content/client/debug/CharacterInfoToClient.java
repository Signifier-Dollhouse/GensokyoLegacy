package dev.xkmc.gensokyolegacy.content.client.debug;

import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationState;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public record CharacterInfoToClient(
		ArrayList<Component> info,
		ArrayList<Component> advanced
) implements SerialPacketBase<CharacterInfoToClient> {

	/** What a party guest has eaten, for the one readout the client cannot answer: her tallies live on the
	 * server-side {@code PartyModule} and cover two questions at once — how much she was <i>fed</i> against
	 * her own fulfillment and how much she was <i>moved by</i> — so both halves travel, not a ratio.
	 *
	 * @param eaten  hunger served to her, measured against her party taste
	 * @param appeal appeal served to her, which is what her gift is sized on
	 * @param wanted the fulfillment her taste asks for
	 */
	public record PartyTally(int eaten, int appeal, int wanted) { }

	public static CharacterInfoToClient ofEntity(
			@Nullable StructureKey home,
			@Nullable BlockPos bed,
			int reputation,
			int reputationCap,
			int feedCD,
			int giftCD,
			String activity,
			@Nullable PartyTally party
	) {
		ArrayList<Component> info = new ArrayList<>();
		ArrayList<Component> advanced = new ArrayList<>();
		if (home == null || bed == null) {
			info.add(GLLang.Info.ENTITY_UNBOUND.get().withStyle(ChatFormatting.GRAY));
		} else {
			info.add(GLLang.Info.ENTITY_BED.get(bed.getX(), bed.getY(), bed.getZ()).withStyle(ChatFormatting.GRAY));
		}
		info.add(ReputationState.toInfo(reputation, reputationCap));
		if (feedCD > 0) {
			info.add(GLLang.Info.ENTITY_FEED.time(feedCD).withStyle(ChatFormatting.GRAY));
		}
		if (giftCD > 0) {
			info.add(GLLang.Info.ENTITY_GIFT.time(giftCD).withStyle(ChatFormatting.GRAY));
		}
		if (party != null) {
			info.add(GLLang.Info.ENTITY_PARTY_EATEN.get(party.eaten(), party.wanted())
					.withStyle(ChatFormatting.GRAY));
			info.add(GLLang.Info.ENTITY_PARTY_APPEAL.get(party.appeal()).withStyle(ChatFormatting.GRAY));
		}
		if (!activity.isEmpty()) {
			String[] strs = activity.split("\n");
			for (var e : strs) {
				advanced.add(Component.literal(e).withStyle(ChatFormatting.DARK_GRAY));
			}
		}
		return new CharacterInfoToClient(info, advanced);
	}

	public static CharacterInfoToClient ofDoor(ArrayList<Component> doors) {
		ArrayList<Component> info = new ArrayList<>();
		info.add(GLLang.Info.DOORS_TO_CLOSE.get(doors.size()).withStyle(ChatFormatting.AQUA));
		info.addAll(doors);
		return new CharacterInfoToClient(info, new ArrayList<>());
	}

	@Override
	public void handle(Player player) {
		InfoUpdateClientManager.handleCharacterInfo(this);
	}

}
