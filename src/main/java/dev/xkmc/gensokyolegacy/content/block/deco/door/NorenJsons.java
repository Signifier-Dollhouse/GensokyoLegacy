package dev.xkmc.gensokyolegacy.content.block.deco.door;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class NorenJsons {

	/**
	 * 普通款与长款各一份共享几何,挂上贴图就能当方块模型用
	 */
	private static final BlockModelBuilder[] BASE = new BlockModelBuilder[2];

	private static ItemModelBuilder[] ITEM_BASE;

	public static void buildBlockState(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd,
									   boolean hanging) {
		pvd.horizontalBlock(ctx.get(), model(ctx, pvd, hanging));
	}

	public static void genItemModel(DataGenContext<Item, BlockItem> ctx, RegistrateItemModelProvider pvd,
									boolean hanging) {
		itemBase(pvd, hanging);
		pvd.getBuilder(ctx.getName())
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("item/" + itemBaseName(hanging))))
				.texture("curtain", pvd.modLoc("block/noren/" + ctx.getName()));
	}

	public static BlockModelBuilder model(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd,
										  boolean hanging) {
		return pvd.models().getBuilder("block/" + ctx.getName())
				.parent(base(pvd, hanging))
				.texture("curtain", texture(pvd, ctx.getName()))
				.renderType("cutout");
	}

	private static ResourceLocation texture(RegistrateBlockstateProvider pvd, String name) {
		return pvd.modLoc("block/noren/" + name);
	}

	private static BlockModelBuilder base(RegistrateBlockstateProvider pvd, boolean hanging) {
		int idx = hanging ? 1 : 0;
		if (BASE[idx] == null) {
			var base = pvd.models().withExistingParent(hanging ? "noren_hanging" : "noren", "block/block");
			cloth(base, hanging ? -4 : 0, hanging ? 10 : 16, true);
			base.texture("particle", "#curtain");
			BASE[idx] = base;
		}
		return BASE[idx];
	}

	/**
	 * 长款贴图有 20 行、方块只有 16 行高,{@code item/generated} 会把整张贴图压到 16x16 上,
	 * 所以长款单独用一张按 20 行取 uv 的物品模型。
	 */
	private static String itemBaseName(boolean hanging) {
		return hanging ? "noren_long_item" : "noren_item";
	}

	private static ItemModelBuilder itemBase(RegistrateItemModelProvider pvd, boolean hanging) {
		if (ITEM_BASE == null) ITEM_BASE = new ItemModelBuilder[2];
		int idx = hanging ? 1 : 0;
		if (ITEM_BASE[idx] == null) {
			var base = pvd.getBuilder(itemBaseName(hanging))
					.parent(new ModelFile.UncheckedModelFile("item/generated"))
					.renderType("cutout");
			cloth(base, 0, hanging ? 10 : 16, false);
			base.texture("layer0", "#curtain");
			ITEM_BASE[idx] = base;
		}
		return ITEM_BASE[idx];
	}

	/**
	 * 帘子是一张零厚度的布,只渲染正反两面。贴脸 z=0 那条边,所以正脸朝南。
	 *
	 * @param cull 世界里才需要 cullface:墙那一侧贴着实心方块时不必再画
	 */
	private static void cloth(ModelBuilder<?> builder, int y0, int height, boolean cull) {
		var elem = builder.element();
		elem.from(0, y0, 0).to(16, 16, 1);
		elem.face(Direction.NORTH).uvs(16, 0, 0, height).texture("#curtain")
				.cullface(cull ? Direction.NORTH : null).end();
		elem.face(Direction.SOUTH).uvs(0, 0, 16, height).texture("#curtain")
				.cullface(cull ? Direction.SOUTH : null).end();
		elem.end();
	}

}