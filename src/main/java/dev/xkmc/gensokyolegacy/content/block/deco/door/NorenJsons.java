package dev.xkmc.gensokyolegacy.content.block.deco.door;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;

public class NorenJsons {

	/** 普通款与长款各一份共享几何,挂上贴图就能当方块模型用 */
	private static final BlockModelBuilder[] BASE = new BlockModelBuilder[2];

	public static void buildBlockState(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd,
	                                   boolean hanging) {
		pvd.horizontalBlock(ctx.get(), model(ctx, pvd, hanging));
	}

	public static BlockModelBuilder model(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd,
	                                      boolean hanging) {
		ResourceLocation tex = pvd.modLoc("block/noren/" + ctx.getName());
		return pvd.models().getBuilder("block/" + ctx.getName())
				.parent(base(pvd, hanging))
				.texture("curtain", tex)
				.renderType("cutout");
	}

	private static BlockModelBuilder base(RegistrateBlockstateProvider pvd, boolean hanging) {
		int idx = hanging ? 1 : 0;
		if (BASE[idx] == null) {
			int y0 = hanging ? -NorenBlock.HANGING : 0;
			int height = 16 - y0;
			var base = pvd.models().withExistingParent(hanging ? "noren_hanging" : "noren", "block/block");
			cloth(base, y0, height);
			base.texture("particle", "#curtain");
			BASE[idx] = base;
		}
		return BASE[idx];
	}

	/** 帘子是一张零厚度的布,只渲染正反两面 */
	private static void cloth(ModelBuilder<?> builder, int y0, int height) {
		var elem = builder.element();
		elem.from(0, y0, 8).to(16, 16, 8);
		elem.face(Direction.NORTH).uvs(16, 0, 0, height).texture("#curtain").cullface(Direction.NORTH).end();
		elem.face(Direction.SOUTH).uvs(0, 0, 16, height).texture("#curtain").cullface(Direction.SOUTH).end();
		elem.end();
	}

}