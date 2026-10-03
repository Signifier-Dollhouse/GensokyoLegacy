package dev.xkmc.gensokyolegacy.content.item.glove;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveMode;
import dev.xkmc.gensokyolegacy.content.item.glovehand.GloveHandModel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;

/**
	 * Item model generation for the doll glove. Split out of {@code GLItems} because the
	 * glove is one of the two tools whose model is built per mode rather than from one texture.
	 *
	 * <p>The held-hand half is {@link GloveHandModel}'s wiring around this glove's own
	 * {@code custom/doll_glove_hand} model; this class is only what is specific to the doll
	 * glove, namely its four modes and the icon variants the wheel shows (glove.md §3b).
	 *
	 * <p>Per-mode held overrides are separate-transforms models rather than flat ones, because
	 * vanilla replaces the whole item model when a predicate matches - a flat override would
	 * drop the hand while in hand. Icon stacks are gui-only and never held, so those stay flat.
	 */
public class DollGloveModel {

	/** This glove's own modelled hand, exported from Blockbench. */
	private static final ResourceLocation HAND_MODEL = GensokyoLegacy.loc("custom/doll_glove_hand");

	/** This glove's own hand skin, which is what that model's {@code #0} is overridden to. */
	private static final ResourceLocation HAND_TEXTURE = GensokyoLegacy.loc("item/doll_glove/glove_hand");

	public static void model(DataGenContext<Item, DollGloveItem> ctx, RegistrateItemModelProvider pvd) {
		var modes = DollGloveMode.values();
		var glove = pvd.getBuilder(ctx.getName());
		perspectives(glove, pvd, pvd.modLoc("item/doll_glove/" + ctx.getName()));
		// vanilla also reverses the override list at bake time and returns the first match
		// with >= per predicate, so emit ascending values for exact per-mode matching
		for (int i = 0; i < modes.length; i++) {
			var name = "item/glove_" + modes[i].iconName();
			var held = pvd.getBuilder(name);
			perspectives(held, pvd, pvd.modLoc("item/doll_glove/glove_" + modes[i].iconName()));
			glove.override()
					.predicate(GensokyoLegacy.loc("glove_display"), i + 1)
					.model(pvd.getExistingFile(pvd.modLoc(name)))
					.end();
		}
		for (int i = 0; i < modes.length; i++) {
			glove.override()
					.predicate(GensokyoLegacy.loc("glove_display"), modes.length + 1 + i)
					.model(pvd.withExistingParent("item/glove_icon_" + modes[i].iconName(), "item/generated")
							.texture("layer0", pvd.modLoc("item/doll_glove/glove_icon_" + modes[i].iconName())))
					.end();
		}
	}

	/**
	 * Turns {@code builder} into one held-mode model of the glove: {@code texture} as the
	 * flat base, the glove's own modelled hand on all four hand displays.
	 */
	private static void perspectives(ItemModelBuilder builder, RegistrateItemModelProvider pvd, ResourceLocation texture) {
		GloveHandModel.perspectives(builder, pvd, texture, HAND_MODEL, HAND_TEXTURE);
	}

}
