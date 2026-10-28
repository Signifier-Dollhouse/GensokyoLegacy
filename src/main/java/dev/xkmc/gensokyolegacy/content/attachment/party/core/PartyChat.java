package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.entity.behavior.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Drives conversations between guests sitting out the pause between dishes. A pair stands up,
 * meets on open ground, trades talk animations and then goes back to eating.
 *
 * @see "doc/design/party/guest.md"
 */
public class PartyChat {

	/**
	 * How often guests are scanned for a conversation, in ticks.
	 */
	public static final int INTERVAL = 5;

	/**
	 * Chance of pairing two guests per guest in seated delay; halved for every pair already talking.
	 */
	private static final double PAIR_CHANCE = 0.3;

	/**
	 * Ticks between talk animations. The speaker alternates each time.
	 */
	private static final int TALK_INTERVAL = 10;

	/**
	 * Conversation length, in ticks: three to five seconds.
	 */
	private static final int TALK_MIN = 60;
	private static final int TALK_MAX = 100;

	/**
	 * How far from a rolled spot to look for ground to stand on.
	 */
	private static final int SPOT_REACH = 2;

	/**
	 * Rolls to look for before giving up on a meeting spot this scan.
	 */
	private static final int SPOT_TRIES = 8;

	/**
	 * One pass over a party's guests: cancel stale talks, start talks whose pair has arrived,
	 * advance the animations, and end any that have run their length.
	 */
	public static void tick(ServerLevel level, PartyAttachment party, long time) {
		var guests = PartyTicker.guests(level, party);
		if (party.phase() != PartyAttachment.Phase.FEASTING) {
			for (var guest : guests) {
				var module = module(guest);
				if (module != null && module.chatting()) module.endChat();
			}
			return;
		}
		for (var guest : guests) {
			var module = module(guest);
			if (module == null || !module.chatting()) continue;
			var other = byId(guests, module.chatPartner());
			var partner = other == null ? null : module(other);
			// a partner who left, unloaded, or turned to something else ends the talk here
			if (other == null || partner == null || !partner.chatting()
					|| !guest.getUUID().equals(partner.chatPartner())) {
				module.endChat();
				continue;
			}
			// one of them was told to leave: the conversation ends before the walkout starts
			if (module.leaving() || partner.leaving()) {
				module.endChat();
				partner.endChat();
				continue;
			}
			if (module.chatArrived() && partner.chatArrived() && !module.talking() && !partner.talking()) {
				module.startChatting(time);
				partner.startChatting(time);
			}
			if (!module.chatLead() || !module.talking()) continue;
			if (time - module.chatStartAt() >= module.chatDuration()) {
				module.endChat();
				partner.endChat();
				continue;
			}
			talk(time, guest, other, module);
		}
		pair(level, party, guests);
	}

	/**
	 * Give a pair that has both arrived their conversation, alternating speakers every
	 * {@link #TALK_INTERVAL} ticks. The last animation is skipped when less than one interval
	 * of the talk remains, so nobody is left mid-gesture.
	 */
	private static void talk(long time, YoukaiEntity lead, YoukaiEntity other, PartyModule module) {
		long elapsed = time - module.chatStartAt();
		if (elapsed < 0 || elapsed % TALK_INTERVAL != 0) return;
		if (module.chatDuration() - elapsed < TALK_INTERVAL) return;
		var speaker = (elapsed / TALK_INTERVAL) % 2 == 0 ? lead : other;
		if (speaker instanceof GeoYoukaiAnim anim)
			anim.broadcastDialogAnim(List.of(), speaker.getRandom());
	}

	/**
	 * Maybe start one new conversation. The chance grows with the number of guests waiting and
	 * halves for each pair already talking, so a full table does not pair everyone at once.
	 */
	private static void pair(ServerLevel level, PartyAttachment party, List<YoukaiEntity> guests) {
		var free = new ArrayList<YoukaiEntity>();
		int talking = 0;
		for (var guest : guests) {
			var module = module(guest);
			if (module == null) continue;
			if (module.chatting()) {
				// a pair still walking to its spot is not talking yet, so it does not halve the chance
				if (module.chatLead() && module.talking()) talking++;
				continue;
			}
			// actually on the chair: rest alone also describes a guest who has stood up
			if (module.inSeatedDelay() && guest.isPassenger()) free.add(guest);
		}
		if (free.size() < 2) return;
		double chance = PAIR_CHANCE * free.size() / (1 << Math.min(talking, 16));
		if (level.getRandom().nextDouble() >= chance) return;
		var random = level.getRandom();
		int first = random.nextInt(free.size());
		int second = random.nextInt(free.size() - 1);
		if (second >= first) second++;
		var a = free.get(first);
		var b = free.get(second);
		var spot = meetingSpot(level, party, a, b);
		if (spot == null) return;
		int duration = TALK_MIN + random.nextInt(TALK_MAX - TALK_MIN + 1);
		// the lead is only the side that drives the clock; either may speak first
		boolean lead = random.nextBoolean();
		begin(a, b, spot, duration, lead);
		begin(b, a, spot, duration, !lead);
	}

	private static void begin(YoukaiEntity guest, YoukaiEntity partner, BlockPos spot, int duration, boolean lead) {
		var module = module(guest);
		if (module == null) return;
		module.beginChat(partner.getUUID(), spot, duration, lead);
	}

	/**
	 * Somewhere inside the party's guest range with ground to stand on, or null when the
	 * neighbourhood is too crowded to find one.
	 */
	@Nullable
	private static BlockPos meetingSpot(ServerLevel level, PartyAttachment party, YoukaiEntity a, YoukaiEntity b) {
		var anchor = party.anchor();
		if (anchor == null) return null;
		int radius = GLModConfig.SERVER.partyGuestRadius.get();
		var random = level.getRandom();
		for (int i = 0; i < SPOT_TRIES; i++) {
			var around = anchor.offset(random.nextInt(-radius, radius + 1), 0,
					random.nextInt(-radius, radius + 1));
			var found = PartySend.freeSpaceNear(level, around, SPOT_REACH);
			if (found == null) continue;
			var spot = BlockPos.containing(found);
			if (spot.distSqr(anchor) > (double) radius * radius) continue;
			if (!a.getNavigation().isStableDestination(spot)) continue;
			if (!b.getNavigation().isStableDestination(spot)) continue;
			return spot;
		}
		return null;
	}

	@Nullable
	private static YoukaiEntity byId(List<YoukaiEntity> guests, @Nullable UUID id) {
		if (id == null) return null;
		for (var guest : guests) {
			if (guest.getUUID().equals(id)) return guest;
		}
		return null;
	}

	@Nullable
	private static PartyModule module(YoukaiEntity guest) {
		return guest.getModule(PartyModule.class).orElse(null);
	}

	private PartyChat() {
	}
}
