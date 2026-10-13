package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.resources.ResourceLocation;

/**
 * How attractive one kind of food is, named by the id it is known under. Appeal is
 * counted per id, never per serving.
 *
 * @see doc/design/party/food.md
 */
public record FoodAppeal(ResourceLocation id, int appeal) {
}
