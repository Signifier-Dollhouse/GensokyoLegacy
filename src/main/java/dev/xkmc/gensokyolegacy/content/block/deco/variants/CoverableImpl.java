package dev.xkmc.gensokyolegacy.content.block.deco.variants;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.ISeatableBlock;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLDecoBlocks;
import dev.xkmc.l2core.serial.loot.LootHelper;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class CoverableImpl implements CreateBlockStateBlockMethod, DefaultStateBlockMethod, UseItemOnBlockMethod {

	public enum Color implements StringRepresentable {
		NONE(Items.AIR),
		BASE(() -> GLDecoBlocks.TATAMI.asItem()),

		WHITE(Blocks.WHITE_CARPET),
		ORANGE(Blocks.ORANGE_CARPET),
		MAGENTA(Blocks.MAGENTA_CARPET),
		LIGHT_BLUE(Blocks.LIGHT_BLUE_CARPET),
		YELLOW(Blocks.YELLOW_CARPET),
		LIME(Blocks.LIME_CARPET),
		PINK(Blocks.PINK_CARPET),
		GRAY(Blocks.GRAY_CARPET),
		LIGHT_GRAY(Blocks.LIGHT_GRAY_CARPET),
		CYAN(Blocks.CYAN_CARPET),
		PURPLE(Blocks.PURPLE_CARPET),
		BLUE(Blocks.BLUE_CARPET),
		BROWN(Blocks.BROWN_CARPET),
		GREEN(Blocks.GREEN_CARPET),
		RED(Blocks.RED_CARPET),
		BLACK(Blocks.BLACK_CARPET);

		private final ItemLike item;

		Color(ItemLike item) {
			this.item = item;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

	}

	public static final EnumProperty<Color> COLOR = EnumProperty.create("color", Color.class, Color.values());

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COLOR);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(COLOR, Color.NONE);
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player pl, InteractionHand hand, BlockHitResult result) {
		// the pad sits on the lower half only, so the upper half stays plain wood
		if (!ISeatableBlock.isSeatPos(state)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (state.getValue(COLOR) == Color.NONE) {
			for (var e : Color.values()) {
				if (e.item.asItem() == Items.AIR) continue;
				if (stack.is(e.item.asItem())) {
					if (!level.isClientSide()) {
						level.setBlockAndUpdate(pos, state.setValue(COLOR, e));
						if (!pl.getAbilities().instabuild) {
							stack.shrink(1);
						}
					}
					return ItemInteractionResult.SUCCESS;
				}
			}
		} else if (stack.canPerformAction(ItemAbilities.SHEARS_CARVE)) {
			if (!level.isClientSide()) {
				var col = state.getValue(COLOR);
				level.setBlockAndUpdate(pos, state.setValue(COLOR, Color.NONE));
				if (!pl.getAbilities().instabuild) {
					Block.popResource(level, pos, new ItemStack(col.item, 1));
					stack.hurtAndBreak(1, pl, LivingEntity.getSlotForHand(hand));
				}
			}
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	public static LootTable.Builder loot(RegistrateBlockLootTables pvd, Block block) {
		return loot(pvd, block, null);
	}

	/**
	 * @param self extra condition gating the block drop, used by two block
	 *             furniture to drop from one half only
	 */
	public static LootTable.Builder loot(RegistrateBlockLootTables pvd, Block block, @Nullable LootItemCondition.Builder self) {
		LootHelper helper = new LootHelper(pvd);
		var ans = LootTable.lootTable();
		var pool = LootPool.lootPool().add(LootItem.lootTableItem(block));
		if (self != null) pool.when(self);
		ans.withPool(pool);
		for (var e : Color.values()) {
			if (e.item.asItem() == Items.AIR) continue;
			ans.withPool(LootPool.lootPool().add(LootItem.lootTableItem(e.item)).when(helper.enumState(block, COLOR, e)));
		}
		return ans;
	}

	/**
	 * Every coverable block puts the same cover geometry on its own geometry, so
	 * the cover models only depend on the color and are shared by all blocks
	 * instead of being rebuilt for every block and color pair. The model provider
	 * caches builders by name, so generating the same cover twice is a no-op and
	 * the loop over the woods can call this once per block.
	 */
	private static void buildCoverModels(RegistrateBlockstateProvider pvd, String suffix, String textureDir, String parent, String textureKey) {
		for (var e : Color.values()) {
			if (e.item.asItem() == Items.AIR) continue;
			String name = coverName(e, suffix);
			pvd.models().getBuilder("block/" + name)
					.parent(new ModelFile.UncheckedModelFile(pvd.modLoc(parent)))
					.texture(textureKey, "block/" + textureDir + "/" + name)
					.renderType("cutout");
		}
	}

	/**
	 * Name of both the cover model and its texture, the base cover being the only
	 * one without a color prefix.
	 */
	public static String coverName(Color color, String suffix) {
		return color == Color.BASE ? suffix : color.getSerializedName() + "_" + suffix;
	}

	public static void buildTableStates(RegistrateBlockstateProvider pvd) {
		buildCoverModels(pvd, "tablecloth", "table", "custom/furniture/tablecloth", "all");
	}

	public static void buildTableStates(MultiPartBlockStateBuilder builder, RegistrateBlockstateProvider pvd, ModelFile table) {
		buildTableStates(pvd);
		for (var e : Color.values()) {
			var file = e.item.asItem() == Items.AIR ? table : new ModelFile.UncheckedModelFile(pvd.modLoc("block/" + coverName(e, "tablecloth")));
			builder.part().modelFile(file).addModel().condition(COLOR, e).end();
		}
	}

	public static void buildChairStates(RegistrateBlockstateProvider pvd) {
		buildCoverModels(pvd, "pad", "chair", "custom/furniture/wooden_large_chair_pad_overlay", "pad");
	}
}
