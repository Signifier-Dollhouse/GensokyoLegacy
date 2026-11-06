package dev.xkmc.gensokyolegacy.content.attachment.character;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SerialClass
public class FeedModuleData {

	/**
	 * How many past feeds are remembered; the newest is at the head of the list.
	 */
	private static final int MEMORY = 10;

	/**
	 * Each older sighting of the same item counts this much less than the one before it.
	 */
	private static final double RATE = 0.9;

	@SerialField
	private final Map<Item, Integer> statistics = new LinkedHashMap<>();
	@SerialField
	private final List<Item> history = new ArrayList<>();

	/**
	 * Favour ratio for one feed of {@code food}: 1, falling toward 0 as the same item fills the memory.
	 */
	public double feed(ItemStack food) {
		double ans = 0;
		double val = 1;
		for (var e : history) {
			if (e == food.getItem()) {
				ans += val;
			}
			val *= RATE;
		}
		statistics.compute(food.getItem(), (k, v) -> (v == null ? 0 : v) + 1);
		history.addFirst(food.getItem());
		if (history.size() > MEMORY) history.removeLast();
		return 1 / (1 + ans);
	}

}
