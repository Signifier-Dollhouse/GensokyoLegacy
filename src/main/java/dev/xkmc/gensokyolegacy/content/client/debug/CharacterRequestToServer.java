package dev.xkmc.gensokyolegacy.content.client.debug;

import dev.xkmc.gensokyolegacy.content.attachment.index.BedRefData;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.FairyFeedModule;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.FeedModule;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.GiftModule;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public record CharacterRequestToServer(UUID id) implements SerialPacketBase<CharacterRequestToServer> {

	@Override
	public void handle(Player player) {
		if (!(player instanceof ServerPlayer sp)) return;
		if (!(sp.serverLevel().getEntity(id) instanceof YoukaiEntity e)) return;
		var home = StructureKey.of(e);
		var bed = home.map(k -> BedRefData.of(sp.serverLevel(), k, e.getType()))
				.map(BedRefData::getBedPos);
		// a character carries at most one feed module, but which kind varies:
		// a fairy's is a fixed menu and flat-rate
		int feedCD = Math.max(
				e.getModule(FeedModule.class).map(FeedModule::getCoolDown).orElse(0),
				e.getModule(FairyFeedModule.class).map(FairyFeedModule::getCoolDown).orElse(0));
		int giftCD = e.getModule(GiftModule.class).map(GiftModule::getCoolDown).orElse(0);
		String activity = e instanceof SmartYoukaiEntity smart ? smart.getBrainDebugInfo() : "";
		// a party guest's tallies are server state and reach the client with everything
		// else about her; null for anyone who is not at a party, which is the ordinary case
		var party = e.getModule(PartyModule.class)
				.map(m -> m.partying()
						? new CharacterInfoToClient.PartyTally(m.eaten(), m.appealEaten(), wanted(m))
						: null)
				.orElse(null);
		e.getData(sp).ifPresent(data -> GensokyoLegacy.HANDLER.toClientPlayer(CharacterInfoToClient.ofEntity(
				home.orElse(null),
				bed.orElse(null),
				data.data().reputation,
				data.data().reputationCap,
				feedCD, giftCD, activity, party
		), sp));
	}

	/**
	 * The fulfillment this character is measured against, or 0 when she has no taste: a datapack can
	 * remove a {@code party_taste} entry mid-party, and a readout showing 0 beats one that throws.
	 */
	private static int wanted(PartyModule module) {
		var taste = module.taste();
		return taste == null ? 0 : taste.fulfillment();
	}

}
