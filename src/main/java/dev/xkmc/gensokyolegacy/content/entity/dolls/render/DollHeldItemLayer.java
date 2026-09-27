package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollLoadout;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
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
		return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

	@Override
	protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, DollEntity doll,
									  MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
		super.renderStackForBone(poseStack, bone, stack, doll, itemBuffer(bufferSource),
				partialTick, packedLight, packedOverlay);
	}

	/**
	 * While glowing, the whole model is drawn through the {@link OutlineBufferSource}, whose outline
	 * source is a single shared {@code ByteBufferBuilder} with no fixed buffers: asking it for any new
	 * render type ends the batch of the previous type. Rendering the held item through it therefore
	 * ends the model's outline batch mid-model and leaves GeckoLib holding a dead consumer, which
	 * drops the geometry of the following bones (the arms). Also note GeckoLib's own
	 * {@code checkAndRefreshBuffer} only repairs the consumer at bone boundaries.
	 * <p>
	 * The item is not part of the silhouette anyway, so route it to the main buffer source (the same
	 * batch vanilla draws the normal body of a glowing entity into).
	 */
	private static MultiBufferSource itemBuffer(MultiBufferSource bufferSource) {
		if (bufferSource instanceof OutlineBufferSource)
			return Minecraft.getInstance().renderBuffers().bufferSource();
		return bufferSource;
	}

}
