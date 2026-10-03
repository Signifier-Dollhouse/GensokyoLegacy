package dev.xkmc.gensokyolegacy.content.item.glovehand;

import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;

/**
 * The glove hand model both gloves wear while held, shared by the doll glove and the
 * dagger glove.
 *
 * <p>Both are the same mitten: the two skins differ only in colour, so the geometry and
 * every display transform live here once and each glove supplies only its own {@code #0}.
 * This sits in its own package beside {@code content/item/targeting/} — the other thing
 * the two gloves share — rather than in {@code content/item/glove/}, which is the doll
 * glove's alone (dagger_glove.md §9).
 *
 * <p>The mitten is modelled while held but a flat sprite everywhere else, so a glove item
 * is a {@code neoforge:separate_transforms} model: the base is the flat sprite (gui,
 * selector wheel, attack sidebar) and each of the four hand displays swaps in
 * {@link #HAND_MODEL}. That model carries its own transform for every hand display, all
 * tuned in Blockbench, so nothing is set here.
 *
 * <p>Vanilla replaces the <em>whole</em> item model when an override predicate matches, so
 * a glove's per-mode overrides are separate-transforms models too - a flat override would
 * silently drop the mitten while in hand.
 */
public class GloveHandModel {

	/**
	 * The modelled mitten, exported from Blockbench, shared by every glove. Its {@code #0}
	 * texture reference is what a child model overrides to restyle the mitten without
	 * touching the geometry; the value baked in here is the doll glove's own skin, so that
	 * is the mitten a glove gets if it does not override.
	 */
	public static final ResourceLocation HAND_MODEL = GensokyoLegacy.loc("custom/glove_hand");

	/**
	 * The four hand displays a glove is held in. Each needs its own perspective entry,
	 * otherwise the flat base sprite renders in hand.
	 */
	private static final ItemDisplayContext[] HANDS = {
			ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
			ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
	};

	/**
	 * Turns {@code builder} into a glove's separate-transforms model: {@code flat} as the
	 * base sprite, {@link #HAND_MODEL} skinned with {@code hand} on all four hand displays.
	 *
	 * @param flat the glove's flat sprite, for every context that is not a hand
	 * @param hand the glove's own mitten skin, overriding the shared model's {@code #0}
	 */
	public static void perspectives(ItemModelBuilder builder, RegistrateItemModelProvider pvd,
			ResourceLocation flat, ResourceLocation hand) {
		// one nested builder reused across all four perspectives: the datagen serialises
		// each perspective in place rather than writing it out, so the four entries come
		// out identical and nothing is emitted twice
		var mitten = pvd.nested()
				.parent(new ModelFile.UncheckedModelFile(HAND_MODEL))
				.texture("0", hand);
		var glove = builder.customLoader(SeparateTransformsModelBuilder::begin);
		for (var context : HANDS) glove.perspective(context, mitten);
		glove.base(pvd.nested()
				.parent(new ModelFile.UncheckedModelFile("item/generated"))
				.texture("layer0", flat))
				.end().guiLight(BlockModel.GuiLight.FRONT);
	}

}