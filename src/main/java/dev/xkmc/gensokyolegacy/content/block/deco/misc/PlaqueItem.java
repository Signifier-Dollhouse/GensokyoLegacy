package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;

public class PlaqueItem extends BlockItem {

	public static final ResourceLocation ALTERNATE_MODEL = GensokyoLegacy.loc("plaque_alternate");

	public PlaqueItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static boolean isAlternate(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null) return false;
		CompoundTag state = tag.getCompound(BLOCK_STATE_TAG);
		return PlaqueBlock.ALTERNATE.getValue(state.getString(PlaqueBlock.ALTERNATE.getName())).orElse(false);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!player.isCreative() || player.pick(player.getBlockReach(), 1, false).getType() != HitResult.Type.MISS) {
			return super.use(level, player, hand);
		}
		ItemStack stack = player.getItemInHand(hand);
		CompoundTag tag = stack.getOrCreateTag();
		CompoundTag state = tag.getCompound(BLOCK_STATE_TAG);
		state.putString(PlaqueBlock.ALTERNATE.getName(), Boolean.toString(!isAlternate(stack)));
		tag.put(BLOCK_STATE_TAG, state);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}
}
