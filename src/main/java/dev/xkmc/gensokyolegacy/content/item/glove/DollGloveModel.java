package dev.xkmc.gensokyolegacy.content.item.glove;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveMode;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;

/**
 * Item model generation for the doll glove. Split out of {@code GLItems} because the
 * glove is the one tool whose model is built per mode rather than from one texture.
 *
 * <p>The glove is a modelled mitten while held but a flat sprite everywhere else, so
 * the item is a {@code neoforge:separate_transforms} model: the base is the flat
 * sprite (gui, selector wheel, attack sidebar) and each of the four hand displays
 * swaps in {@link #HAND_MODEL}. That model carries its own transform for every hand
 * display, all tuned in Blockbench, so nothing is set here.
 *
 * <p>Vanilla replaces the <em>whole</em> item model when an override predicate
 * matches, so the held-mode overrides are separate-transforms models too - a flat
 * override would silently drop the mitten while in hand. Icon stacks are gui-only and
 * never held, so those overrides stay flat.
 */
public class DollGloveModel {

	/**
	 * The modelled mitten, exported from Blockbench. Its {@code #0} texture reference is
	 * what a child model overrides to restyle the mitten without touching the geometry.
	 */
	private static final ResourceLocation HAND_MODEL = GensokyoLegacy.loc("custom/doll_glove_hand");

	/**
	 * The four hand displays the glove is held in. Each needs its own perspective entry,
	 * otherwise the flat base sprite renders in hand.
	 */
	private static final ItemDisplayContext[] HANDS = {
			ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
			ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
	};

	public static void model(DataGenContext<Item, DollGloveItem> ctx, RegistrateItemModelProvider pvd) {
		var modes = DollGloveMode.values();
		var glove = pvd.getBuilder(ctx.getName());
		perspectives(glove, pvd, pvd.modLoc("item/tool/" + ctx.getName()));
		// vanilla also reverses the override list at bake time and returns the first match
		// with >= per predicate, so emit ascending values for exact per-mode matching
		for (int i = 0; i < modes.length; i++) {
			var name = "item/glove_" + modes[i].iconName();
			var held = pvd.getBuilder(name);
			perspectives(held, pvd, pvd.modLoc("item/tool/glove_" + modes[i].iconName()));
			glove.override()
					.predicate(GensokyoLegacy.loc("glove_display"), i + 1)
					.model(pvd.getExistingFile(pvd.modLoc(name)))
					.end();
		}
		for (int i = 0; i < modes.length; i++) {
			glove.override()
					.predicate(GensokyoLegacy.loc("glove_display"), modes.length + 1 + i)
					.model(pvd.withExistingParent("item/glove_icon_" + modes[i].iconName(), "item/generated")
							.texture("layer0", pvd.modLoc("item/tool/glove_icon_" + modes[i].iconName())))
					.end();
		}
	}

	/**
	 * Turns {@code builder} into the glove's separate-transforms model: {@code texture} as
	 * the flat base, {@link #HAND_MODEL} on all four hand displays.
	 */
	private static void perspectives(ItemModelBuilder builder, RegistrateItemModelProvider pvd, ResourceLocation texture) {
		var glove = builder.customLoader(SeparateTransformsModelBuilder::begin);
		for (var hand : HANDS) glove.perspective(hand, pvd.nested().parent(new ModelFile.UncheckedModelFile(HAND_MODEL)));
		glove.base(pvd.nested()
				.parent(new ModelFile.UncheckedModelFile("item/generated"))
				.texture("layer0", texture))
				.end().guiLight(BlockModel.GuiLight.FRONT);
	}

}
