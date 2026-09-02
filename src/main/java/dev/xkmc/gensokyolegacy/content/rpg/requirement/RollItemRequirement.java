package dev.xkmc.gensokyolegacy.content.rpg.requirement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestData;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.EmptyTrigger;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.util.InventoryMapper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

public record RollItemRequirement(
		ResourceLocation table
) implements QuestRequirement<RollItemRequirement, EmptyTrigger> {

	public static final MapCodec<RollItemRequirement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceLocation.CODEC.fieldOf("table").forGetter(RollItemRequirement::table)
	).apply(i, RollItemRequirement::new));

	@Override
	public MapCodec<RollItemRequirement> codec() {
		return CODEC;
	}

	@Override
	public Class<EmptyTrigger> getTrigger() {
		return EmptyTrigger.class;
	}

	@Override
	public int getMaxProgress() {
		return 1;
	}

	@Override
	public void start(QuestData data, ServerPlayer sp, String key) {
		var loot = sp.serverLevel().getServer().reloadableRegistries()
				.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, table));
		var params = new LootParams.Builder(sp.serverLevel())
				.withParameter(LootContextParams.THIS_ENTITY, sp)
				.withParameter(LootContextParams.ORIGIN, sp.position())
				.create(LootContextParamSets.ADVANCEMENT_REWARD);
		var stacks = new ArrayList<ItemStack>();
		loot.getRandomItems(params, stacks::add);
		data.requirementData.put(key, new RolledIngredientList(stacks));
	}

	@Override
	public boolean canComplete(Player pl, QuestData data, String key) {
		var obj = data.requirementData.get(key);
		if (!(obj instanceof RolledIngredientList list)) return true;
		if (list.rolled.isEmpty()) return true;
		return InventoryMapper.testCached(pl, list);
	}

	@Override
	public void doComplete(ServerPlayer sp, QuestData data, String key) {
		var obj = data.requirementData.get(key);
		if (!(obj instanceof RolledIngredientList list)) return;
		var ans = new InventoryMapper(sp.getInventory().items, list.ingredients());
		ans.test();
		ans.consume();
	}

	@Override
	public List<Component> getDesc(Player player, QuestData data, String key) {
		var obj = data.requirementData.get(key);
		if (!(obj instanceof RolledIngredientList list)) return List.of();
		var ingredients = list.ingredients();
		List<Component> ans = new ArrayList<>();
		for (var e : ingredients) {
			ans.add(e.getDesc(player));
		}
		if (ingredients.size() > 1) {
			boolean pass = new InventoryMapper(player.getInventory().items, ingredients).test();
			ans.addFirst(pass ? GLLang.Quest.ITEM_SUBMIT_PASS.get() : GLLang.Quest.ITEM_SUBMIT_FAIL.get());
		}
		return ans;
	}

}
