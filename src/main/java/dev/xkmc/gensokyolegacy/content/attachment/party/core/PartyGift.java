package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftItemData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftPreference;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What a satisfied guest leaves behind.
 * <p>
 * Reuses the existing gift items rather than inventing new ones:
 * {@code GLMeta.GIFT_DATA} already scores favour per item and
 * {@code GLMeta.GIFT_PREFERENCE} weights categories per character, so a guest
 * picks a gift the same way {@code GiftModule} scores one handed to her.
 * <p>
 * The gift is dropped on the table with {@link Block#popResource} rather than
 * granted into an inventory, which is the in-tree idiom for putting an item down
 * in the world. Nothing here needs unwinding when the real gift item is designed.
 */
public class PartyGift {

	/** A guest who was not fully served gives away a smaller gift. */
	public static final int UNFULFILLED_DIVISOR = 2;

	/**
	 * Drop a gift on the guest's table. A guest with nothing tasteful to give
	 * leaves nothing rather than something arbitrary.
	 */
	public static void give(ServerLevel level, YoukaiEntity guest, PartyModule party) {
		var table = party.table();
		if (table == null) return;
		ItemStack gift = pick(level, guest);
		if (gift.isEmpty()) return;
		int count = gift.getCount();
		// under-served guests are less generous
		if (party.fulfillment() < 1.0) count = Math.max(1, count / UNFULFILLED_DIVISOR);
		Block.popResource(level, table.above(), gift.copyWithCount(count));
	}

	/**
	 * One gift item, weighted by how much this character likes it. Mirrors
	 * {@code GiftItemData.getFavor}: a per-item override beats the per-category
	 * preference, and a weight of zero means she never gives that item at all.
	 */
	private static ItemStack pick(ServerLevel level, YoukaiEntity guest) {
		var pref = GLMeta.GIFT_PREFERENCE.get(level.registryAccess(), guest.getType().builtInRegistryHolder());
		List<ItemStack> items = new ArrayList<>();
		List<Integer> weights = new ArrayList<>();
		int total = 0;
		for (var holder : level.registryAccess().registryOrThrow(Registries.ITEM).asHolderIdMap()) {
			var data = GLMeta.GIFT_DATA.get(level.registryAccess(), holder);
			if (data == null) continue;
			int weight = weightOf(data, pref, holder.value());
			if (weight <= 0) continue;
			items.add(new ItemStack(holder.value()));
			weights.add(weight);
			total += weight;
		}
		if (total <= 0) return ItemStack.EMPTY;
		int roll = guest.getRandom().nextInt(total);
		for (int i = 0; i < items.size(); i++) {
			roll -= weights.get(i);
			if (roll < 0) return items.get(i);
		}
		return items.getLast();
	}

	private static int weightOf(GiftItemData data, @Nullable GiftPreference pref, Item item) {
		if (pref == null) return 1;
		var override = pref.override().get(item);
		if (override != null) return (int) Math.round(Math.max(0, override));
		double mult = pref.preference().getOrDefault(data.type(), 1d);
		return (int) Math.round(Math.max(0, mult));
	}

	private PartyGift() {
	}

}
