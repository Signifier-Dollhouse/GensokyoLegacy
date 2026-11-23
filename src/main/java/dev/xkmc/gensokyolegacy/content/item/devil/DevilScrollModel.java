package dev.xkmc.gensokyolegacy.content.item.devil;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;

import java.util.Locale;

/**
 * Item model generation for the scroll: a flat icon for every perspective except the hands, where the
 * Blockbench scroll is drawn instead. The open scroll covers the superhuman main hand and both devil
 * hands, so those hands are empty models ({@code DevilScrollLayer}).
 *
 * @see "doc/design/item/devils_scroll.md"
 */
public class DevilScrollModel {

	/**
	 * The hand-authored Blockbench closed scroll.
	 */
	private static final String HELD = "custom/devils_scroll_passive";

	/**
	 * Icons by mode, in {@link DevilScrollMode} order: the plain book is the passive base model.
	 */
	private static final String[] ICONS = {
			"item/tool/devils_scroll",
			"item/tool/devils_scroll_superhuman",
			"item/tool/devils_scroll_devil"
	};

	public static void model(DataGenContext<Item, DevilScrollItem> ctx, RegistrateItemModelProvider pvd) {
		var root = pvd.getBuilder(ctx.getName());
		separate(root, pvd, ICONS[0], DevilScrollMode.PASSIVE);
		var modes = DevilScrollMode.values();
		for (int i = 1; i < modes.length; i++) {
			String name = "item/" + ctx.getName() + "_" + modes[i].name().toLowerCase(Locale.ROOT);
			var model = pvd.getBuilder(name);
			separate(model, pvd, ICONS[i], modes[i]);
			// vanilla picks the first override whose predicate is >= its value, so emit ascending
			root.override()
					.predicate(GensokyoLegacy.loc("devils_scroll_mode"), i)
					.model(model)
					.end();
		}
	}

	/**
	 * Icon for everything but the hands, the closed scroll for the hands that do not carry an open
	 * scroll, and an empty model where the layer draws instead.
	 */
	private static void separate(ItemModelBuilder builder, RegistrateItemModelProvider pvd,
								 String icon, DevilScrollMode mode) {
		var iconModel = pvd.nested()
				.parent(new ModelFile.UncheckedModelFile("item/generated"))
				.texture("layer0", pvd.modLoc(icon));
		var held = pvd.nested()
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc(HELD)));
		var empty = pvd.nested()
				.parent(new ModelFile.UncheckedModelFile("item/air"));
		boolean hideMain = mode == DevilScrollMode.SUPERHUMAN || mode == DevilScrollMode.DEVIL;
		boolean hideOff = mode == DevilScrollMode.DEVIL;
		builder.customLoader(SeparateTransformsModelBuilder::begin)
				.base(iconModel)
				.perspective(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, hideMain ? empty : held)
				.perspective(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, hideOff ? empty : held)
				.perspective(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, hideMain ? empty : held)
				.perspective(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, hideOff ? empty : held)
				.end().guiLight(BlockModel.GuiLight.FRONT);
	}

}
