package dev.xkmc.gensokyolegacy.content.item.dagger;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.gensokyolegacy.content.item.glovehand.GloveHandModel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;

import java.util.Locale;

/**
 * Item model generation for the dagger glove. Split out of {@code GLItems} for the same
 * reason {@code DollGloveModel} is: the glove is one of the two tools whose model is built
 * per mode rather than from one texture.
 *
 * <p>The held-mitten half is {@link GloveHandModel}'s, shared with the doll glove; this
 * class is only the three firing modes on top of it (dagger_glove.md §6, §8).
 *
 * <p>The per-mode held overrides are separate-transforms models for the same reason the
 * doll glove's are: vanilla replaces the whole item model when a predicate matches, so a
 * flat override would drop the mitten while in hand.
 */
public class DaggerGloveModel {

	/** This glove's own mitten skin, which is what the shared model is overridden to. */
	private static final ResourceLocation HAND_TEXTURE = GensokyoLegacy.loc("item/tool/dagger_glove_hand");

	public static void model(DataGenContext<Item, DaggerGloveItem> ctx, RegistrateItemModelProvider pvd) {
		var modes = DaggerGloveMode.values();
		var glove = pvd.getBuilder(ctx.getName());
		perspectives(glove, pvd, pvd.modLoc("item/tool/" + ctx.getName()));
		// vanilla also reverses the override list at bake time and returns the first match
		// with >= per predicate, so emit ascending values for exact per-mode matching
		for (int i = 0; i < modes.length; i++) {
			var name = "item/dagger_glove_" + modes[i].name().toLowerCase(Locale.ROOT);
			var held = pvd.getBuilder(name);
			perspectives(held, pvd, pvd.modLoc("item/tool/dagger_glove_" + modes[i].name().toLowerCase(Locale.ROOT)));
			glove.override()
					.predicate(GensokyoLegacy.loc("dagger_glove_display"), i + 1)
					.model(pvd.getExistingFile(pvd.modLoc(name)))
					.end();
		}
	}

	/**
	 * Turns {@code builder} into one held-mode model of the glove: {@code texture} as the
	 * flat base, the shared mitten on all four hand displays.
	 */
	private static void perspectives(ItemModelBuilder builder, RegistrateItemModelProvider pvd, ResourceLocation texture) {
		GloveHandModel.perspectives(builder, pvd, texture, HAND_TEXTURE);
	}

}