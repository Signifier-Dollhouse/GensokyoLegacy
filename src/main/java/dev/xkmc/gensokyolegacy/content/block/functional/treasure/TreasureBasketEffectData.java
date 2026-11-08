package dev.xkmc.gensokyolegacy.content.block.functional.treasure;

import dev.xkmc.gensokyolegacy.content.attachment.area.EffectData;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2serial.serialization.marker.OnInject;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Effect payload of the treasure basket: a 54-slot item buffer kept in the level attachment, so
 * contents survive and stay reachable while the owner chunk is unloaded, without a block entity.
 *
 * @see "doc/design/treasure_basket.md"
 */
@SerialClass
public class TreasureBasketEffectData extends EffectData {

	public static final int SLOTS = 54;

	private static ItemStack[] defaultStacks() {
		ItemStack[] ans = new ItemStack[SLOTS];
		Arrays.fill(ans, ItemStack.EMPTY);
		return ans;
	}

	@SerialField(toClient = false)
	private ItemStack[] items = defaultStacks();

	public TreasureBasketEffectData() {
	}

	@OnInject
	public void onInject() {
		items = normalize(items);
	}

	private static ItemStack[] normalize(@Nullable ItemStack[] data) {
		ItemStack[] ans = data != null && data.length == SLOTS ? data : new ItemStack[SLOTS];
		if (data != null && data.length != SLOTS) {
			Arrays.fill(ans, ItemStack.EMPTY);
			System.arraycopy(data, 0, ans, 0, Math.min(data.length, SLOTS));
		}
		for (int i = 0; i < SLOTS; i++) {
			if (ans[i] == null) ans[i] = ItemStack.EMPTY;
		}
		return ans;
	}

	@Override
	public boolean isOwnerStillValid(ServerLevel level, BlockPos ownerPos, BlockState state) {
		return state.is(GLBlocks.TREASURE_BASKET.get());
	}

	/**
	 * Stores the whole stack, or nothing when this basket cannot take all of it. Merges into
	 * matching stacks first, then fills empty slots.
	 *
	 * @return an empty stack on success, or {@code stack} unchanged when there is no room
	 */
	public ItemStack insert(ItemStack stack) {
		int space = 0;
		for (int i = 0; i < SLOTS; i++) {
			ItemStack cur = items[i];
			if (cur.isEmpty()) space += stack.getMaxStackSize();
			else if (ItemStack.isSameItemSameComponents(cur, stack))
				space += Math.max(0, cur.getMaxStackSize() - cur.getCount());
		}
		if (space < stack.getCount()) return stack;
		ItemStack remain = stack.copy();
		for (int i = 0; i < SLOTS && !remain.isEmpty(); i++) {
			ItemStack cur = items[i];
			if (cur.isEmpty() || !ItemStack.isSameItemSameComponents(cur, remain)) continue;
			int move = Math.min(remain.getCount(), cur.getMaxStackSize() - cur.getCount());
			if (move <= 0) continue;
			cur.grow(move);
			remain.shrink(move);
		}
		for (int i = 0; i < SLOTS && !remain.isEmpty(); i++) {
			if (!items[i].isEmpty()) continue;
			int move = Math.min(remain.getCount(), remain.getMaxStackSize());
			items[i] = remain.copyWithCount(move);
			remain.shrink(move);
		}
		return remain;
	}

	/**
	 * Drops every stored stack at the basket and clears the buffer.
	 */
	public void dropContents(ServerLevel level, BlockPos pos) {
		for (int i = 0; i < SLOTS; i++) {
			ItemStack stack = items[i];
			if (stack == null || stack.isEmpty()) continue;
			Block.popResource(level, pos, stack);
			items[i] = ItemStack.EMPTY;
		}
	}

}
