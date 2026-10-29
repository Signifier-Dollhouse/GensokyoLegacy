package dev.xkmc.gensokyolegacy.content.attachment.character;


import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.l2core.capability.player.PlayerCapabilityTemplate;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@SerialClass
public class CharacterAttachment extends PlayerCapabilityTemplate<CharacterAttachment> {

	/**
	 * Shifts the day rollover from dawn to midnight (tick 18000), so a skipped night is caught on the next
	 * morning when the day time jumps forward.
	 */
	private static final long MIDNIGHT_SHIFT = 6000L;

	/**
	 * Upper bound on day steps settled in one tick; only a clock jump can exceed it.
	 */
	private static final int MAX_DAILY_STEPS = 1000;

	/**
	 * Day of the last daily update, or -1 before the first login. Persisted so offline days can be settled
	 * on the next login.
	 */
	@SerialField
	private long lastDailyDay = -1;

	@SerialField
	private final Map<EntityType<?>, CharacterData> characterData = new LinkedHashMap<>();

	@Override
	public void tick(Player player) {
		if (player instanceof ServerPlayer sp) {
			updateDay(sp);
		}
	}

	/**
	 * Settles every day change since {@link #lastDailyDay}: positive decay once, then one negative
	 * recovery step per elapsed day.
	 */
	private void updateDay(ServerPlayer player) {
		long day = dayIndex(player);
		if (lastDailyDay < 0) {
			lastDailyDay = day;
			return;
		}
		if (day < lastDailyDay) {
			lastDailyDay = day;
			return;
		}
		if (day == lastDailyDay) return;
		int passed = (int) Math.min(day - lastDailyDay, MAX_DAILY_STEPS);
		lastDailyDay = day;
		for (var e : characterData.values()) {
			e.dailyUpdate(passed);
		}
	}

	private static long dayIndex(ServerPlayer player) {
		return (player.server.overworld().getDayTime() + MIDNIGHT_SHIFT) / Level.TICKS_PER_DAY;
	}

	public CharDataHolder get(Player player, YoukaiEntity e) {
		var val = characterData.computeIfAbsent(e.getType(), k -> new CharacterData());
		return new CharDataHolder(val, player, e.getType(), e);
	}

	public CharDataHolder getUnbounded(Player player, EntityType<?> e) {
		var val = characterData.computeIfAbsent(e, k -> new CharacterData());
		return new CharDataHolder(val, player, e, null);
	}

	public void replace(EntityType<?> target, CharacterData data) {
		characterData.put(target, data);
	}

	public void reset(EntityType<?> target) {
		characterData.put(target, new CharacterData());
	}

	public Set<EntityType<?>> allTypes() {
		return new LinkedHashSet<>(characterData.keySet());
	}

	public void clearAll() {
		characterData.clear();
	}
}
