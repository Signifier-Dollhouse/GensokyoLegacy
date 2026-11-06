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
 * plus one flat wheel icon per mode, both selected by {@link AnchorItem#displayPredicate}.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class AnchorModel {

	/**
	 * The hand-authored Blockbench geometry both wrappers parent to.
	 */
	private static final String GEOMETRY = "custom/anchor";

	/**
	 * The iron wrapper, shared by every mode: only the flat GUI icon changes.
	 */
	private static final String HELD_MODEL = "custom/minamitsu_anchor";

	/**
	 * The water wrapper, drawn only by the storm anchor's projectile twin.
	 */
	private static final String WET_MODEL = "custom/minamitsu_anchor_wet";

	/**
	 * GUI icons by mode, in {@link AnchorMode} order.
	 */
	private static final String[] ICONS = {
			"item/anchor/anchor_voyage",
			"item/anchor/anchor_destroy",
			"item/anchor/anchor_storm"
	};

	/**
	 * Wheel-only icons by mode, in {@link AnchorMode} order, for {@link AnchorItem#iconStack}.
	 */
	private static final String[] WHEEL_ICONS = {
			"item/anchor/anchor_icon_voyage",
			"item/anchor/anchor_icon_destroy",
			"item/anchor/anchor_icon_storm"
	};

	public static void model(DataGenContext<Item, AnchorItem> ctx, RegistrateItemModelProvider pvd) {
		wrapper(pvd, HELD_MODEL, "item/anchor/iron_anchor");
		wrapper(pvd, WET_MODEL, "item/anchor/water_anchor");
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
		// the wheel range sits behind the held range, marked by AnchorItem.iconStack's ICON
		for (int i = 0; i < modes.length; i++) {
			String name = "item/" + ctx.getName() + "_icon_" + modes[i].name().toLowerCase(Locale.ROOT);
			anchor.override()
					.predicate(GensokyoLegacy.loc("anchor_display"), modes.length + 1 + i)
					.model(pvd.withExistingParent(name, "item/generated")
							.texture("layer0", pvd.modLoc(WHEEL_ICONS[i])))
					.end();
		}
	}

	/**
	 * Generates one per-texture wrapper over {@link #GEOMETRY}: the parent owns the elements, the
	 * wrapper only resolves the {@code #all} skin.
	 */
	private static void wrapper(RegistrateItemModelProvider pvd, String name, String texture) {
		pvd.getBuilder(name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc(GEOMETRY)))
				.texture("all", pvd.modLoc(texture));
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
