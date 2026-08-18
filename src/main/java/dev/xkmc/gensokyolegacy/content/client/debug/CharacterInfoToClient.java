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

	public static CharacterInfoToClient ofEntity(
			@Nullable StructureKey home,
			@Nullable BlockPos bed,
			int reputation,
			int feedCD,
			int giftCD,
			String activity
	) {
		ArrayList<Component> info = new ArrayList<>();
		ArrayList<Component> advanced = new ArrayList<>();
		if (home == null || bed == null) {
			info.add(GLLang.INFO$ENTITY_UNBOUND.get().withStyle(ChatFormatting.GRAY));
		} else {
			info.add(GLLang.INFO$ENTITY_BED.get(bed.getX(), bed.getY(), bed.getZ()).withStyle(ChatFormatting.GRAY));
		}
		info.add(ReputationState.toInfo(reputation));
		if (feedCD > 0) {
			info.add(GLLang.INFO$ENTITY_FEED.time(feedCD).withStyle(ChatFormatting.GRAY));
		}
		if (giftCD > 0) {
			info.add(GLLang.INFO$ENTITY_GIFT.time(giftCD).withStyle(ChatFormatting.GRAY));
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
		info.add(GLLang.INFO$DOORS_TO_CLOSE.get(doors.size()).withStyle(ChatFormatting.AQUA));
		info.addAll(doors);
		return new CharacterInfoToClient(info, new ArrayList<>());
	}

	@Override
	public void handle(Player player) {
		InfoUpdateClientManager.handleCharacterInfo(this);
	}

}
