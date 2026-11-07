package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.StrayDollEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Renders the stray doll's main-hand item at the rig's right hand locator.
 */
public class StrayDollHeldItemLayer extends BlockAndItemGeoLayer<StrayDollEntity> {

	public StrayDollHeldItemLayer(GeoRenderer<StrayDollEntity> renderer) {
		super(renderer);
	}

	@Override
	@Nullable
	protected ItemStack getStackForBone(GeoBone bone, StrayDollEntity doll) {
		if (!bone.getName().equals(DollHeldItemLayer.RIGHT_HAND_BONE)) return null;
		ItemStack stack = doll.getMainHandItem();
		return stack.isEmpty() ? null : stack;
	}

	@Override
	protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, StrayDollEntity doll) {
		return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

}
