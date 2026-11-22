package dev.xkmc.gensokyolegacy.content.rpg.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.core.*;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.util.InventoryMapper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record TradeOffer(
		EntityType<?> character,
		List<QuestCondition<?>> conditions,
		ItemStack result,
		TradeRecurrence recurrence,
		List<IngredientEntry> ingredients
) implements GatedEntry, CharacterEntry, IngredientList {

	public static final Codec<TradeOffer> CODEC = RecordCodecBuilder.create(i -> i.group(
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("character").forGetter(TradeOffer::character),
			CodecRegistry.CONDITION.codec().listOf().fieldOf("conditions").forGetter(TradeOffer::conditions),
			ItemStack.CODEC.fieldOf("result").forGetter(TradeOffer::result),
			TradeRecurrence.CODEC.fieldOf("recurrence").forGetter(TradeOffer::recurrence),
			IngredientEntry.CODEC.listOf().fieldOf("ingredients").forGetter(TradeOffer::ingredients)
	).apply(i, TradeOffer::new));

	public static final Codec<Holder<TradeOffer>> HOLDER = RegistryFileCodec.create(CodecRegistry.Keys.TRADE, CODEC);

	/** The side that carries currency decides the kind: result pays the player, ingredient costs them,
	 * neither is processing; a currency result wins if both sides are. */
	public OfferKind kind() {
		if (result.is(GLTagGen.CURRENCY)) return OfferKind.BUYING;
		for (var entry : ingredients) {
			for (var stack : entry.ingredient().getItems()) {
				if (stack.is(GLTagGen.CURRENCY)) return OfferKind.TRADE;
			}
		}
		return OfferKind.PROCESSING;
	}

	public boolean canTrade(Player pl) {
		return InventoryMapper.testCached(pl, this);
	}

	public void doTrade(ServerPlayer sp) {
		var ans = new InventoryMapper(sp.getInventory().items, ingredients);
		ans.test();
		ans.consume();
	}

	public static ItemStack toIcon(Holder<TradeOffer> offer) {
		var id = offer.unwrapKey().orElseThrow().location();
		var val = offer.value();
		ItemStack stack = val.result().copy();
		if (val.kind() == OfferKind.BUYING && !val.ingredients().isEmpty()) {
			var entry = val.ingredients().getFirst();
			var items = entry.ingredient().getItems();
			if (items.length > 0) stack = items[0].copyWithCount(entry.count());
		}
		return GLItems.DC_OFFER.set(stack, id);
	}

}
