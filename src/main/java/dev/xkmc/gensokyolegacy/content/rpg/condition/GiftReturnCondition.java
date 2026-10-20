package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.MapCodec;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.core.GiftDialogData;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Whether this character has been given enough gifts to owe one back. The bar is her own
 * {@code gift_dialog} threshold rather than a number in here, so a character with no datamap entry
 * (or one set to 0) simply never qualifies.
 *
 * @see "doc/design/gift.md"
 */
public record GiftReturnCondition() implements QuestCondition<GiftReturnCondition> {

	/** No fields: the bar is the character's own threshold, not something a dialog spells out. */
	public static final MapCodec<GiftReturnCondition> CODEC = MapCodec.unit(GiftReturnCondition::new);

	@Override
	public MapCodec<GiftReturnCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		var config = GiftDialogData.of(ch.getType());
		if (config == null || !config.owes()) return false;
		return CharDataHolder.get(pl, ch).data().giftReturn >= config.threshold();
	}

}