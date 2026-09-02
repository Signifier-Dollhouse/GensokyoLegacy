package dev.xkmc.gensokyolegacy.content.rpg.quest;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.QuestRequirementData;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.RolledIngredientList;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.TreeMap;

@SerialClass
public class QuestData {

	@SerialField
	public int completed;

	@SerialField
	public boolean started;

	@SerialField
	public long lastCompletion;

	@SerialField
	public final TreeMap<String, Integer> progress = new TreeMap<>();

	@SerialField
	public final TreeMap<String, QuestRequirementData> requirementData = new TreeMap<>();

	public boolean isCompletable(Player sp, Quest quest) {
		if (!started) return false;
		for (var e : quest.requirements().entrySet()) {
			var req = e.getValue();
			if (req.getMaxProgress() > progress.getOrDefault(e.getKey(), 0))
				return false;
			if (!req.canComplete(sp, this, e.getKey()))
				return false;
		}
		return true;
	}

	public boolean hasStarted(Quest quest) {
		return started;
	}

	public boolean canStart(Player sp, Quest quest) {
		var opt = quest.recurrence();
		if (opt.isEmpty()) {
			return completed == 0;
		}
		long time = sp.level().getGameTime();
		return time < lastCompletion || lastCompletion <= 0 || time > lastCompletion + opt.get().cooldown();
	}

	public void start(ServerPlayer sp, Quest quest) {
		started = true;
		for (var e : quest.requirements().entrySet()) {
			e.getValue().start(this, sp, e.getKey());
		}
	}

	public void complete(ServerPlayer sp, Quest quest, YoukaiEntity ch) {
		for (var e : quest.requirements().entrySet()) {
			var req = e.getValue();
			req.doComplete(sp, this, e.getKey());
		}
		for (var e : quest.rewards()) {
			e.execute(sp, ch);
		}
		progress.clear();
		requirementData.clear();
		completed++;
		lastCompletion = sp.level().getGameTime();
		started = false;
	}

}
