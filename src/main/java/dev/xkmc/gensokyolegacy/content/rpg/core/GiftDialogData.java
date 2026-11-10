package dev.xkmc.gensokyolegacy.content.rpg.core;

import dev.xkmc.gensokyolegacy.content.rpg.dialog.Dialog;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * What a character does about a gift: the three lines she answers it with, and how she pays you back.
 * <p>
 * The dialogs are stored as ids rather than holders because the config loader has no registry access;
 * {@link #holder(RegistryAccess, ResourceLocation)} resolves one at the point of use.
 *
 * @param like        said when she is glad to be handed the gift
 * @param neutral     said when she takes it at face value
 * @param dislike     said when she turns it down
 * @param threshold   gift return progress that buys a gift back; 0 or less means she never pays one
 * @param returnTable one roll of this table is what she hands over, or null for no table
 * @see "doc/design/gift.md"
 */
public record GiftDialogData(ResourceLocation like, ResourceLocation neutral, ResourceLocation dislike,
							 int threshold, ResourceLocation returnTable) {

	@Nullable
	public static GiftDialogData of(EntityType<?> key) {
		return GLMeta.GIFT_DIALOG_DATA.get(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(key));
	}

	@Nullable
	public static Holder<Dialog> holder(RegistryAccess access, ResourceLocation id) {
		return access.registry(CodecRegistry.Keys.DIALOG)
				.flatMap(e -> e.getHolder(ResourceKey.create(CodecRegistry.Keys.DIALOG, id)))
				.orElse(null);
	}

	/**
	 * Whether she owes the player a gift at all. The threshold is read off the datamap rather than
	 * written into the dialogs, so retuning how long a return takes is a datapack edit and not a regen.
	 */
	public boolean owes() {
		return threshold > 0;
	}

}
