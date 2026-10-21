package dev.xkmc.gensokyolegacy.init.data.rpg;

import com.tterrag.registrate.providers.ProviderType;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.SetTimerAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.*;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.core.DialogConfig;
import dev.xkmc.gensokyolegacy.content.rpg.core.GiftDialogData;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.*;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
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
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.*;
import java.util.function.Consumer;

public class QuestDialogData {

	public static final int CHAT_DEFAULT = 1;
	public static final int CHAT_MISC = 30;
	public static final int CHAT_INFO = 100;
	public static final int CHAT_SPECIAL = 1000;

	/**
	 * Standard dialog animations: quest start/follow-up play think, completion plays agree. Chats leave
	 * the list empty so a random talk clip is picked at play time (see {@code GeoYoukaiAnim}).
	 */
	protected static final List<String> THINK_ANIMS = List.of(YoukaiAnim.THINK.trigger());
	protected static final List<String> AGREE_ANIMS = List.of(YoukaiAnim.AGREE.trigger());

	private final Map<ResourceKey<Dialog>, DataGenHolder<Dialog>> dialogRegistry = new LinkedHashMap<>();
	private final Map<ResourceKey<DialogStarter>, DataGenHolder<DialogStarter>> starterRegistry = new LinkedHashMap<>();
	private final Map<ResourceKey<Quest>, DataGenHolder<Quest>> questRegistry = new LinkedHashMap<>();
	private final Map<ResourceKey<TradeOffer>, DataGenHolder<TradeOffer>> tradeRegistry = new LinkedHashMap<>();
	private final Map<EntityType<?>, DialogConfig> defaultDialogMap = new LinkedHashMap<>();
	private final Map<EntityType<?>, GiftDialogData> giftDialogMap = new LinkedHashMap<>();
	private final Map<String, List<String>> questAnims = new LinkedHashMap<>();

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
			var gifts = pvd.builder(GLMeta.GIFT_DIALOG_DATA.reg());
			forAll(all, d -> d.giftDialogMap.forEach((k, v) ->
					gifts.add(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(k), v, false)));
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

	/**
	 * The animations this section's dialogs play, overriding what the shared quest helpers would
	 * otherwise stamp on all of them alike.
	 * <p>
	 * {@code start("dialog_think", "dialog_decline", "dialog_agree")} reads the {@code start} helper's
	 * three dialogs in the order it emits them — opener, refusal, acceptance — and {@code follow} and
	 * {@code complete} likewise. A helper with no entry here keeps its default, which is why the
	 * per-dialog choices below do not have to restate the ones that were already right.
	 */
	protected void questAnims(String helper, String... triggers) {
		// List.of rejects nulls, and a null slot is how a section says "leave this one alone"
		questAnims.put(prefix + "/" + helper, Arrays.asList(triggers));
	}

	/**
	 * What a shared helper stamps on one of its dialogs: this section's choice if it named one, else
	 * the helper's own default. A {@code null} in the override means "no choice, use the default", so
	 * a section can fill in only the slots it cares about.
	 */
	protected List<String> anims(String helper, int slot, List<String> fallback) {
		var triggers = questAnims.get(prefix + "/" + helper);
		if (triggers == null || slot >= triggers.size() || triggers.get(slot) == null) return fallback;
		return List.of(triggers.get(slot));
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

	public void defaultDialog(EntityType<?> type, String greetingText, String visitGreetingText, String tradeText) {
		defaultDialogMap.put(type, new DialogConfig(
				dialogText("greeting", greetingText),
				dialogText("visit_greeting", visitGreetingText),
				dialogText("trade", tradeText)));
	}

	/**
	 * What this character does about a gift: the two dialogs she answers it with, and the favor she has
	 * to owe before she hands one back. Held as holders rather than ids, so nothing resolves a path at
	 * runtime and a mistyped one fails at datagen.
	 */
	public void giftDialog(EntityType<?> type, Holder<Dialog> like, Holder<Dialog> dislike,
						   int threshold, ResourceLocation returnTable) {
		giftDialogMap.put(type, new GiftDialogData(like, dislike, threshold, returnTable));
	}

	/**
	 * The one line every gift reaction ends on, registered once however many characters use it. A
	 * per-character copy would differ only in the lang key, not in what it says.
	 */
	public SimpleDialogOption giftLeave() {
		reg.addRawLang(GIFT_LEAVE_KEY, "(Leave)");
		return new SimpleDialogOption(List.of(), GIFT_LEAVE_KEY, List.of(), Optional.empty());
	}

	public static final String GIFT_LEAVE_KEY = "gensokyolegacy/dialog/gift/leave";

	private String dialogText(String type, String text) {
		String full = modid + "/" + prefix + "/" + type;
		reg.addRawLang(full, text);
		return full;
	}

	/**
	 * The same, at a key spelled out whole rather than assembled from the prefix. {@link #dialog} can
	 * only make {@code prefix/dialog/prefix/id}, which spells the prefix twice; a section that already
	 * carries it in its own path says so through here instead.
	 */
	public String rawText(String full, String text) {
		reg.addRawLang(modid + "/" + full, text);
		return modid + "/" + full;
	}

	public String questDesc(String text) {
		return text("quest", "desc", text);
	}

	protected String text(String type, String id, String text) {
		String full = modid + "/" + prefix + "/" + type + "/" + id;
		reg.addRawLang(full, text);
		return full;
	}

	public Holder<Dialog> dialog(String id, String text, DialogOption<?>... options) {
		return dialog(id, text, List.of(), options);
	}

	public Holder<Dialog> dialog(String id, String text, List<String> animations, DialogOption<?>... options) {
		id = prefix + "/" + id;
		var key = ResourceKey.create(CodecRegistry.DIALOG.key(), loc(id));
		var holder = new DataGenHolder<>(key, new Dialog(text("dialog", id, text), List.of(options), animations));
		dialogRegistry.put(key, holder);
		return holder;
	}

	public Holder<Dialog> dialogKey(String id, String key, DialogOption<?>... options) {
		return dialogKey(id, key, List.of(), options);
	}

	public Holder<Dialog> dialogKey(String id, String key, List<String> animations, DialogOption<?>... options) {
		id = prefix + "/" + id;
		var rkey = ResourceKey.create(CodecRegistry.DIALOG.key(), loc(id));
		var holder = new DataGenHolder<>(rkey, new Dialog(key, List.of(options), animations));
		dialogRegistry.put(rkey, holder);
		return holder;
	}

	public Holder<DialogStarter> starter(String id, DialogStarter dialog) {
		var key = ResourceKey.create(CodecRegistry.STARTER.key(), loc(id));
		var holder = new DataGenHolder<>(key, dialog);
		starterRegistry.put(key, holder);
		return holder;
	}

	public Holder<DialogStarter> chat(String id, EntityType<?> character, List<QuestCondition<?>> conditions,
									  String text, Holder<Dialog> dialog, int weight) {
		return starter(id, new DialogStarter(character, conditions, text, dialog, weight, false));
	}

	/**
	 * A chat the character starts the conversation on rather than offering as a topic: no starter line,
	 * no weight, and out of the roll that picks the one chat per conversation. For something owed to the
	 * player that has to happen before anything else does.
	 */
	public Holder<DialogStarter> overrideChat(String id, EntityType<?> character, List<QuestCondition<?>> conditions,
											  Holder<Dialog> dialog) {
		return starter(id, new DialogStarter(character, conditions, "", dialog, 1, true));
	}

	protected HasAdvancementCondition hasAdv(ResourceLocation adv) {
		return new HasAdvancementCondition(adv);
	}

	protected HasAdvancementCondition missingAdv(ResourceLocation adv) {
		return new HasAdvancementCondition(adv, true);
	}

	protected HasQuestCompletedCondition hasQuest(ResourceLocation quest) {
		return new HasQuestCompletedCondition(quest);
	}

	protected HasItemCondition hasItem(IngredientEntry... entries) {
		return new HasItemCondition(List.of(entries));
	}

	protected TimerCondition timer(String key) {
		return new TimerCondition(key);
	}

	/**
	 * Content that only makes sense in the character's own home: the greeting
	 * chats, and trades that need her equipment or her shelves.
	 */
	protected HomeBoundCondition homeBound() {
		return new HomeBoundCondition();
	}

	/**
	 * Content that only makes sense while she is a guest somewhere else.
	 */
	protected HomeBoundCondition visiting() {
		return new HomeBoundCondition(true);
	}

	/**
	 * Content that only makes sense at this particular host home, which is how visit chat is written:
	 * a guest says different things in different rooms. Implies {@link #visiting()}.
	 */
	protected VisitStructureCondition visitingAt(ResourceLocation structure) {
		return new VisitStructureCondition(structure);
	}

	protected SetTimerAction setTimer(String key, int delay) {
		return new SetTimerAction(key, delay);
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

	public GroupDialogOption groupOption(String group, String id, String text) {
		return new GroupDialogOption(group, optionText(id, text), List.of(), Optional.empty());
	}

	public GroupDialogOption groupOption(String group, String id, String text, DialogAction<?> action) {
		return new GroupDialogOption(group, optionText(id, text), List.of(action), Optional.empty());
	}

	public GroupDialogOption groupOption(String group, String id, String text, Holder<Dialog> next) {
		return new GroupDialogOption(group, optionText(id, text), List.of(), Optional.of(next));
	}

	public GroupDialogOption groupKey(String group, String key, Holder<Dialog> next) {
		return new GroupDialogOption(group, key, List.of(), Optional.of(next));
	}

	public GroupDialogOption groupKey(String group, String key, DialogAction<?> action, Holder<Dialog> next) {
		return new GroupDialogOption(group, key, List.of(action), Optional.of(next));
	}

	public GroupDialogOption groupKey(String group, String key, List<DialogAction<?>> actions, Holder<Dialog> next) {
		return new GroupDialogOption(group, key, actions, Optional.of(next));
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

	public SimpleDialogOption optionKey(String key) {
		return new SimpleDialogOption(List.of(), key, List.of(), Optional.empty());
	}

	public SimpleDialogOption optionKey(String key, DialogAction<?> action) {
		return new SimpleDialogOption(List.of(), key, List.of(action), Optional.empty());
	}

	public SimpleDialogOption optionKey(String key, Holder<Dialog> next) {
		return new SimpleDialogOption(List.of(), key, List.of(), Optional.of(next));
	}

	public SimpleDialogOption optionKey(String key, DialogAction<?> action, Holder<Dialog> next) {
		return new SimpleDialogOption(List.of(), key, List.of(action), Optional.of(next));
	}

	public SimpleDialogOption optionKey(String key, List<DialogAction<?>> actions, Holder<Dialog> next) {
		return new SimpleDialogOption(List.of(), key, actions, Optional.of(next));
	}

	public SimpleDialogOption option(String id, String text, List<DialogAction<?>> actions, Holder<Dialog> next) {
		return new SimpleDialogOption(List.of(), optionText(id, text), actions, Optional.of(next));
	}

	public WeightedEntry weighted(int weight, Holder<Dialog> next, DialogAction<?>... actions) {
		return new WeightedEntry(weight, List.of(actions), Optional.of(next));
	}

	public RandomDialogOption randomOption(String id, String text, WeightedEntry... entries) {
		return new RandomDialogOption(List.of(), optionText(id, text), List.of(entries));
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

	public LootPool.Builder lootItem(ItemLike item, int min, int max) {
		return LootPool.lootPool().add(LootItem.lootTableItem(item)
				.apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max))));
	}

}
