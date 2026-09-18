package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollLoadout;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Renders the doll loadout hand slots at the model's hand locator bones.
 * Reads the synced client mirror via {@link DollLoadout#getLoadoutItem} — dolls
 * deliberately bypass vanilla equipment slots, so {@code getMainHandItem} stays empty.
 */
public class DollHeldItemLayer extends BlockAndItemGeoLayer<DollEntity> {

	public static final String RIGHT_HAND_BONE = "RightHandLocator";
	public static final String LEFT_HAND_BONE = "LeftHandLocator";

	public DollHeldItemLayer(GeoRenderer<DollEntity> renderer) {
		super(renderer);
	}

	@Override
	@Nullable
	protected ItemStack getStackForBone(GeoBone bone, DollEntity doll) {
		ItemStack stack = switch (bone.getName()) {
			case RIGHT_HAND_BONE -> doll.getLoadoutItem(DollSlot.MAIN_HAND);
			case LEFT_HAND_BONE -> doll.getLoadoutItem(DollSlot.OFF_HAND);
			default -> ItemStack.EMPTY;
		};
		return stack.isEmpty() ? null : stack;
	}

	@Override
	protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, DollEntity doll) {
		return bone.getName().equals(LEFT_HAND_BONE) ?
				ItemDisplayContext.THIRD_PERSON_LEFT_HAND :
				ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

}
