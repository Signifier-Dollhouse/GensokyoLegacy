package dev.xkmc.gensokyolegacy.content.item.anchor;

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
 * Item model generation for the anchor: one flat GUI icon per mode over the same Blockbench base,
 * selected by {@link AnchorItem#displayPredicate}.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class AnchorModel {

	/**
	 * The Blockbench anchor, shared by every mode: only the flat GUI icon changes.
	 */
	private static final String HELD_MODEL = "custom/minamitsu_anchor";

	/**
	 * GUI icons by mode, in {@link AnchorMode} order.
	 */
	private static final String[] ICONS = {
			"item/tool/iron_anchor_icon",
			"item/tool/anchor_icon_sink",
			"item/tool/anchor_icon_storm"
	};

	public static void model(DataGenContext<Item, AnchorItem> ctx, RegistrateItemModelProvider pvd) {
		var anchor = pvd.getBuilder(ctx.getName());
		gui(anchor, pvd, ICONS[0]);
		var modes = AnchorMode.values();
		for (int i = 0; i < modes.length; i++) {
			String name = "item/" + ctx.getName() + "_" + modes[i].name().toLowerCase(Locale.ROOT);
			var model = pvd.getBuilder(name);
			gui(model, pvd, ICONS[i]);
			// vanilla picks the first override whose predicate is >= its value, so emit ascending
			anchor.override()
					.predicate(GensokyoLegacy.loc("anchor_display"), i + 1)
					.model(pvd.getExistingFile(pvd.modLoc(name)))
					.end();
		}
	}

	private static void gui(ItemModelBuilder builder, RegistrateItemModelProvider pvd, String icon) {
		var base = pvd.nested()
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc(HELD_MODEL)));
		var guiModel = pvd.nested()
				.parent(new ModelFile.UncheckedModelFile("item/generated"))
				.texture("layer0", pvd.modLoc(icon));
		builder.customLoader(SeparateTransformsModelBuilder::begin)
				.base(base)
				.perspective(ItemDisplayContext.GUI, guiModel)
				.end().guiLight(BlockModel.GuiLight.FRONT);
	}

}
