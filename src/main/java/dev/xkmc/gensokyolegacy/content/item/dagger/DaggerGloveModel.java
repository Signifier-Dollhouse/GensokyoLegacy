package dev.xkmc.gensokyolegacy.content.item.dagger;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.gensokyolegacy.content.item.common.GloveHandModel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;

import java.util.Locale;

/**
 * Item model generation for the dagger glove, built per mode: the held half wraps {@link GloveHandModel}
 * around the per-mode-skinned {@code dagger_glove_hand} model; {@link DaggerGloveItem#iconStack} icons stay flat.
 *
 * @see "doc/design/dagger_glove.md"
 */
public class DaggerGloveModel {

	/**
	 * This glove's own modelled hand, exported from Blockbench.
	 */
	private static final ResourceLocation HAND_MODEL = GensokyoLegacy.loc("custom/dagger_glove_hand");

	/**
	 * This glove's hand skins, one per mode, which is what that model's {@code #0} is overridden to.
	 */
	private static ResourceLocation handTexture(String mode) {
		return GensokyoLegacy.loc("item/dagger_glove/dagger_glove_hand_" + mode);
	}

	public static void model(DataGenContext<Item, DaggerGloveItem> ctx, RegistrateItemModelProvider pvd) {
		var modes = DaggerGloveMode.values();
		var glove = pvd.getBuilder(ctx.getName());
		perspectives(glove, pvd, pvd.modLoc("item/dagger_glove/" + ctx.getName()), modes[0].name().toLowerCase(Locale.ROOT));
		// vanilla reverses the override list at bake time and returns the first match with >= per
		// predicate, so emit ascending values for exact per-mode matching
		for (int i = 0; i < modes.length; i++) {
			var mode = modes[i].name().toLowerCase(Locale.ROOT);
			var name = "item/dagger_glove_" + mode;
			var held = pvd.getBuilder(name);
			perspectives(held, pvd, pvd.modLoc("item/dagger_glove/dagger_glove_" + mode), mode);
			glove.override()
					.predicate(GensokyoLegacy.loc("dagger_glove_display"), i + 1)
					.model(pvd.getExistingFile(pvd.modLoc(name)))
					.end();
		}
		for (int i = 0; i < modes.length; i++) {
			glove.override()
					.predicate(GensokyoLegacy.loc("dagger_glove_display"), modes.length + 1 + i)
					.model(pvd.withExistingParent("item/dagger_glove_icon_" + modes[i].name().toLowerCase(Locale.ROOT), "item/generated")
							.texture("layer0", pvd.modLoc("item/dagger_glove/dagger_glove_icon_" + modes[i].name().toLowerCase(Locale.ROOT))))
					.end();
		}
	}

	/**
	 * Turns {@code builder} into one held-mode model: {@code texture} as the flat base, the glove's
	 * own modelled hand in {@code mode}'s skin on all four hand displays.
	 */
	private static void perspectives(ItemModelBuilder builder, RegistrateItemModelProvider pvd,
									 ResourceLocation texture, String mode) {
		GloveHandModel.perspectives(builder, pvd, texture, HAND_MODEL, handTexture(mode));
	}

}