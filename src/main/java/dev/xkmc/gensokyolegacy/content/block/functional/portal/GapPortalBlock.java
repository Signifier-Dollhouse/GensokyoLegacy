package dev.xkmc.gensokyolegacy.content.block.functional.portal;

import dev.xkmc.gensokyolegacy.content.attachment.gap.GapMapping;
import dev.xkmc.gensokyolegacy.content.attachment.gap.GapMappingData;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.impl.DoubleBlockImpl;
import dev.xkmc.l2modularblock.mult.AnimateTickBlockMethod;
import dev.xkmc.l2modularblock.mult.OnReplacedBlockMethod;
import dev.xkmc.l2modularblock.mult.SetPlacedByBlockMethod;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import dev.xkmc.l2modularblock.type.BlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GapPortalBlock implements AnimateTickBlockMethod, ShapeBlockMethod, SetPlacedByBlockMethod, UseItemOnBlockMethod, OnReplacedBlockMethod {

	public static final BlockMethod TE = new BlockEntityBlockMethodImpl<>(GLBlocks.GAP_BE, GapPortalBlockEntity.class);

	public static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

	public static BasePortalBlock of(BlockBehaviour.Properties properties) {
		return new BasePortalBlock(properties, new BasePortalBlock.PortalMethod(), new DoubleBlockImpl(), new GapPortalBlock(), TE);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity le, ItemStack stack) {
		if (state.getValue(BlockStateProperties.HALF) == Half.BOTTOM) {
			if (level.getBlockEntity(pos) instanceof GapPortalBlockEntity be) {
				be.setPlacedBy(stack);
			}
		}
	}

	@Override
	public void onReplaced(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (isMoving) return;
		if (state.getValue(BlockStateProperties.HALF) != Half.BOTTOM) return;
		if (newState.getBlock() == state.getBlock()) return;
		if (!(level instanceof ServerLevel sl)) return;
		if (!(level.getBlockEntity(pos) instanceof GapPortalBlockEntity gap) || gap.id == null) return;
		var data = GapMappingData.get(sl);
		var mapping = data.get(gap.id);
		if (mapping == null) return;
		PortalSide side = gap.getSide();
		GapMapping newMapping = mapping.with(side, null, null);
		if (newMapping.entryPos() == null && newMapping.exitPos() == null) {
			data.remove(gap.id);
		} else {
			data.set(gap.id, newMapping);
		}
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player pl, InteractionHand hand, BlockHitResult result) {
		if (state.getValue(BlockStateProperties.HALF) == Half.TOP) pos = pos.below();
		// Use portal item on portal with same uuid to generate the other side's data
		if (!stack.isEmpty() && stack.has(GLItems.DC_UUID) && stack.has(GLItems.DC_PORTAL_SIDE)
				&& level.getBlockEntity(pos) instanceof GapPortalBlockEntity gap && gap.id != null
				&& stack.get(GLItems.DC_UUID).equals(gap.id)) {
			if (level instanceof ServerLevel sl) {
				var data = GapMappingData.get(sl);
				var mapping = data.get(gap.id);
				if (mapping != null && mapping.isPending()) {
					PortalSide itemSide = stack.get(GLItems.DC_PORTAL_SIDE);
					PortalSide blockSide = gap.getSide();
					if (itemSide != null && blockSide != null && itemSide != blockSide) {
						GapMapping newMapping = GapPortalForcer.completePending(mapping, itemSide, sl);
						if (newMapping.isPending()) return ItemInteractionResult.FAIL;
						BlockPos targetPos = newMapping.posAt(itemSide);
						ResourceLocation targetDim = newMapping.dimAt(itemSide);
						ServerLevel targetLevel = sl.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, targetDim));
						if (targetLevel == null || !GapPortalForcer.canPlaceAt(targetLevel, targetPos)) {
							return ItemInteractionResult.FAIL;
						}
						if (!level.isClientSide()) {
							data.set(gap.id, newMapping);
							pl.setItemInHand(hand, ItemStack.EMPTY);
							level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
						}
						return ItemInteractionResult.SUCCESS;
					}
				}
				return ItemInteractionResult.FAIL;
			}
			return ItemInteractionResult.SUCCESS;
		}
		if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND && level.getBlockEntity(pos) instanceof GapPortalBlockEntity gap) {
			if (!level.isClientSide()) {
				ItemStack toGive = gap.getItem();
				level.removeBlock(pos, false);
				level.removeBlock(pos.above(), false);
				pl.setItemInHand(hand, toGive);
			}
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.FAIL;
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
		if (rand.nextInt(100) == 0) {
			level.playLocalSound(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F,
					SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, rand.nextFloat() * 0.4F + 0.8F, false);
		}

		for (int i = 0; i < 4; ++i) {
			double dx = (rand.nextFloat() - 0.5F) * 4F;
			double dz = (rand.nextFloat() - 0.5F) * 4F;

			double x = pos.getX() + 0.5f;
			double y = pos.getY() + rand.nextFloat();
			double z = pos.getZ() + 0.5f;

			level.addParticle(ParticleTypes.PORTAL, x, y, z, dx, 0, dz);
		}

	}


}
