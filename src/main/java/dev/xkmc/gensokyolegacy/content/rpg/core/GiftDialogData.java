package dev.xkmc.gensokyolegacy.content.rpg.core;

import dev.xkmc.gensokyolegacy.content.rpg.dialog.Dialog;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * What a character does about a gift: the two lines she answers it with, and how she pays you back.
 *
 * @param like    said when she takes the gift
 * @param dislike said when she turns it down
 * @param threshold gift return progress that buys a gift back; 0 or less means she never pays one
 * @param returnTable one roll of this table is what she hands over, or null for no table
 *
 * @see "doc/design/gift.md"
 */
public record GiftDialogData(Holder<Dialog> like, Holder<Dialog> dislike,
		int threshold, ResourceLocation returnTable) {

	@Nullable
	public static GiftDialogData of(EntityType<?> key) {
		return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(key).getData(GLMeta.GIFT_DIALOG_DATA.reg());
	}

	/**
	 * Whether she owes the player a gift at all. The threshold is read off the datamap rather than
	 * written into the dialogs, so retuning how long a return takes is a datapack edit and not a regen.
	 */
	public boolean owes() {
		return threshold > 0;
	}

}