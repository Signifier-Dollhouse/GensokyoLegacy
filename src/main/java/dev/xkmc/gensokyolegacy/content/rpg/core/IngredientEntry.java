package dev.xkmc.gensokyolegacy.content.rpg.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record IngredientEntry(Ingredient ingredient, int count, Optional<String> text) {

	/**
	 * 1.20.1 ingredients carry no codec, so the object is read through the vanilla JSON form:
	 * {@code count} and {@code text} sit next to the ingredient's own keys.
	 */
	public static final Codec<IngredientEntry> CODEC = Codec.PASSTHROUGH.xmap(
			dyn -> {
				JsonObject obj = dyn.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
				JsonElement ing = obj.has("items") ? obj.get("items") : obj;
				int count = GsonHelper.getAsInt(obj, "count", 1);
				Optional<String> text = obj.has("text") ? Optional.of(GsonHelper.getAsString(obj, "text")) : Optional.empty();
				return new IngredientEntry(Ingredient.fromJson(ing), count, text);
			},
			entry -> {
				JsonElement json = entry.ingredient().toJson();
				JsonObject obj;
				if (json.isJsonObject()) {
					obj = json.getAsJsonObject();
				} else {
					obj = new JsonObject();
					obj.add("items", json);
				}
				obj.addProperty("count", entry.count());
				entry.text().ifPresent(s -> obj.addProperty("text", s));
				return new Dynamic<>(JsonOps.INSTANCE, obj);
			});

	/**
	 * "- {name}: have/need". A null {@code player} drops the have/need part, for surfaces
	 * that describe an offer without an inventory behind them (JEI).
	 */
	public Component getDesc(@Nullable Player player) {
		var item = text().map(Component::literal).orElse(ingredient.getItems()[0].getHoverName().copy());
		if (player == null)
			return Component.literal("- ").append(item).append(": ")
					.append(Component.literal("" + count).withStyle(ChatFormatting.AQUA));
		int has = 0;
		for (var e : player.getInventory().items) {
			if (e.isEmpty()) continue;
			if (ingredient.test(e))
				has += e.getCount();
		}
		if (has > count) has = count;

		return Component.literal("- ").append(item).append(": ")
				.append(Component.literal("" + has).withStyle(has == count ? ChatFormatting.GREEN : ChatFormatting.RED))
				.append("/").append(Component.literal("" + count).withStyle(ChatFormatting.AQUA));

	}
}
