package dev.xkmc.gensokyolegacy.content.block.functional.treasure;

import dev.xkmc.gensokyolegacy.init.registrate.GLMisc;
import dev.xkmc.l2core.base.tile.BaseBlockEntity;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Gives the treasure basket a block entity so the container capability and the menu have a
 * handle. Contents stay in {@link TreasureBasketEffectData}; this only proxies to it.
 *
 * @see "doc/design/treasure_basket.md"
 */
@SerialClass
public class TreasureBasketBlockEntity extends BaseBlockEntity implements MenuProvider {

	private IItemHandler handler;

	public TreasureBasketBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public IItemHandler getItemCap(@Nullable Direction dir) {
		if (handler == null) {
			handler = level instanceof ServerLevel sl
					? new TreasureBasketItemHandler(sl, getBlockPos())
					: new ItemStackHandler(TreasureBasketEffectData.SLOTS);
		}
		return handler;
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new TreasureBasketMenu(GLMisc.TREASURE_BASKET.get(), id, inv, getBlockPos());
	}

}
