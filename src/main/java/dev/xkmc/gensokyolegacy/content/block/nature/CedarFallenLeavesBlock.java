package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CedarFallenLeavesBlock extends SimpleBushBlock {

	public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 2);
	private static final MapCodec<CedarFallenLeavesBlock> CODEC = simpleCodec(CedarFallenLeavesBlock::new);

	public CedarFallenLeavesBlock(Properties properties) {
		super(properties, LAYER);
		this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1));
	}

	@Override
	public MapCodec<? extends BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LAYERS);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(LAYERS, 2);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult result) {
		if (level.isClientSide) return ItemInteractionResult.CONSUME;
		if (itemStack.getItem().equals(blockState.getBlock().asItem()) && blockState.getValue(LAYERS) == 2) {
			level.setBlockAndUpdate(pos, blockState.setValue(LAYERS, 1));
			if (!player.isCreative()) itemStack.shrink(1);
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	public static void loot(RegistrateBlockLootTables tb, Block block) {
		var enchantments = tb.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
		var shovelOrSilk = MatchTool.toolMatches(ItemPredicate.Builder.item().of(ItemTags.SHOVELS))
				.or(MatchTool.toolMatches(ItemPredicate.Builder.item()
						.withSubPredicate(ItemSubPredicates.ENCHANTMENTS,
								ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
										enchantments.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)))))));
		tb.add(block, LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(shovelOrSilk)
						.add(LootItem.lootTableItem(block))));
	}
}
