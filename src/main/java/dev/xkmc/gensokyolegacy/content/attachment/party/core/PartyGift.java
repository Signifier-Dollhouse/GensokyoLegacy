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
 * What a guest leaves behind when the spread runs out.
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

	/** Appeal at which the guest's gift gains one item over the item's own count. */
	public static final int APPEAL_PER_EXTRA = 8;

	/** Ceiling on those extra items, so a lavish banquet does not bury the table. */
	public static final int MAX_EXTRA = 4;

	/**
	 * Drop a gift on the guest's table, once per visit.
	 * <p>
	 * A guest with nothing tasteful to give leaves nothing rather than something
	 * arbitrary.
	 * <p>
	 * Sized on the <b>appeal she personally consumed</b>, not on whether she was
	 * satisfied. Those are the two food measurements (design doc §5.2) and they answer
	 * different questions: rare-effect delicacies fill her up no better than plain bread
	 * does, and she is still more moved by them. This version halved the gift for an
	 * under-served guest, which made a beautiful thin spread read as stingy rather than
	 * as modest — the host's generosity and the guest's appetite are not one axis.
	 * <p>
	 * <b>Once.</b> She leaves through two doors — running out of food she can reach, or
	 * rolling {@code leaveChance} — and a visit can pass near enough to both that she
	 * would otherwise thank the host twice for one banquet. The flag lives on her module
	 * rather than here, so it survives the two calls and any task restart between them.
	 *
	 * @return whether a gift was left
	 */
	public static boolean give(ServerLevel level, YoukaiEntity guest, PartyModule party) {
		if (party.gifted()) return false;
		var table = party.table();
		if (table == null) return false;
		ItemStack gift = pick(level, guest);
		if (gift.isEmpty()) return false;
		party.markGifted();
		int extra = Math.min(MAX_EXTRA, party.appealEaten() / APPEAL_PER_EXTRA);
		Block.popResource(level, table.above(), gift.copyWithCount(gift.getCount() + extra));
		return true;
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
