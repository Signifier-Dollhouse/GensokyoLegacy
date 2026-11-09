package dev.xkmc.gensokyolegacy.content.rpg.core;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * The labels a character uses on the topic list.
 *
 * @param greeting      shown above the topic list at home
 * @param visitGreeting shown instead while she is a guest elsewhere, since the home line reads as if the player came
 *                      here
 * @param partyGreeting shown instead while she is a party guest, which outranks the visit line
 * @param trade         label of the trade topic
 */
public record DialogConfig(String greeting, String visitGreeting, String partyGreeting, String trade) {

	/**
	 * The line to show above the topic list: the party line for a party guest, else the visit line for a
	 * visitor, else the home line. An empty line falls through, so one line can serve several contexts.
	 */
	public String body(boolean visiting, boolean partying) {
		if (partying && !partyGreeting.isEmpty()) return partyGreeting;
		if (visiting && !visitGreeting.isEmpty()) return visitGreeting;
		return greeting;
	}

	@Nullable
	public static DialogConfig of(EntityType<?> key) {
		return GLMeta.DIALOG_DATA.get(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(key));
	}

}