package dev.xkmc.gensokyolegacy.init.data.rpg;

import com.tterrag.registrate.providers.ProviderType;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.DialogConfig;
import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.Dialog;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.SimpleDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.RollItemRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.reward.LootTableReward;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2core.init.reg.ench.DataGenHolder;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class QuestDialogData {

	private final Map<ResourceKey<Dialog>, DataGenHolder<Dialog>> dialogRegistry = new LinkedHashMap<>();
	private final Map<ResourceKey<DialogStarter>, DataGenHolder<DialogStarter>> starterRegistry = new LinkedHashMap<>();
	private final Map<ResourceKey<Quest>, DataGenHolder<Quest>> questRegistry = new LinkedHashMap<>();
	private final Map<ResourceKey<TradeOffer>, DataGenHolder<TradeOffer>> tradeRegistry = new LinkedHashMap<>();
	private final Map<EntityType<?>, DialogConfig> defaultDialogMap = new LinkedHashMap<>();

	private String prefix = "";

	private final String modid;
	private final L2Registrate reg;

	public QuestDialogData(String modid, L2Registrate reg) {
		this.modid = modid;
		this.reg = reg;
	}

	public QuestDialogData() {
		this(GensokyoLegacy.MODID, GensokyoLegacy.REGISTRATE);
	}

	/**
	 * Registers all content contributed by the given instances into each datapack registry exactly once.
	 */
	public static void build(L2Registrate reg, QuestDialogData... all) {
		reg.getDataGenInitializer().add(CodecRegistry.DIALOG.key(), ctx ->
				forAll(all, d -> d.dialogRegistry.forEach((k, v) -> ctx.register(k, v.value()))));
		reg.getDataGenInitializer().add(CodecRegistry.STARTER.key(), ctx ->
				forAll(all, d -> d.starterRegistry.forEach((k, v) -> ctx.register(k, v.value()))));
		reg.getDataGenInitializer().add(CodecRegistry.QUEST.key(), ctx ->
				forAll(all, d -> d.questRegistry.forEach((k, v) -> ctx.register(k, v.value()))));
		reg.getDataGenInitializer().add(CodecRegistry.TRADE.key(), ctx ->
				forAll(all, d -> d.tradeRegistry.forEach((k, v) -> ctx.register(k, v.value()))));
		reg.addDataGenerator(ProviderType.DATA_MAP, pvd -> {
			var builder = pvd.builder(GLMeta.DIALOG_DATA.reg());
			forAll(all, d -> d.defaultDialogMap.forEach((k, v) ->
					builder.add(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(k), v, false)));
		});
	}

	private static void forAll(QuestDialogData[] all, Consumer<QuestDialogData> c) {
		for (var d : all)
			c.accept(d);
	}

	public ResourceLocation loc(String id) {
		return ResourceLocation.fromNamespaceAndPath(modid, id);
	}

	public void prefix(String prefix) {
		this.prefix = prefix;
	}

	public String starterText(String id, String text) {
		return text("starter", id, text);
	}

	public String optionText(String id, String text) {
		return text("option", id, text);
	}

	public String reqText(String id, String text) {
		return text("requirement", id, text);
	}

	public String questTitle(String text) {
		return text("quest", "title", text);
	}

	public void defaultDialog(EntityType<?> type, String greetingText, String tradeText) {
		defaultDialogMap.put(type, new DialogConfig(
				dialogText("greeting", greetingText),
				dialogText("trade", tradeText)));
	}

	private String dialogText(String type, String text) {
		String full = modid + "/" + prefix + "/" + type;
		reg.addRawLang(full, text);
		return full;
	}

	public String questDesc(String text) {
		return text("quest", "desc", text);
	}

	private String text(String type, String id, String text) {
		String full = modid + "/" + prefix + "/" + type + "/" + id;
		reg.addRawLang(full, text);
		return full;
	}

	public Holder<Dialog> dialog(String id, String text, DialogOption<?>... options) {
		id = prefix + "/" + id;
		var key = ResourceKey.create(CodecRegistry.DIALOG.key(), loc(id));
		var holder = new DataGenHolder<>(key, new Dialog(text("dialog", id, text), List.of(options)));
		dialogRegistry.put(key, holder);
		return holder;
	}

	public Holder<DialogStarter> starter(String id, DialogStarter dialog) {
		var key = ResourceKey.create(CodecRegistry.STARTER.key(), loc(id));
		var holder = new DataGenHolder<>(key, dialog);
		starterRegistry.put(key, holder);
		return holder;
	}

	public Holder<Quest> quest(String id, Quest quest) {
		var key = ResourceKey.create(CodecRegistry.QUEST.key(), loc(id));
		var holder = new DataGenHolder<>(key, quest);
		questRegistry.put(key, holder);
		return holder;
	}

	public Holder<TradeOffer> trade(String id, TradeOffer offer) {
		var key = ResourceKey.create(CodecRegistry.TRADE.key(), loc(prefix + "/" + id));
		var holder = new DataGenHolder<>(key, offer);
		tradeRegistry.put(key, holder);
		return holder;
	}

	public Holder<TradeOffer> trade(String id, EntityType<?> character, ItemStack result,
	                                TradeRecurrence recurrence, IngredientEntry... ingredients) {
		return trade(id, new TradeOffer(character, List.of(), result, recurrence, List.of(ingredients)));
	}

	public LootTableReward loot(String id, LootTable.Builder loot) {
		var key = ResourceKey.create(Registries.LOOT_TABLE, loc(id));
		reg.addDataGenerator(ProviderType.LOOT, pvd -> pvd.addLootAction(
				LootContextParamSets.ADVANCEMENT_REWARD,
				c -> c.accept(key, loot)));
		return new LootTableReward(key.location());
	}

	public ResourceLocation requestTable(String id, LootTable.Builder loot) {
		var key = ResourceKey.create(Registries.LOOT_TABLE, loc("quest_req/" + prefix + "/" + id));
		reg.addDataGenerator(ProviderType.LOOT, pvd -> pvd.addLootAction(
				LootContextParamSets.ADVANCEMENT_REWARD,
				c -> c.accept(key, loot)));
		return key.location();
	}

	public RollItemRequirement rollItem(ResourceLocation table) {
		return new RollItemRequirement(table);
	}

	public SimpleDialogOption option(String id, String text) {
		return new SimpleDialogOption(List.of(), optionText(id, text), List.of(), Optional.empty());
	}

	public SimpleDialogOption option(String id, String text, DialogAction<?> action) {
		return new SimpleDialogOption(List.of(), optionText(id, text), List.of(action), Optional.empty());
	}

	public SimpleDialogOption option(String id, String text, Holder<Dialog> next) {
		return new SimpleDialogOption(List.of(), optionText(id, text), List.of(), Optional.of(next));
	}

	public SimpleDialogOption option(String id, String text, DialogAction<?> action, Holder<Dialog> next) {
		return new SimpleDialogOption(List.of(), optionText(id, text), List.of(action), Optional.of(next));
	}

	public IngredientEntry item(ItemLike item, int count) {
		return new IngredientEntry(Ingredient.of(item), count, Optional.empty());
	}

	public IngredientEntry itemTag(TagKey<Item> tag, int count) {
		return new IngredientEntry(Ingredient.of(tag), count, Optional.empty());
	}

	public LootPool.Builder lootItem(ItemLike item, int count) {
		return LootPool.lootPool().add(LootItem.lootTableItem(item)
				.apply(SetItemCountFunction.setCount(ConstantValue.exactly(count))));
	}

}
