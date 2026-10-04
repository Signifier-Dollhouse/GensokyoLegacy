package dev.xkmc.gensokyolegacy.content.item.common;

import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;

/**
 * The held-hand wiring both gloves share: a modelled hand while held, a flat sprite everywhere
 * else.
 *
 * <p>The two gloves have their <em>own</em> hand models — {@code models/custom/doll_glove_hand.json}
 * and {@code models/custom/dagger_glove_hand.json}, each exported from Blockbench — so this owns no
 * model of its own. What they do share is the shape of the item model built around one: a
 * {@code neoforge:separate_transforms} model whose base is the flat sprite (gui, selector wheel,
 * attack sidebar) and whose four hand displays point at the glove's hand model. The caller supplies
 * both that model and the skin to override its {@code #0}.
 *
 * <p>This sits in its own package beside {@code content/item/targeting/} — the other thing the two
 * gloves share — rather than in {@code content/item/glove/}, which is the doll glove's alone
 * (dagger_glove.md §9).
 *
 * <p>Vanilla replaces the <em>whole</em> item model when an override predicate matches, so a glove's
 * per-mode overrides are separate-transforms models too - a flat override would silently drop the
 * hand while in hand.
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

	/**
	 * Turns {@code builder} into a glove's separate-transforms model: {@code flat} as the base
	 * sprite, {@code handModel} skinned with {@code hand} on all four hand displays.
	 *
	 * @param flat      the glove's flat sprite, for every context that is not a hand
	 * @param handModel the glove's own Blockbench hand model, under {@code models/custom}
	 * @param hand      the glove's own hand skin, overriding that model's {@code #0}
	 */
	public static void perspectives(ItemModelBuilder builder, RegistrateItemModelProvider pvd,
			ResourceLocation flat, ResourceLocation handModel, ResourceLocation hand) {
		// one nested builder reused across all four perspectives: the datagen serialises
		// each perspective in place rather than writing it out, so the four entries come
		// out identical and nothing is emitted twice
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