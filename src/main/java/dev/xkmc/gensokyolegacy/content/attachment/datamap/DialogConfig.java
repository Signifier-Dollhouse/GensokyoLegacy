package dev.xkmc.gensokyolegacy.content.attachment.datamap;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * The labels a character uses on the topic list.
 *
 * @param greeting      shown above the topic list at home
 * @param visitGreeting shown instead while she is a guest somewhere else, since
 *                      the home line reads as if the player came to her house
 * @param trade         label of the trade topic
 */
public record DialogConfig(String greeting, String visitGreeting, String trade) {

	@Nullable
	public static DialogConfig of(EntityType<?> key) {
		return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(key).getData(GLMeta.DIALOG_DATA.reg());
	}

}