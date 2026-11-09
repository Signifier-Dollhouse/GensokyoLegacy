package dev.xkmc.gensokyolegacy.content.block.functional.treasure;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectVisual;
import dev.xkmc.gensokyolegacy.content.attachment.area.EffectData;
import dev.xkmc.gensokyolegacy.content.item.character.StrangeGlassesItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2serial.serialization.marker.OnInject;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Effect payload of the treasure basket: a 54-slot item buffer kept in the level attachment, so
 * contents survive and stay reachable while the owner chunk is unloaded, without a block entity.
 *
 * @see "doc/design/treasure_basket.md"
 */
@SerialClass
public class TreasureBasketEffectData extends EffectData {

	public static final int SLOTS = 54;

	// placeholder: same forcefield look as the sealing pot until the basket has its own texture
	private static final AreaEffectVisual VISUAL = new AreaEffectVisual(GensokyoLegacy.loc("textures/barriers/sealing_pot.png"),
			0.35F, 1F, 0.45F, 0.35F, -0.5F, true, false, false);

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

	@Override
	public List<AreaEffectVisual> getClientVisual(@Nullable Player viewer) {
		if (viewer != null && (StrangeGlassesItem.isWearing(viewer) || isHoldingTreasureBasket(viewer)))
			return List.of(VISUAL);
		return List.of();
	}

	private static boolean isHoldingTreasureBasket(Player player) {
		return player.getMainHandItem().is(GLBlocks.TREASURE_BASKET.asItem()) ||
				player.getOffhandItem().is(GLBlocks.TREASURE_BASKET.asItem());
	}

	/**
	 * Merges into matching stacks first, then fills empty slots.
	 *
	 * @return the part that did not fit, empty when the whole stack was stored
	 */
	public ItemStack insert(ItemStack stack) {
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

	public ItemStack getStack(int slot) {
		return slot >= 0 && slot < SLOTS ? items[slot] : ItemStack.EMPTY;
	}

	public void setStack(int slot, ItemStack stack) {
		if (slot < 0 || slot >= SLOTS) return;
		items[slot] = stack.isEmpty() ? ItemStack.EMPTY
				: stack.copyWithCount(Math.min(stack.getCount(), stack.getMaxStackSize()));
	}

}
