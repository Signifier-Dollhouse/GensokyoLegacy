package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.rpg.action.GiftReturnAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.GiftReturnCondition;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.SimpleDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.gift.GiftReturn;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.List;

/**
 * What a character does about a gift: the line for taking it, the line for turning it down, and the
 * chat she hands one back in once the player has given her enough. The last is an override rather than
 * a topic, so it costs no starter line of its own. Every character is listed here rather than in a
 * generator of its own because all three are the same shape for all of them and two of them — Cirno
 * and Suika — have no generator to put them in.
 * <p>
 * The third reaction the rigs also ship, a plain acceptance for something she has no opinion about, is
 * deliberately not wired up: it is not worth a line of her own, and the two the datamap can actually
 * decide between are whether she takes the item and whether she was going to.
 *
 * @see "doc/design/gift.md"
 */
public class GiftQDGen extends QuestDialogData {

	public GiftQDGen() {
		var leave = giftLeave();
		pair("reimu", GLEntities.REIMU.get(), leave, YoukaiAnim.TALK_05, 10,
				"Why are you giving me things all of a sudden... but I do like it, so I'll keep it.",
				"Ugh, what's this? Take it away, take it away.",
				"Here — this is a thank-you for the gifts. Don't read anything into it.",
				"...Thanks. I'll take it, then.");
		pair("marisa", GLEntities.MARISA.get(), leave, YoukaiAnim.TALK_05, 12,
				"Wow, you really get me. I love this!",
				"Er, I just remembered something's on the stove. Hold on to this one, will you?",
				"Ta-da! I picked this out myself. Hope it's useful!",
				"Oh, this is great! Thanks!");
		pair("morichika", GLEntities.MORICHIKA.get(), leave, YoukaiAnim.TALK_05, 9,
				"How did you know I liked this? I'll accept your kind offer, then.",
				"Er, the shop has no room for this. You take it back, customer.",
				"Here, with the establishment's thanks. Do come again.",
				"Thank you. I'll be sure to visit.");
		pair("alice", GLEntities.ALICE.get(), leave, YoukaiAnim.TALK_05, 12,
				"What a curious thing. I'll keep it — care for some freshly brewed red tea?",
				"Mm, thank you, but I'm afraid I can't accept this one.",
				"I've prepared a little something in return. It's not much, but it's yours.",
				"How thoughtful. Thank you.");
		pair("sakuya", GLEntities.IZAYOI_SAKUYA.get(), leave, YoukaiAnim.TALK_05, 8,
				"This looks like something the young lady would like. I'll take it.",
				"I'm sorry, Scarlet Devil Mansion has no use for this. Please keep it.",
				"Take this, with the young lady's compliments. It is only a small thing.",
				"You're very kind. Thank you.");
		pair("cirno", GLEntities.CIRNO.get(), leave, YoukaiAnim.TALK_05, 10,
				"You're so nice! I really like this!",
				"I don't like this one. You keep it.",
				"I found this outside! It's yours now. Come play with me again, okay?",
				"It's a date! I'll come find you.");
		pair("suika", GLEntities.IBUKI_SUIKA.get(), leave, YoukaiAnim.TALK_01, 15,
				"Now you're talking — you know what we like, so I won't be shy about it~",
				"Hey hey, I don't take just anything. Think it over before you hand it over.",
				"This does nothing for me, so I'm giving it to you. Come drink with me again~",
				"My tolerance isn't great, so go easy on me!");
	}

	/**
	 * One character's pair, plus the chat she returns a gift in.
	 * <p>
	 * {@code threshold} is her own ceiling: the most one gift is currently worth to her, read off the
	 * {@code gift_data} and {@code gift_preference} datamaps, so that no single gift buys a return on
	 * its own. It is hers rather than one figure for the mod because a gift is worth a factor times a
	 * base favor, and the factors differ enough that a shared number would be either out of reach for
	 * Sakuya or trivially passed by Suika. Raising a favor or a factor past it and it has to follow.
	 * <p>
	 * The text key is spelled whole rather than left to {@code dialog}, which would repeat the prefix:
	 * {@code <char>/gift/dialog/<variant>}, not {@code .../gift/<char>/gift/...}.
	 */
	private void pair(String name, EntityType<?> type, SimpleDialogOption leave, YoukaiAnim talkSlot,
					  int threshold, String like, String dislike, String giveBack, String takeIt) {
		prefix(name + "/gift");
		// Named rather than inlined: the gift data map holds them as holders, since which of the two
		// plays is decided by the data map entry, not by the dialog tree.
		var liked = namedDialogKey("like", rawText(name + "/gift/dialog/like", like),
				List.of(YoukaiAnim.LIKE.trigger()), leave);
		var disliked = namedDialogKey("dislike", rawText(name + "/gift/dialog/dislike", dislike),
				List.of(YoukaiAnim.DISLIKE.trigger()), leave);
		// An override, not a weight: a gift owed is owed whatever the player came to ask about, so she
		// hands it over on sight and no topic list is shown until the bank is empty again.
		// TALK_05 where she has one, TALK_01 where she does not: Suika's rig carries two talk clips and
		// folds every slot onto them, so naming the fifth would resolve to nothing and play no animation
		overrideChat(name + "/gift", type, List.of(new GiftReturnCondition()),
				dialogKey("return", rawText(name + "/gift/dialog/return", giveBack),
						List.of(talkSlot.trigger()),
						option("return/take", takeIt, new GiftReturnAction())));
		giftDialog(type, liked, disliked, threshold,
				GensokyoLegacy.loc(GiftReturn.TABLE_DIR + "/" + tablePath(type)));
	}

	/**
	 * The folder name a character's return table sits under, which is her entity type's path — the
	 * same path {@code PartyGift} files its own tables under.
	 */
	private String tablePath(EntityType<?> type) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
	}

}