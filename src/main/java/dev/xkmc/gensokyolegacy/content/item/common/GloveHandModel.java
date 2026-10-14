package dev.xkmc.gensokyolegacy.content.item.common;

import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;

/**
 * The held-hand wiring both gloves share: a modelled hand while held, a flat sprite everywhere else.
 * Each glove supplies its own hand model and skin; this builds the {@code neoforge:separate_transforms}
 * model around one - the flat sprite as the base, the four hand displays pointing at the glove's hand
 * model - and holds no model of its own.
 *
 * <p>Vanilla replaces the <em>whole</em> item model when an override predicate matches, so per-mode
 * overrides are separate-transforms models too: a flat override would drop the hand while in hand.
 *
 * @see "doc/design/dagger_glove.md"
 */
public class GloveHandModel {

	/**
	 * The four hand displays a glove is held in. Each needs its own perspective entry,
	 * otherwise the flat base sprite renders in hand.
	 */
	private static final ItemDisplayContext[] HANDS = {
			ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
			ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
	};

	/** Turns {@code builder} into a glove's separate-transforms model: {@code flat} as the base sprite and
	 * {@code handModel} skinned with {@code hand} on all four hand displays.
	 *
	 * @param flat      the glove's flat sprite, for every context that is not a hand
	 * @param handModel the glove's own Blockbench hand model, under {@code models/custom}
	 * @param hand      the glove's own hand skin, overriding that model's {@code #0}
	 */
	public static void perspectives(ItemModelBuilder builder, RegistrateItemModelProvider pvd,
			ResourceLocation flat, ResourceLocation handModel, ResourceLocation hand) {
		// one nested builder reused across all four perspectives: datagen serialises each in place
		// rather than writing it out, so the four entries come out identical and nothing is emitted twice
		var modelled = pvd.nested()
				.parent(new ModelFile.UncheckedModelFile(handModel))
				.texture("0", hand);
		var glove = builder.customLoader(SeparateTransformsModelBuilder::begin);
		for (var context : HANDS) glove.perspective(context, modelled);
		glove.base(pvd.nested()
				.parent(new ModelFile.UncheckedModelFile("item/generated"))
				.texture("layer0", flat))
				.end().guiLight(BlockModel.GuiLight.FRONT);
	}

}