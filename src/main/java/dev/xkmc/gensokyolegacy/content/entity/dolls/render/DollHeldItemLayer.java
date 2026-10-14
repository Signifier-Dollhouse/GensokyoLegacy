package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

import static dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiHeldItemLayer.itemBuffer;

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

    /**
     * The bone's own side, not always right: items whose model carries separate
     * per-side transforms render unmirrored on the left arm otherwise.
     * <p>
     * Pairs with the {@code leftHand} flag in {@link #renderStackForBone}: the context
     * picks <em>which</em> authored transform, the flag cancels the mirror it carries.
     */
    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, DollEntity doll) {
        return bone.getName().equals(LEFT_HAND_BONE)
                ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    /**
     * Renders the stack itself instead of delegating to {@code super}, which hardcodes
     * {@code leftHand = false} and so cannot reproduce vanilla's third-person left hand.
     * <p>
     * Vanilla authors {@code thirdperson_lefthand} as the mirror of
     * {@code thirdperson_righthand} and cancels that mirror at apply time, so both hands
     * get the <em>same</em> hand-relative transform and the mirrored arm bone does the
     * mirroring. The two locator bones are exact X-mirrors of each other, so asking for
     * the left-hand context alone lands the item one mirror too far; the flag is what
     * takes that extra mirror back out.
     */
    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, DollEntity doll,
                                      MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        var left = bone.getName().equals(LEFT_HAND_BONE);
        poseStack.pushPose();
        poseStack.scale(0.8F, 0.8F, 0.8F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.translate(0.0D, 0.0D, -0.2D);
        Minecraft.getInstance().getItemRenderer().renderStatic(doll, stack,
                getTransformTypeForStack(bone, stack, doll), left, poseStack, itemBuffer(bufferSource),
                doll.level(), packedLight, packedOverlay, doll.getId());
        poseStack.popPose();
    }
}
