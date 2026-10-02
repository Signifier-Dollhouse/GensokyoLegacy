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
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.util.TransformationHelper.TransformOrigin;

import java.util.function.Function;

public class NorenJsons {

	/** 挂帘的木杆,占方块最顶上那 1 格 */
	private static final float BAR = 1;
	/** 木杆有 1 格厚,帘子零厚度地吊在厚度的正中,也就是墙那边往外挪半格 */
	private static final float MID_Z = 0.5F;
	/** 普通款的布占掉木杆以外剩下的 15 格 */
	private static final float CLOTH = 16 - BAR;

	/**
	 * 普通款与长款各一份共享几何,挂上贴图就能当方块模型用
	 */
	private static final BlockModelBuilder[] BASE = new BlockModelBuilder[2];

	public static void buildBlockState(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd,
									   boolean hanging) {
		pvd.horizontalBlock(ctx.get(), model(ctx, pvd, hanging));
	}

	public static BlockModelBuilder model(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd,
										  boolean hanging) {
		return pvd.models().getBuilder("block/" + ctx.getName())
				.parent(base(pvd, hanging))
				.texture("curtain", texture(pvd::modLoc, ctx.getName()))
				.renderType("cutout");
	}

	private static ResourceLocation texture(Function<String, ResourceLocation> modLoc, String name) {
		return modLoc.apply("block/noren/" + name);
	}

	private static BlockModelBuilder base(RegistrateBlockstateProvider pvd, boolean hanging) {
		int idx = hanging ? 1 : 0;
		if (BASE[idx] == null) {
			float row = uvPerRow(hanging);
			float top = 16 - BAR;
			var base = pvd.models().withExistingParent(hanging ? "noren_hanging" : "noren", "block/block");
			bar(base, row);
			cloth(base, top - (hanging ? CLOTH + NorenBlock.HANGING : CLOTH), top, row);
			base.texture("particle", "#curtain");
			BASE[idx] = base;
		}
		return BASE[idx];
	}

	/**
	 * 一行贴图折合多少 uv。uv 永远是 0..16 铺满整张贴图,不管贴图本身多少像素高:
	 * 普通款贴图 16 行高,一行就是一格 uv;长款贴图 32 行高、只有前 20 行有内容,一行是半格 uv。
	 */
	private static float uvPerRow(boolean hanging) {
		return hanging ? 0.5F : 1F;
	}

	/**
	 * 木杆:方块最顶上那 1 格,占满整格宽,贴图就取最顶上那一行。
	 * 六个面都画出来,不然从侧面看是一条空的框。
	 */
	private static void bar(ModelBuilder<?> builder, float row) {
		var elem = builder.element();
		elem.from(0, 16 - BAR, 0).to(16, 16, 1);
		elem.face(Direction.NORTH).uvs(16, 0, 0, row).texture("#curtain").cullface(Direction.NORTH).end();
		elem.face(Direction.SOUTH).uvs(0, 0, 16, row).texture("#curtain").cullface(Direction.SOUTH).end();
		elem.face(Direction.UP).uvs(0, 0, 16, row).texture("#curtain").cullface(Direction.UP).end();
		elem.face(Direction.DOWN).uvs(0, 0, 16, row).texture("#curtain").cullface(Direction.DOWN).end();
		elem.face(Direction.WEST).uvs(0, 0, row, row).texture("#curtain").cullface(Direction.WEST).end();
		elem.face(Direction.EAST).uvs(16 - row, 0, 16, row).texture("#curtain").cullface(Direction.EAST).end();
		elem.end();
	}

	/**
	 * 帘子本体是一张零厚度的布,吊在木杆厚度的正中,只渲染正反两面。贴脸 z=0 那条边,所以正脸朝南。
	 * 木杆占掉贴图第一行,布要的是剩下的行,所以 uv 比模型高出那一行。
	 *
	 * @param bottom 布的下沿,长款往下多吊 {@link NorenBlock#HANGING} 格
	 * @param top    布的上沿,也就是木杆的底面
	 */
	private static void cloth(ModelBuilder<?> builder, float bottom, float top, float row) {
		var elem = builder.element();
		elem.from(0, bottom, MID_Z).to(16, top, MID_Z);
		elem.face(Direction.NORTH).uvs(16, row, 0, (top - bottom + BAR) * row).texture("#curtain")
				.cullface(Direction.NORTH).end();
		elem.face(Direction.SOUTH).uvs(0, row, 16, (top - bottom + BAR) * row).texture("#curtain")
				.cullface(Direction.SOUTH).end();
		elem.end();
	}

	/**
	 * 物品模型一律用 {@code item/generated}:它把整张贴图铺满 0..16 的面片,普通款的方形贴图正好合适。
	 * 长款贴图 16x32,原样铺就变成 16x16 的正方形,帘子被压得又扁又胖;
	 * 绕中心把 x 压掉一半,面片就成了 8x16 —— 横竖压得一样多,长宽比才对得上,图标也小一圈。
	 * 缩放走根 {@code transform} 而不是重写 {@code elements},所以两种款式都是一张干净的 generated 模型。
	 */
	public static void genItemModel(DataGenContext<Item, BlockItem> ctx, RegistrateItemModelProvider pvd,
									boolean hanging) {
		var model = pvd.getBuilder(ctx.getName())
				.parent(new ModelFile.UncheckedModelFile("item/generated"))
				.texture("layer0", texture(pvd::modLoc, ctx.getName()));
		if (hanging) {
			model.rootTransforms().scale(0.8F, 1.6F, 1).translation(0,-0.25F,0).origin(TransformOrigin.CENTER).end();
		}
	}

}