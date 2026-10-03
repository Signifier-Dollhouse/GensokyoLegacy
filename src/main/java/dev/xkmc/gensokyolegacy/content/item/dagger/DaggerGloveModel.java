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
 * <p>The held-hand half is {@link GloveHandModel}'s wiring around this glove's own
 * {@code custom/dagger_glove_hand} model — a hand with three daggers fanned out of it, not
 * the doll glove's bare mitten. This class is the three firing modes on top of it
 * (dagger_glove.md §6, §8).
 *
 * <p>Every mode gets its own hand skin, so the hand displays are per-mode too. The per-mode
 * held overrides are separate-transforms models rather than flat ones, because vanilla replaces
 * the whole item model when a predicate matches - a flat override would drop the hand while
 * in hand. The wheel icons behind {@link DaggerGloveItem#iconStack} are flat, since an icon
 * stack is gui-only and never held.
 *
 * <p>All of the glove's art sits under {@code item/dagger_glove/}, beside the umbrella's and
 * the doll glove's.
 */
public class DaggerGloveModel {

	/** This glove's own modelled hand, exported from Blockbench. */
	private static final ResourceLocation HAND_MODEL = GensokyoLegacy.loc("custom/dagger_glove_hand");

	/** This glove's hand skins, one per mode, which is what that model's {@code #0} is overridden to. */
	private static ResourceLocation handTexture(String mode) {
		return GensokyoLegacy.loc("item/dagger_glove/dagger_glove_hand_" + mode);
	}

	public static void model(DataGenContext<Item, DaggerGloveItem> ctx, RegistrateItemModelProvider pvd) {
		var modes = DaggerGloveMode.values();
		var glove = pvd.getBuilder(ctx.getName());
		perspectives(glove, pvd, pvd.modLoc("item/dagger_glove/" + ctx.getName()), modes[0].name().toLowerCase(Locale.ROOT));
		// vanilla also reverses the override list at bake time and returns the first match
		// with >= per predicate, so emit ascending values for exact per-mode matching
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
	 * Turns {@code builder} into one held-mode model of the glove: {@code texture} as the
	 * flat base, the glove's own modelled hand in {@code mode}'s skin on all four hand displays.
	 */
	private static void perspectives(ItemModelBuilder builder, RegistrateItemModelProvider pvd,
			ResourceLocation texture, String mode) {
		GloveHandModel.perspectives(builder, pvd, texture, HAND_MODEL, handTexture(mode));
	}

}