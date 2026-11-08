package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectEntry;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import dev.xkmc.gensokyolegacy.content.block.functional.treasure.TreasureBasketEffectData;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Expiring items in a treasure basket's chunks go into the nearest basket that still has room,
 * instead of despawning.
 *
 * @see "doc/design/treasure_basket.md"
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class TreasureBasketEventHandlers {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onItemExpire(ItemExpireEvent event) {
		ItemEntity item = event.getEntity();
		if (!(item.level() instanceof ServerLevel level)) return;
		// another mod may have extended the lifespan, so honour the post-event lifespan
		int lifespan = Mth.clamp(item.lifespan + event.getExtraLife(), 0, Short.MAX_VALUE - 1);
		if (item.getAge() < lifespan) return;
		ItemStack stack = item.getItem();
		if (stack.isEmpty()) return;
		List<AreaEffectEntry> baskets = new ArrayList<>();
		for (AreaEffectEntry entry : AreaEffectManager.getAffecting(level, item.chunkPosition())) {
			if (entry.data instanceof TreasureBasketEffectData) baskets.add(entry);
		}
		if (baskets.isEmpty()) return;
		Vec3 pos = item.position();
		baskets.sort(Comparator.comparingDouble(e -> pos.distanceToSqr(Vec3.atCenterOf(e.ownerPos))));
		ItemStack remain = stack.copy();
		for (AreaEffectEntry entry : baskets) {
			remain = ((TreasureBasketEffectData) entry.data).insert(remain);
			if (remain.isEmpty()) break;
		}
		if (remain.isEmpty()) item.discard();
		else if (remain.getCount() != stack.getCount()) item.setItem(remain);
	}

}
