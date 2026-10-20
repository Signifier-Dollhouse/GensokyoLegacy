package dev.xkmc.gensokyolegacy.content.rpg.action;

import com.mojang.serialization.MapCodec;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;
import dev.xkmc.gensokyolegacy.content.rpg.core.GiftDialogData;
import dev.xkmc.gensokyolegacy.content.rpg.gift.GiftReturn;
import net.minecraft.world.item.ItemStack;

/**
 * Hands back one roll of the character's {@code gift_return} table and spends the banked favor.
 * <p>
 * The bank goes before the roll rather than after: a table a datapack has emptied should not leave
 * the player having paid for nothing and owed the next one too. The one case that leaves the counter
 * at zero and hands over nothing is a character with no table at all, which the condition has already
 * kept out of the conversation.
 */
public record GiftReturnAction() implements DialogAction<GiftReturnAction> {

	/**
	 * No fields: which table to roll and how much favor to spend are both read off the character, so a
	 * dialog saying "give a gift back" carries nothing else.
	 */
	public static final MapCodec<GiftReturnAction> CODEC = MapCodec.unit(GiftReturnAction::new);

	@Override
	public MapCodec<GiftReturnAction> codec() {
		return CODEC;
	}

	@Override
	public void execute(ActionContext context) {
		var relationship = CharDataHolder.get(context.sp(), context.character());
		var config = GiftDialogData.of(context.character().getType());
		if (config == null) return;
		relationship.data().giftReturn = 0;
		relationship.sync();
		ItemStack gift = GiftReturn.roll(context.sp(), context.character(), config);
		if (gift.isEmpty()) return;
		context.sp().getInventory().add(gift);
	}

}