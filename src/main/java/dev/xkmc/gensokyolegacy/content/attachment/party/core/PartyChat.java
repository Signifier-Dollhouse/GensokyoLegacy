package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.entity.behavior.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.GeoYoukaiAnim;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Drives conversations between guests sitting out the pause between dishes. One guest walks to
 * the other, both stand up when she is close, trade talk animations and then go back to eating.
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
	 * How close the guest who walks over has to be before the other one stands and the talk starts.
	 */
	public static final double TALK_RANGE = 3;

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
	 * One pass over a party's guests: cancel stale talks, start talks whose pair is close
	 * enough, advance the animations, and end any that have run their length.
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
			if (other == null || other == guest || partner == null || !partner.chatting()
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
			// both are on their feet and close enough: the initiator has arrived
			if (!module.talking() && !partner.talking()
					&& !guest.isPassenger() && !other.isPassenger()
					&& guest.distanceToSqr(other) <= TALK_RANGE * TALK_RANGE) {
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
		pair(level, guests);
	}

	/**
	 * Give a pair that is on its feet and close enough its conversation, alternating speakers
	 * every {@link #TALK_INTERVAL} ticks. The last animation is skipped when less than one
	 * interval of the talk remains, so nobody is left mid-gesture.
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
	private static void pair(ServerLevel level, List<YoukaiEntity> guests) {
		var free = new ArrayList<YoukaiEntity>();
		int talking = 0;
		for (var guest : guests) {
			var module = module(guest);
			if (module == null) continue;
			if (module.chatting()) {
				// a pair still walking over is not talking yet, so it does not halve the chance
				if (module.chatLead() && module.talking()) talking++;
				continue;
			}
			// actually on the chair: rest alone also describes a guest who has stood up
			if (module.inSeatedDelay() && guest.isPassenger()) free.add(guest);
		}
		if (free.size() < 2) return;
		double chance = PAIR_CHANCE * free.size() / (1 << Math.min(talking, 16));
		var random = level.getRandom();
		if (random.nextDouble() >= chance) return;
		int first = random.nextInt(free.size());
		int second = random.nextInt(free.size() - 1);
		if (second >= first) second++;
		var a = free.get(first);
		var b = free.get(second);
		int duration = TALK_MIN + random.nextInt(TALK_MAX - TALK_MIN + 1);
		// the initiator is the one who walks over; either may speak first
		boolean lead = random.nextBoolean();
		begin(a, b, duration, lead);
		begin(b, a, duration, !lead);
	}

	private static void begin(YoukaiEntity guest, YoukaiEntity partner, int duration, boolean lead) {
		var module = module(guest);
		if (module == null) return;
		module.beginChat(partner.getUUID(), duration, lead);
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
