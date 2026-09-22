package dev.xkmc.gensokyolegacy.content.rpg.requirement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientList;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestData;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.EmptyTrigger;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.util.InventoryMapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public record HasItemRequirement(
		List<IngredientEntry> ingredients
) implements QuestRequirement<HasItemRequirement, EmptyTrigger>, IngredientList {

	public static final MapCodec<HasItemRequirement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IngredientEntry.CODEC.listOf().fieldOf("ingredients").forGetter(HasItemRequirement::ingredients)
	).apply(i, HasItemRequirement::new));

	@Override
	public MapCodec<HasItemRequirement> codec() {
		return CODEC;
	}

	@Override
	public Class<EmptyTrigger> getTrigger() {
		return EmptyTrigger.class;
	}

	@Override
	public boolean canComplete(Player pl, QuestData data, String key) {
		return InventoryMapper.testCached(pl, this);
	}

	@Override
	public List<Component> getDesc(Player player, QuestData data, String key) {
		List<Component> ans = new ArrayList<>();
		for (var e : ingredients) {
			ans.add(e.getDesc(player));
		}
		if (ingredients.size() > 1) {
			boolean pass = InventoryMapper.testCached(player, this);
			ans.addFirst(pass ? GLLang.Quest.ITEM_SUBMIT_PASS.get() : GLLang.Quest.ITEM_SUBMIT_FAIL.get());
		}
		return ans;
	}

}
