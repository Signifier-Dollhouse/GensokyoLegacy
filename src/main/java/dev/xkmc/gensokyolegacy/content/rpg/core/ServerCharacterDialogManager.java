package dev.xkmc.gensokyolegacy.content.rpg.core;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.handle.*;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2core.init.reg.datapack.DatapackReg;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ServerCharacterDialogManager {

	public static final Map<EntityType<?>, ServerCharacterDialogManager> MAP = new LinkedHashMap<>();

	public static void clearCache() {
		MAP.clear();
	}

	public static ServerCharacterDialogManager get(ServerLevel sl, EntityType<?> type) {
		return MAP.computeIfAbsent(type, k -> new ServerCharacterDialogManager(sl.registryAccess(), k));
	}

	private static <T extends CharacterEntry> List<Holder<T>> getAllMatching(RegistryAccess pvd, DatapackReg<T> reg, EntityType<?> ch) {
		return reg.getAll(pvd).filter(e -> e.value().character() == ch)
				.sorted(Comparator.comparing(e -> e.unwrapKey().orElseThrow())).toList();
	}

	private final RegistryAccess pvd;
	private final EntityType<?> character;

	private final List<Holder<DialogStarter>> dialogs;
	private final List<Holder<Quest>> quests;
	private final List<Holder<TradeOffer>> offers;

	public ServerCharacterDialogManager(RegistryAccess pvd, EntityType<?> character) {
		this.pvd = pvd;
		this.character = character;
		dialogs = getAllMatching(pvd, CodecRegistry.STARTER, character);
		quests = getAllMatching(pvd, CodecRegistry.QUEST, character);
		offers = getAllMatching(pvd, CodecRegistry.TRADE, character);
	}

	public List<Holder<TradeOffer>> getTradeOffers(ServerPlayer sp, YoukaiEntity ch) {
		List<Holder<TradeOffer>> ans = new ArrayList<>();
		for (var e : offers) {
			if (e.value().match(sp, ch)) {
				ans.add(e);
			}
		}
		return ans;
	}

	/**
	 * Pick one unlocked chat by weight. Default chat has weight 1, regular
	 * informative chats have weight 100, special item-related chats have weight 1000.
	 */
	@Nullable
	public Holder<DialogStarter> pickChat(ServerPlayer sp, YoukaiEntity ch) {
		int total = 0;
		for (var e : dialogs) {
			if (e.value().match(sp, ch))
				total += Math.max(0, e.value().weight());
		}
		if (total <= 0) return null;
		int roll = sp.getRandom().nextInt(total);
		for (var e : dialogs) {
			if (!e.value().match(sp, ch)) continue;
			roll -= Math.max(0, e.value().weight());
			if (roll < 0) return e;
		}
		return null;
	}

	public List<IDialogHandle> getInitialConversation(ServerPlayer sp, YoukaiEntity ch) {
		List<IDialogHandle> ans = new ArrayList<>();
		Holder<DialogStarter> chat = pickChat(sp, ch);
		if (chat != null)
			ans.add(new DialogHandle(chat));
		var questData = GLMeta.QUEST.type().getOrCreate(sp);
		for (var e : quests) {
			var data = questData.getData(e.unwrapKey().orElseThrow().location());
			if (data.isCompletable(sp, e.value()))
				ans.add(new QuestHandle(e, e.value().completionDialog(), QuestHandle.Kind.COMPLETE));
			else if (data.hasStarted(e.value()))
				ans.add(new QuestHandle(e, e.value().followUpDialog(), QuestHandle.Kind.FOLLOW_UP));
			else if (data.canStart(sp, e.value()) && e.value().match(sp, ch))
				ans.add(new QuestHandle(e, e.value().initialDialog(), QuestHandle.Kind.START));
		}
		groupHandles(ans);
		if (!getTradeOffers(sp, ch).isEmpty())
			ans.add(new TradeHandle(ch.getType()));
		return ans;
	}

	private static void groupHandles(List<IDialogHandle> ans) {
		LinkedHashMap<String, List<IDialogHandle>> map = new LinkedHashMap<>();
		for (var e : ans) {
			String key = e.groupKey();
			if (key.isEmpty()) continue;
			map.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
		}
		map.entrySet().removeIf(e -> e.getValue().size() == 1);
		ans.removeIf(e -> map.containsKey(e.groupKey()));
		for (var e : map.entrySet()) {
			ans.add(new GroupHandle(e.getValue().getFirst().display(), e.getValue(), Component.translatable(e.getKey())));
		}
	}

}
