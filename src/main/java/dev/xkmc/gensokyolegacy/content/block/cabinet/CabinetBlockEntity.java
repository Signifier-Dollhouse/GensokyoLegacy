package dev.xkmc.gensokyolegacy.content.block.cabinet;

import dev.xkmc.l2modularblock.tile_api.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

public class CabinetBlockEntity extends BaseContainerBlockEntity implements TickableBlockEntity {

	private static final int SIZE = 27;

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {

		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS,
					1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
			setOpen(level, pos, state, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS,
					1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
			setOpen(level, pos, state, false);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount) {
		}

		@Override
		protected boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == CabinetBlockEntity.this;
		}

	};

	public CabinetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private static void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
		if (state.hasProperty(CabinetBlock.OPEN) && state.getValue(CabinetBlock.OPEN) != open) {
			level.setBlock(pos, state.setValue(CabinetBlock.OPEN, open), 3);
		}
	}

	@Override
	public void tick() {
		if (level != null && !level.isClientSide()) {
			openersCounter.recheckOpeners(level, worldPosition, getBlockState());
		}
	}

	@Override
	public void startOpen(Player player) {
		if (!remove && !player.isSpectator()) {
            if (getLevel() != null) {
                openersCounter.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
            }
        }
	}

	@Override
	public void stopOpen(Player player) {
		if (!remove && !player.isSpectator()) {
            if (getLevel() != null) {
                openersCounter.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
            }
        }
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> list) {
		items = list;
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inv) {
		return ChestMenu.threeRows(id, inv, this);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, items, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, items, registries);
	}

}
