package dev.xkmc.gensokyolegacy.content.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterData;
import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.network.QuestStatusToClient;
import dev.xkmc.gensokyolegacy.content.rpg.network.TradeStatusToClient;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestData;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ReputationReward;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeData;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GLCommands {

	private static final SimpleCommandExceptionType ERROR_UNKNOWN_CHARACTER =
			new SimpleCommandExceptionType(Component.literal("Unknown character entity type"));
	private static final SimpleCommandExceptionType ERROR_UNKNOWN_QUEST =
			new SimpleCommandExceptionType(Component.literal("Unknown quest"));
	private static final SimpleCommandExceptionType ERROR_QUEST_MISMATCH =
			new SimpleCommandExceptionType(Component.literal("Quest does not belong to this character"));
	private static final SimpleCommandExceptionType ERROR_UNKNOWN_TRADE =
			new SimpleCommandExceptionType(Component.literal("Unknown trade offer"));
	private static final SimpleCommandExceptionType ERROR_TRADE_MISMATCH =
			new SimpleCommandExceptionType(Component.literal("Trade offer does not belong to this character"));
	private static final SimpleCommandExceptionType ERROR_UNKNOWN_PROGRESS_KEY =
			new SimpleCommandExceptionType(Component.literal("Unknown quest requirement key"));

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_CHARACTER = (ctx, builder) -> {
		for (var entry : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
			if (!entry.getKey().location().getNamespace().equals(GensokyoLegacy.MODID)) continue;
			if (!(entry.getValue() instanceof EntityType<?> type)) continue;
			if (!YoukaiEntity.class.isAssignableFrom(type.getBaseClass())) continue;
			builder.suggest(entry.getKey().location().toString());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_ALL_QUESTS = (ctx, builder) -> {
		var src = ctx.getSource();
		var reg = src.registryAccess().registryOrThrow(CodecRegistry.Keys.QUEST);
		for (var key : reg.keySet()) {
			builder.suggest(key.toString());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_ALL_TRADES = (ctx, builder) -> {
		var src = ctx.getSource();
		var reg = src.registryAccess().registryOrThrow(CodecRegistry.Keys.TRADE);
		for (var key : reg.keySet()) {
			builder.suggest(key.toString());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_CHARACTER_TRADES = (ctx, builder) -> {
		var src = ctx.getSource();
		EntityType<?> character = null;
		try {
			character = getCharacter(ctx, "character");
		} catch (Exception ignored) {
		}
		var reg = src.registryAccess().registryOrThrow(CodecRegistry.Keys.TRADE);
		for (var entry : reg.entrySet()) {
			if (character != null && entry.getValue().character() != character) continue;
			builder.suggest(entry.getKey().location().toString());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_REQ_KEYS = (ctx, builder) -> {
		try {
			Holder<Quest> quest = getQuest(ctx, "quest");
			for (var key : quest.value().requirements().keySet()) {
				builder.suggest(key);
			}
		} catch (Exception ignored) {
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_CHARACTER_QUESTS = (ctx, builder) -> {
		var src = ctx.getSource();
		EntityType<?> character = null;
		try {
			character = getCharacter(ctx, "character");
		} catch (Exception ignored) {
		}
		var reg = src.registryAccess().registryOrThrow(CodecRegistry.Keys.QUEST);
		for (var entry : reg.entrySet()) {
			if (character != null && entry.getValue().character() != character) continue;
			builder.suggest(entry.getKey().location().toString());
		}
		return builder.buildFuture();
	};

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(literal("gensokyo").requires(src -> src.hasPermission(2))
				.then(Commands.literal("character")
						.then(Commands.argument("character", ResourceLocationArgument.id())
								.suggests(SUGGEST_CHARACTER)
								.then(Commands.literal("info")
										.then(Commands.argument("player", EntityArgument.player())
												.executes(GLCommands::executeCharacterInfo)))
								.then(Commands.literal("reset")
										.then(Commands.argument("player", EntityArgument.player())
												.executes(GLCommands::executeCharacterReset)))
								.then(Commands.literal("reputation")
										.then(Commands.argument("player", EntityArgument.player())
												.then(Commands.literal("set")
														.then(Commands.argument("value", IntegerArgumentType.integer(
																		ReputationConstants.MIN_REPUTATION, ReputationConstants.MAX_REPUTATION))
																.executes(GLCommands::executeRepSet)))
												.then(Commands.literal("setCap")
														.then(Commands.argument("value", IntegerArgumentType.integer(
																		0, ReputationConstants.MAX_REPUTATION))
																.executes(GLCommands::executeRepSetCap)))
												.then(Commands.literal("add")
														.then(Commands.argument("value", IntegerArgumentType.integer(
																		ReputationConstants.MIN_REPUTATION, ReputationConstants.MAX_REPUTATION))
																.executes(GLCommands::executeRepAdd)))
												.then(Commands.literal("reset")
														.executes(GLCommands::executeRepReset))))
								.then(Commands.literal("quest")
										.then(Commands.argument("player", EntityArgument.player())
												.then(Commands.argument("quest", ResourceLocationArgument.id())
														.suggests(SUGGEST_CHARACTER_QUESTS)
														.then(Commands.literal("info")
																.executes(GLCommands::executeCharacterQuestInfo))
														.then(Commands.literal("start")
																.executes(GLCommands::executeCharacterQuestStart))
														.then(Commands.literal("complete")
																.executes(GLCommands::executeCharacterQuestComplete))
														.then(Commands.literal("reset")
																.executes(GLCommands::executeCharacterQuestReset))
														.then(Commands.literal("progress")
																.then(Commands.argument("key", StringArgumentType.string())
																		.suggests(SUGGEST_REQ_KEYS)
																		.then(Commands.literal("set")
																				.then(Commands.argument("value", IntegerArgumentType.integer(0))
																						.executes(GLCommands::executeCharacterQuestProgressSet)))
																		.then(Commands.literal("add")
																				.then(Commands.argument("value", IntegerArgumentType.integer())
																						.executes(GLCommands::executeCharacterQuestProgressAdd))))))))
								.then(Commands.literal("trade")
										.then(Commands.argument("player", EntityArgument.player())
												.then(Commands.argument("offer", ResourceLocationArgument.id())
														.suggests(SUGGEST_CHARACTER_TRADES)
														.then(Commands.literal("info")
																.executes(GLCommands::executeCharacterTradeInfo))
														.then(Commands.literal("restock")
																.executes(GLCommands::executeCharacterTradeRestock)))))
								.then(Commands.literal("completeAll")
										.then(Commands.argument("player", EntityArgument.player())
												.executes(GLCommands::executeCharacterCompleteAll)))
								.then(Commands.literal("restockAll")
										.then(Commands.argument("player", EntityArgument.player())
												.executes(GLCommands::executeCharacterRestockAll)))))
				.then(Commands.literal("quest")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("quest", ResourceLocationArgument.id())
										.suggests(SUGGEST_ALL_QUESTS)
										.then(Commands.literal("info")
												.executes(GLCommands::executeQuestInfo))
										.then(Commands.literal("start")
												.executes(GLCommands::executeQuestStart))
										.then(Commands.literal("complete")
												.executes(GLCommands::executeQuestComplete))
										.then(Commands.literal("reset")
												.executes(GLCommands::executeQuestReset))
										.then(Commands.literal("progress")
												.then(Commands.argument("key", StringArgumentType.string())
														.suggests(SUGGEST_REQ_KEYS)
														.then(Commands.literal("set")
																.then(Commands.argument("value", IntegerArgumentType.integer(0))
																		.executes(GLCommands::executeQuestProgressSet)))
														.then(Commands.literal("add")
																.then(Commands.argument("value", IntegerArgumentType.integer())
																		.executes(GLCommands::executeQuestProgressAdd))))))))
				.then(Commands.literal("trade")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("offer", ResourceLocationArgument.id())
										.suggests(SUGGEST_ALL_TRADES)
										.then(Commands.literal("info")
												.executes(GLCommands::executeTradeInfo))
										.then(Commands.literal("restock")
												.executes(GLCommands::executeTradeRestock)))
								.then(Commands.literal("restockAll")
										.executes(GLCommands::executeTradeRestockAll))))
				.then(Commands.literal("completeAll")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(GLCommands::executeCompleteAll)))
				.then(Commands.literal("resetAll")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(GLCommands::executeResetAll))));
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literal(String str) {
		return Commands.literal(str);
	}

	private static EntityType<?> getCharacter(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
		ResourceLocation id = ResourceLocationArgument.getId(ctx, name);
		if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) throw ERROR_UNKNOWN_CHARACTER.create();
		return BuiltInRegistries.ENTITY_TYPE.get(id);
	}

	private static Holder<Quest> getQuest(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
		ResourceLocation id = ResourceLocationArgument.getId(ctx, name);
		var reg = ctx.getSource().registryAccess().registryOrThrow(CodecRegistry.Keys.QUEST);
		var holder = reg.getHolder(ResourceKey.create(CodecRegistry.Keys.QUEST, id));
		if (holder.isEmpty()) throw ERROR_UNKNOWN_QUEST.create();
		return holder.get();
	}

	private static Holder<TradeOffer> getTrade(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
		ResourceLocation id = ResourceLocationArgument.getId(ctx, name);
		var reg = ctx.getSource().registryAccess().registryOrThrow(CodecRegistry.Keys.TRADE);
		var holder = reg.getHolder(ResourceKey.create(CodecRegistry.Keys.TRADE, id));
		if (holder.isEmpty()) throw ERROR_UNKNOWN_TRADE.create();
		return holder.get();
	}

	private static ResourceLocation questId(Holder<Quest> quest) {
		return quest.unwrapKey().orElseThrow().location();
	}

	private static ResourceLocation tradeId(Holder<TradeOffer> offer) {
		return offer.unwrapKey().orElseThrow().location();
	}

	private static CharDataHolder charHolder(ServerPlayer sp, EntityType<?> type) {
		return GLMeta.CHAR.type().getOrCreate(sp).getUnbounded(sp, type);
	}

	// --- reputation ---

	private static int executeRepSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		int value = IntegerArgumentType.getInteger(ctx, "value");
		var holder = charHolder(sp, type);
		int old = holder.data().reputation;
		holder.data().reputation = Math.clamp(value, ReputationConstants.MIN_REPUTATION, ReputationConstants.MAX_REPUTATION);
		holder.sync();
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Set reputation of " + type.toShortString() + " for " + sp.getGameProfile().getName() +
						": " + old + " -> " + holder.data().reputation + " (cap " + holder.data().reputationCap + ")"), true);
		return 1;
	}

	private static int executeRepSetCap(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		int value = IntegerArgumentType.getInteger(ctx, "value");
		var holder = charHolder(sp, type);
		int old = holder.data().reputationCap;
		holder.data().reputationCap = Math.clamp(value, 0, ReputationConstants.MAX_REPUTATION);
		holder.sync();
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Set reputation cap of " + type.toShortString() + " for " + sp.getGameProfile().getName() +
						": " + old + " -> " + holder.data().reputationCap + " (rep " + holder.data().reputation + ")"), true);
		return 1;
	}

	private static int executeRepAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		int value = IntegerArgumentType.getInteger(ctx, "value");
		var holder = charHolder(sp, type);
		int old = holder.data().reputation;
		int rep;
		if (value >= 0) {
			rep = Math.min(old + value, Math.min(holder.data().reputationCap, ReputationConstants.MAX_REPUTATION));
		} else {
			rep = Math.max(old + value, ReputationConstants.MIN_REPUTATION);
		}
		holder.data().reputation = rep;
		holder.sync();
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Added " + value + " reputation of " + type.toShortString() + " for " + sp.getGameProfile().getName() +
						": " + old + " -> " + rep), true);
		return 1;
	}

	private static int executeRepReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		resetReputation(sp, type);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Reset reputation of " + type.toShortString() + " for " + sp.getGameProfile().getName() +
						" to " + ReputationConstants.INITIAL_REPUTATION + "/" + ReputationConstants.INITIAL_CAP), true);
		return 1;
	}

	private static void resetReputation(ServerPlayer sp, EntityType<?> type) {
		GLMeta.CHAR.type().getOrCreate(sp).reset(type);
		charHolder(sp, type).sync();
	}

	// --- quest ---

	/**
	 * Force-complete: set bookkeeping and run rewards, but never call
	 * requirement doComplete, so submit-item requirements consume nothing.
	 */
	private static void forceComplete(ServerPlayer sp, Holder<Quest> quest) {
		ResourceLocation id = questId(quest);
		var quests = GLMeta.QUEST.type().getOrCreate(sp);
		var data = quests.getData(id);
		data.started = false;
		data.progress.clear();
		data.requirementData.clear();
		for (var reward : quest.value().rewards()) {
			if (reward instanceof ReputationReward r) {
				charHolder(sp, quest.value().character()).gain(r.reputation(), r.softCap(), r.capIncrease(), r.maxCap());
			} else {
				reward.execute(sp, null);
			}
		}
		data.completed++;
		data.lastCompletion = sp.level().getGameTime();
		data.started = false;
		GensokyoLegacy.HANDLER.toClientPlayer(new QuestStatusToClient(id, data, QuestStatusToClient.Reason.COMPLETE), sp);
	}

	/**
	 * Reset quest to never-started and deduct all completions' reputation
	 * and cap gains, floored at minimums.
	 */
	private static void resetQuest(ServerPlayer sp, Holder<Quest> quest) {
		ResourceLocation id = questId(quest);
		var quests = GLMeta.QUEST.type().getOrCreate(sp);
		var data = quests.data.get(id);
		int times = data == null ? 0 : data.completed;
		if (times > 0) {
			int repSum = 0, capSum = 0;
			for (var reward : quest.value().rewards()) {
				if (reward instanceof ReputationReward r) {
					repSum += r.reputation();
					capSum += r.capIncrease();
				}
			}
			if (repSum != 0 || capSum != 0) {
				var holder = charHolder(sp, quest.value().character());
				CharacterData chData = holder.data();
				if (repSum != 0)
					chData.reputation = Math.max(chData.reputation - repSum * times, ReputationConstants.MIN_REPUTATION);
				if (capSum != 0)
					chData.reputationCap = Math.max(chData.reputationCap - capSum * times, ReputationConstants.INITIAL_CAP);
				holder.sync();
			}
		}
		if (data != null) {
			quests.data.remove(id);
			GensokyoLegacy.HANDLER.toClientPlayer(
					new QuestStatusToClient(id, new QuestData(), QuestStatusToClient.Reason.UPDATE), sp);
		}
	}

	private static int executeQuestComplete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		forceComplete(sp, quest);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Force-completed quest " + questId(quest) + " for " + sp.getGameProfile().getName() +
						" (no items consumed)"), true);
		return 1;
	}

	private static int executeQuestReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		resetQuest(sp, quest);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Reset quest " + questId(quest) + " for " + sp.getGameProfile().getName() +
						" (deducted all completions' reputation)"), true);
		return 1;
	}

	private static int executeCharacterQuestComplete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		if (quest.value().character() != type) throw ERROR_QUEST_MISMATCH.create();
		forceComplete(sp, quest);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Force-completed quest " + questId(quest) + " for " + sp.getGameProfile().getName() +
						" (no items consumed)"), true);
		return 1;
	}

	private static int executeCharacterQuestReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		if (quest.value().character() != type) throw ERROR_QUEST_MISMATCH.create();
		resetQuest(sp, quest);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Reset quest " + questId(quest) + " for " + sp.getGameProfile().getName() +
						" (deducted all completions' reputation)"), true);
		return 1;
	}

	private static List<Holder<Quest>> allQuests(CommandSourceStack src) {
		return src.registryAccess().registryOrThrow(CodecRegistry.Keys.QUEST).holders()
				.map(e -> (Holder<Quest>) e).toList();
	}

	private static int completeAll(CommandSourceStack src, ServerPlayer sp, EntityType<?> filter) {
		int count = 0;
		for (var quest : allQuests(src)) {
			if (filter != null && quest.value().character() != filter) continue;
			forceComplete(sp, quest);
			count++;
		}
		return count;
	}

	private static int executeCharacterCompleteAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		int count = completeAll(ctx.getSource(), sp, type);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Force-completed " + count + " quests of " + type.toShortString() +
						" for " + sp.getGameProfile().getName()), true);
		return count;
	}

	private static int executeCompleteAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		int count = completeAll(ctx.getSource(), sp, null);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Force-completed " + count + " quests for " + sp.getGameProfile().getName()), true);
		return count;
	}

	// --- reset ---

	private static void clearTradesFor(ServerPlayer sp, EntityType<?> type) {
		var tradeReg = sp.level().registryAccess().registryOrThrow(CodecRegistry.Keys.TRADE);
		var trades = GLMeta.TRADE.type().getOrCreate(sp);
		List<ResourceLocation> ids = new ArrayList<>();
		for (var entry : tradeReg.entrySet()) {
			if (entry.getValue().character() != type) continue;
			ids.add(entry.getKey().location());
		}
		for (var id : ids) {
			if (trades.data.remove(id) != null) {
				GensokyoLegacy.HANDLER.toClientPlayer(new TradeStatusToClient(id, new TradeData()), sp);
			}
		}
	}

	private static int removeQuestsFor(ServerPlayer sp, CommandSourceStack src, EntityType<?> type) {
		var quests = GLMeta.QUEST.type().getOrCreate(sp);
		List<ResourceLocation> ids = new ArrayList<>();
		for (var quest : allQuests(src)) {
			if (quest.value().character() != type) continue;
			ids.add(questId(quest));
		}
		int count = 0;
		for (var id : ids) {
			if (quests.data.remove(id) != null) {
				GensokyoLegacy.HANDLER.toClientPlayer(
						new QuestStatusToClient(id, new QuestData(), QuestStatusToClient.Reason.UPDATE), sp);
				count++;
			}
		}
		return count;
	}

	private static int executeCharacterReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		resetReputation(sp, type);
		int quests = removeQuestsFor(sp, ctx.getSource(), type);
		clearTradesFor(sp, type);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Reset character " + type.toShortString() + " for " + sp.getGameProfile().getName() +
						" (reputation, " + quests + " quests, trades, timers)"), true);
		return 1;
	}

	private static int executeResetAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		var chars = GLMeta.CHAR.type().getOrCreate(sp);
		List<EntityType<?>> types = new ArrayList<>(chars.allTypes());
		for (var type : types) {
			chars.reset(type);
			charHolder(sp, type).sync();
		}
		var quests = GLMeta.QUEST.type().getOrCreate(sp);
		List<ResourceLocation> questIds = new ArrayList<>(quests.data.keySet());
		for (var id : questIds) {
			quests.data.remove(id);
			GensokyoLegacy.HANDLER.toClientPlayer(
					new QuestStatusToClient(id, new QuestData(), QuestStatusToClient.Reason.UPDATE), sp);
		}
		var trades = GLMeta.TRADE.type().getOrCreate(sp);
		List<ResourceLocation> tradeIds = new ArrayList<>(trades.data.keySet());
		for (var id : tradeIds) {
			trades.data.remove(id);
			GensokyoLegacy.HANDLER.toClientPlayer(new TradeStatusToClient(id, new TradeData()), sp);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Reset all characters for " + sp.getGameProfile().getName() +
						" (" + types.size() + " reputations, " + questIds.size() + " quests, " +
						tradeIds.size() + " trades)"), true);
		return 1;
	}

	// --- viewing ---

	private static int executeCharacterInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		var holder = charHolder(sp, type);
		CharacterData data = holder.data();
		long now = sp.level().getGameTime();
		StringBuilder sb = new StringBuilder();
		sb.append("Character ").append(type.toShortString()).append(" for ").append(sp.getGameProfile().getName());
		sb.append("\nRep ").append(data.reputation).append("/").append(data.reputationCap);
		sb.append(" (").append(CharacterData.getState(data.reputation)).append(")");
		if (data.timers.isEmpty()) {
			sb.append("\nTimers: none");
		} else {
			sb.append("\nTimers:");
			for (var e : data.timers.entrySet()) {
				long left = Math.max(0, e.getValue() - now);
				sb.append("\n- ").append(e.getKey()).append(": ");
				sb.append(left <= 0 ? "expired" : left + " ticks (" + left / 20 + "s)");
			}
		}
		var quests = GLMeta.QUEST.type().getOrCreate(sp);
		int started = 0, completed = 0, total = 0;
		for (var quest : allQuests(ctx.getSource())) {
			if (quest.value().character() != type) continue;
			total++;
			var qd = quests.data.get(questId(quest));
			if (qd == null) continue;
			completed += qd.completed;
			if (qd.started) started++;
		}
		sb.append("\nQuests: ").append(total).append(" total, ").append(started).append(" started, ")
				.append(completed).append(" completions");
		var trades = GLMeta.TRADE.type().getOrCreate(sp);
		var tradeReg = sp.level().registryAccess().registryOrThrow(CodecRegistry.Keys.TRADE);
		int offers = 0, depleted = 0;
		for (var entry : tradeReg.entrySet()) {
			if (entry.getValue().character() != type) continue;
			offers++;
			var ref = tradeReg.getHolderOrThrow(entry.getKey());
			if (trades.getRemainingTrades(sp, ref) <= 0) depleted++;
		}
		sb.append("\nTrades: ").append(offers).append(" offers, ").append(depleted).append(" sold out");
		String out = sb.toString();
		ctx.getSource().sendSuccess(() -> Component.literal(out), false);
		return 1;
	}

	private static String describeQuest(CommandSourceStack src, ServerPlayer sp, Holder<Quest> quest) {
		var value = quest.value();
		StringBuilder sb = new StringBuilder();
		sb.append("Quest ").append(questId(quest));
		sb.append("\nTitle: ").append(value.title());
		sb.append("\nCharacter: ").append(value.character().toShortString());
		sb.append("\nType: ").append(value.recurrence()
				.map(r -> "repeatable, cooldown " + r.cooldown() + " ticks (" + r.cooldown() / 20 + "s)")
				.orElse("one-shot"));
		sb.append("\nConditions: ").append(value.conditions().size());
		sb.append("\nRewards:");
		for (var r : value.rewards()) {
			sb.append("\n- ").append(r);
		}
		var quests = GLMeta.QUEST.type().getOrCreate(sp);
		QuestData data = quests.data.getOrDefault(questId(quest), new QuestData());
		long now = sp.level().getGameTime();
		sb.append("\nState: started=").append(data.started)
				.append(", completed=").append(data.completed)
				.append(", lastCompletion=").append(data.lastCompletion)
				.append(" (now ").append(now).append(")");
		sb.append("\nRequirements:");
		for (var e : value.requirements().entrySet()) {
			int max = Math.max(0, e.getValue().getMaxProgress());
			int cur = data.progress.getOrDefault(e.getKey(), 0);
			sb.append("\n- ").append(e.getKey()).append(": ").append(cur).append("/").append(max);
			if (data.requirementData.containsKey(e.getKey()))
				sb.append(" [rolled]");
			for (var line : e.getValue().getDesc(sp, data, e.getKey())) {
				sb.append("\n  ").append(line.getString());
			}
		}
		return sb.toString();
	}

	private static int infoQuest(CommandSourceStack src, ServerPlayer sp, Holder<Quest> quest) {
		String out = describeQuest(src, sp, quest);
		src.sendSuccess(() -> Component.literal(out), false);
		return 1;
	}

	private static int executeQuestInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		return infoQuest(ctx.getSource(), sp, quest);
	}

	private static int executeCharacterQuestInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		if (quest.value().character() != type) throw ERROR_QUEST_MISMATCH.create();
		return infoQuest(ctx.getSource(), sp, quest);
	}

	private static String describeTrade(ServerPlayer sp, Holder<TradeOffer> offer) {
		var value = offer.value();
		var trades = GLMeta.TRADE.type().getOrCreate(sp);
		StringBuilder sb = new StringBuilder();
		sb.append("Trade ").append(tradeId(offer));
		sb.append("\nCharacter: ").append(value.character().toShortString());
		sb.append("\nResult: ").append(value.result().getHoverName().getString())
				.append(" x").append(value.result().getCount());
		sb.append("\nIngredients:");
		for (var ing : value.ingredients()) {
			sb.append("\n  ").append(ing.getDesc(sp).getString());
		}
		sb.append("\nStock: ").append(trades.getRemainingTrades(sp, offer))
				.append("/").append(trades.getMaxTrades(offer));
		sb.append(", restockTime ").append(value.recurrence().restockTime())
				.append(" ticks (").append(value.recurrence().restockTime() / 20).append("s)");
		var data = trades.data.get(tradeId(offer));
		if (data == null) {
			sb.append("\nState: never traded (full stock)");
		} else {
			long left = data.timestamp <= 0 ? 0 :
					Math.max(0, value.recurrence().restockTime() - (sp.level().getGameTime() - data.timestamp));
			sb.append("\nState: timestamp=").append(data.timestamp)
					.append(", restock in ").append(left).append(" ticks");
		}
		sb.append("\nConditions: ").append(value.conditions().size());
		return sb.toString();
	}

	private static int infoTrade(CommandSourceStack src, ServerPlayer sp, Holder<TradeOffer> offer) {
		String out = describeTrade(sp, offer);
		src.sendSuccess(() -> Component.literal(out), false);
		return 1;
	}

	private static int executeTradeInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<TradeOffer> offer = getTrade(ctx, "offer");
		return infoTrade(ctx.getSource(), sp, offer);
	}

	private static int executeCharacterTradeInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<TradeOffer> offer = getTrade(ctx, "offer");
		if (offer.value().character() != type) throw ERROR_TRADE_MISMATCH.create();
		return infoTrade(ctx.getSource(), sp, offer);
	}

	// --- quest progress micro-management ---

	private static void syncQuest(ServerPlayer sp, ResourceLocation id, QuestData data) {
		GensokyoLegacy.HANDLER.toClientPlayer(new QuestStatusToClient(id, data, QuestStatusToClient.Reason.UPDATE), sp);
	}

	private static void forceStart(ServerPlayer sp, Holder<Quest> quest) {
		ResourceLocation id = questId(quest);
		var data = GLMeta.QUEST.type().getOrCreate(sp).getData(id);
		data.started = true;
		for (var e : quest.value().requirements().entrySet()) {
			e.getValue().start(data, sp, e.getKey());
		}
		GensokyoLegacy.HANDLER.toClientPlayer(new QuestStatusToClient(id, data, QuestStatusToClient.Reason.START), sp);
	}

	private static int startQuest(CommandSourceStack src, ServerPlayer sp, Holder<Quest> quest) {
		forceStart(sp, quest);
		src.sendSuccess(() -> Component.literal(
				"Force-started quest " + questId(quest) + " for " + sp.getGameProfile().getName()), true);
		return 1;
	}

	private static int executeQuestStart(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		return startQuest(ctx.getSource(), sp, quest);
	}

	private static int executeCharacterQuestStart(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		if (quest.value().character() != type) throw ERROR_QUEST_MISMATCH.create();
		return startQuest(ctx.getSource(), sp, quest);
	}

	private static int setProgress(CommandSourceStack src, ServerPlayer sp, Holder<Quest> quest, String key, int value) throws CommandSyntaxException {
		var req = quest.value().requirements().get(key);
		if (req == null) throw ERROR_UNKNOWN_PROGRESS_KEY.create();
		int max = Math.max(0, req.getMaxProgress());
		var data = GLMeta.QUEST.type().getOrCreate(sp).getData(questId(quest));
		int old = data.progress.getOrDefault(key, 0);
		boolean autoStarted = false;
		if (!data.started) {
			data.started = true;
			autoStarted = true;
		}
		int v = Math.clamp(value, 0, max);
		data.progress.put(key, v);
		syncQuest(sp, questId(quest), data);
		String msg = "Set progress " + questId(quest) + "[" + key + "] for " + sp.getGameProfile().getName() +
				": " + old + " -> " + v + "/" + max + (autoStarted ? " (auto-started)" : "");
		src.sendSuccess(() -> Component.literal(msg), true);
		return 1;
	}

	private static int addProgress(CommandSourceStack src, ServerPlayer sp, Holder<Quest> quest, String key, int delta) throws CommandSyntaxException {
		var req = quest.value().requirements().get(key);
		if (req == null) throw ERROR_UNKNOWN_PROGRESS_KEY.create();
		int max = Math.max(0, req.getMaxProgress());
		var data = GLMeta.QUEST.type().getOrCreate(sp).getData(questId(quest));
		int old = data.progress.getOrDefault(key, 0);
		return setProgress(src, sp, quest, key, old + delta);
	}

	private static int executeQuestProgressSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		String key = StringArgumentType.getString(ctx, "key");
		int value = IntegerArgumentType.getInteger(ctx, "value");
		return setProgress(ctx.getSource(), sp, quest, key, value);
	}

	private static int executeQuestProgressAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		String key = StringArgumentType.getString(ctx, "key");
		int value = IntegerArgumentType.getInteger(ctx, "value");
		return addProgress(ctx.getSource(), sp, quest, key, value);
	}

	private static int executeCharacterQuestProgressSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		if (quest.value().character() != type) throw ERROR_QUEST_MISMATCH.create();
		String key = StringArgumentType.getString(ctx, "key");
		int value = IntegerArgumentType.getInteger(ctx, "value");
		return setProgress(ctx.getSource(), sp, quest, key, value);
	}

	private static int executeCharacterQuestProgressAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<Quest> quest = getQuest(ctx, "quest");
		if (quest.value().character() != type) throw ERROR_QUEST_MISMATCH.create();
		String key = StringArgumentType.getString(ctx, "key");
		int value = IntegerArgumentType.getInteger(ctx, "value");
		return addProgress(ctx.getSource(), sp, quest, key, value);
	}

	// --- trade restock ---

	private static List<Holder<TradeOffer>> allTrades(CommandSourceStack src) {
		return src.registryAccess().registryOrThrow(CodecRegistry.Keys.TRADE).holders()
				.map(e -> (Holder<TradeOffer>) e).toList();
	}

	private static int restockOffer(ServerPlayer sp, Holder<TradeOffer> offer) {
		ResourceLocation id = tradeId(offer);
		var trades = GLMeta.TRADE.type().getOrCreate(sp);
		TradeData fresh = new TradeData();
		trades.replace(id, fresh);
		GensokyoLegacy.HANDLER.toClientPlayer(new TradeStatusToClient(id, fresh), sp);
		return trades.getMaxTrades(offer);
	}

	private static int restockAllTrades(CommandSourceStack src, ServerPlayer sp, EntityType<?> filter) {
		int count = 0;
		for (var offer : allTrades(src)) {
			if (filter != null && offer.value().character() != filter) continue;
			restockOffer(sp, offer);
			count++;
		}
		return count;
	}

	private static int executeTradeRestock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<TradeOffer> offer = getTrade(ctx, "offer");
		int max = restockOffer(sp, offer);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Restocked trade " + tradeId(offer) + " for " + sp.getGameProfile().getName() +
						" (" + max + " in stock)"), true);
		return 1;
	}

	private static int executeCharacterTradeRestock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		Holder<TradeOffer> offer = getTrade(ctx, "offer");
		if (offer.value().character() != type) throw ERROR_TRADE_MISMATCH.create();
		int max = restockOffer(sp, offer);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Restocked trade " + tradeId(offer) + " for " + sp.getGameProfile().getName() +
						" (" + max + " in stock)"), true);
		return 1;
	}

	private static int executeTradeRestockAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		int count = restockAllTrades(ctx.getSource(), sp, null);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Restocked " + count + " trades for " + sp.getGameProfile().getName()), true);
		return count;
	}

	private static int executeCharacterRestockAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = getCharacter(ctx, "character");
		ServerPlayer sp = EntityArgument.getPlayer(ctx, "player");
		int count = restockAllTrades(ctx.getSource(), sp, type);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Restocked " + count + " trades of " + type.toShortString() +
						" for " + sp.getGameProfile().getName()), true);
		return count;
	}

}
