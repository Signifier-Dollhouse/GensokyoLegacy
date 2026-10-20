package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** A log or wood block that an axe strips to a vanilla block. The target is per instance, so one class serves
 * every wood; a wood with no vanilla counterpart of its own strips to the closest one the mod already uses.
 */
public class StrippableWoodBlock extends RotatedPillarBlock {

	private static final MapCodec<StrippableWoodBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockState.CODEC.fieldOf("stripped").forGetter(b -> b.stripped.get().defaultBlockState()),
			propertiesCodec()).apply(i, (state, properties) -> new StrippableWoodBlock(() -> state.getBlock(), properties)));

	private final Supplier<Block> stripped;

	public StrippableWoodBlock(Supplier<Block> stripped, Properties properties) {
		super(properties);
		this.stripped = stripped;
	}

	@Override
	public MapCodec<? extends RotatedPillarBlock> codec() {
		return CODEC;
	}

	@Override
	public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
		if (ability == ItemAbilities.AXE_STRIP) {
			return stripped.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
		}
		return super.getToolModifiedState(state, context, ability, simulate);
	}

}
