package dev.xkmc.gensokyolegacy.content.block.shelf;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import javax.annotation.Nullable;

public class SimpleShelfBlock implements ShapeBlockMethod {

    public static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        for (int i = 0; i < 4; i++) {
            var dir = Direction.from2DDataValue(i);
            SHAPES[i] = new VoxelBuilder(0, 0, 6, 16, 16, 16).rotateFromNorth(dir);
        }
    }

    @Override
    @Nullable
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(BlockStateProperties.HORIZONTAL_FACING).get2DDataValue()];
    }

    public static void buildStates(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd) {
        pvd.horizontalBlock(ctx.get(), pvd.models().getBuilder(ctx.getName())
                .parent(new ModelFile.UncheckedModelFile(GensokyoLegacy.loc("custom/shelf_empty")))
                .texture("0", "block/shelf/" + ctx.getName())
                .texture("particle", "block/shelf/" + ctx.getName())
        );
    }
}