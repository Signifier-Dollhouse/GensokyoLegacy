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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What a character does about a gift: the three lines she answers it with, and the chat she hands one
 * back in once the player has given her enough. The last is an override rather than a topic, so it
 * costs no starter line of its own. Every character is listed here rather than in a generator of its
 * own because all of them are the same shape.
 * <p>
 * The plain acceptance is optional: a character with no line of her own reuses her like line, which is
 * what the gift path did before the third reaction existed.
 *
 * @see "doc/design/gift.md"
 */
public class GiftQDGen extends QuestDialogData {

	public GiftQDGen() {
		var leave = giftLeave();
		pair("reimu", GLEntities.REIMU.get(), leave, YoukaiAnim.TALK_05, 10,
				"Why are you giving me things all of a sudden... but I do like it, so I'll keep it.",
				null,
				"Ugh, what's this? Take it away, take it away.",
				"Here — this is a thank-you for the gifts. Don't read anything into it.",
				"...Thanks. I'll take it, then.");
		pair("marisa", GLEntities.MARISA.get(), leave, YoukaiAnim.TALK_05, 12,
				"Wow, you really get me. I love this!",
				null,
				"Er, I just remembered something's on the stove. Hold on to this one, will you?",
				"Ta-da! I picked this out myself. Hope it's useful!",
				"Oh, this is great! Thanks!");
		pair("morichika", GLEntities.MORICHIKA.get(), leave, YoukaiAnim.TALK_05, 9,
				"How did you know I liked this? I'll accept your kind offer, then.",
				null,
				"Er, the shop has no room for this. You take it back, customer.",
				"Here, with the establishment's thanks. Do come again.",
				"Thank you. I'll be sure to visit.");
		pair("alice", GLEntities.ALICE.get(), leave, YoukaiAnim.TALK_05, 12,
				"What a curious thing. I'll keep it — care for some freshly brewed red tea?",
				null,
				"Mm, thank you, but I'm afraid I can't accept this one.",
				"I've prepared a little something in return. It's not much, but it's yours.",
				"How thoughtful. Thank you.");
		pair("sakuya", GLEntities.IZAYOI_SAKUYA.get(), leave, YoukaiAnim.TALK_05, 8,
				"This looks like something the young lady would like. I'll take it.",
				null,
				"I'm sorry, Scarlet Devil Mansion has no use for this. Please keep it.",
				"Take this, with the young lady's compliments. It is only a small thing.",
				"You're very kind. Thank you.");
		pair("cirno", GLEntities.CIRNO.get(), leave, YoukaiAnim.TALK_05, 10,
				"You're so nice! I really like this!",
				"A present? Then I'll take it~",
				"I don't like this one. You keep it.",
				"I found this outside! It's yours now. Come play with me again, okay?",
				"It's a date! I'll come find you.");
		pair("suika", GLEntities.IBUKI_SUIKA.get(), leave, YoukaiAnim.TALK_01, 15,
				"Now you're talking — you know what we like, so I won't be shy about it~",
				"Looks pretty ordinary, but it's the thought that counts. I'll take it.",
				"Hey hey, I don't take just anything. Think it over before you hand it over.",
				"This does nothing for me, so I'm giving it to you. Come drink with me again~",
				"My tolerance isn't great, so go easy on me!");
		pair("yuyuko", GLEntities.SAIGYOUJI_YUYUKO.get(), leave, YoukaiAnim.TALK_01, 10,
				"Such a delicate gift. I'm delighted — thank you.",
				"Another gift? You're too kind. I'll accept it, then.",
				"I'm sorry, but I can't accept this one.",
				"This is for you. I hope you'll like it too.",
				"(Accept the thank-you gift)");
		pair("minamitsu", GLEntities.MINAMITSU.get(), leave, YoukaiAnim.TALK_05, 10,
				"You have a good eye. I'll take it, then.",
				"A gift? Don't mind if I do.",
				"I've no use for this aboard a ship. Keep it.",
				"Here — I hauled this up a while back. It's yours.",
				"Thanks. I'll take it.");
	}

	/**
	 * One character's three reactions, plus the chat she returns a gift in.
	 * <p>
	 * {@code threshold} is her own ceiling: the most one gift is currently worth to her, read off the
	 * {@code gift_data} and {@code gift_preference} datamaps, so that no single gift buys a return on
	 * its own. It is hers rather than one figure for the mod because a gift is worth a factor times a
	 * base favor, and the factors differ enough that a shared number would be either out of reach for
	 * Sakuya or trivially passed by Suika. Raising a favor or a factor past it and it has to follow.
	 * <p>
	 * {@code neutral} is null for a character with no line of her own: her like line answers an
	 * ordinary gift as well, which is what she did before the third reaction existed.
	 * <p>
	 * The text key is spelled whole rather than left to {@code dialog}, which would repeat the prefix:
	 * {@code <char>/gift/dialog/<variant>}, not {@code .../gift/<char>/gift/...}.
	 */
	private void pair(String name, EntityType<?> type, SimpleDialogOption leave, YoukaiAnim talkSlot,
					  int threshold, String like, @Nullable String neutral, String dislike,
					  String giveBack, String takeIt) {
		prefix(name + "/gift");
		// Named rather than inlined: the gift data map holds them as holders, since which of the
		// three plays is decided by the data map entry, not by the dialog tree.
		var liked = namedDialogKey("like", rawText(name + "/gift/dialog/like", like),
				List.of(YoukaiAnim.LIKE.trigger()), leave);
		var plain = neutral == null ? liked : namedDialogKey("neutral",
				rawText(name + "/gift/dialog/neutral", neutral),
				List.of(YoukaiAnim.AGREE.trigger()), leave);
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
		giftDialog(type, liked, plain, disliked, threshold,
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