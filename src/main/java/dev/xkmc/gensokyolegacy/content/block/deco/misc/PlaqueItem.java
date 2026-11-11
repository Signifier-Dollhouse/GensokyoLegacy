package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;

public class PlaqueItem extends BlockItem {

	public static final ResourceLocation ALTERNATE_MODEL = GensokyoLegacy.loc("plaque_alternate");

	public PlaqueItem(Block block, Properties properties) {
		super(block, properties.component(DataComponents.BLOCK_STATE,
				BlockItemStateProperties.EMPTY.with(PlaqueBlock.ALTERNATE, false)));
	}

	public static boolean isAlternate(ItemStack stack) {
		return Boolean.TRUE.equals(stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
				.get(PlaqueBlock.ALTERNATE));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!player.isCreative() || player.pick(player.blockInteractionRange(), 1, false).getType() != HitResult.Type.MISS) {
			return super.use(level, player, hand);
		}
		ItemStack stack = player.getItemInHand(hand);
		stack.update(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY,
				state -> state.with(PlaqueBlock.ALTERNATE, !Boolean.TRUE.equals(state.get(PlaqueBlock.ALTERNATE))));
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}
}
