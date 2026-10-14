package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollLoadout;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Renders the doll loadout hand slots at the model's hand locator bones. Reads the synced client
 * mirror via {@link DollLoadout#getLoadoutItem} — dolls deliberately bypass vanilla equipment
 * slots, so {@code getMainHandItem} stays empty.
 */
public class DollHeldItemLayer extends BlockAndItemGeoLayer<DollEntity> {

    public static final String RIGHT_HAND_BONE = "RightHandLocator";
    public static final String LEFT_HAND_BONE = "LeftHandLocator";

    private final DollRenderer dollRenderer;

    public DollHeldItemLayer(DollRenderer renderer) {
        super(renderer);
        this.dollRenderer = renderer;
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
     * The bone's own side, not always right: items whose model carries separate per-side transforms
     * render unmirrored on the left arm otherwise. The context picks <em>which</em> authored
     * transform; the {@code leftHand} flag in {@link #renderStackForBone} cancels the mirror it carries.
     */
    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, DollEntity doll) {
        return bone.getName().equals(LEFT_HAND_BONE)
                ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    /**
     * Renders the stack itself instead of {@code super}, which hardcodes {@code leftHand = false}.
     * The two locator bones are exact X-mirrors and vanilla cancels the authored mirror at apply
     * time, so the left-hand context alone would land the item one mirror too far — the flag undoes it.
     */
    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, DollEntity doll,
                                      MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        var left = bone.getName().equals(LEFT_HAND_BONE);
        poseStack.pushPose();
        poseStack.scale(0.8F, 0.8F, 0.8F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.translate(0.0D, 0.0D, -0.2D);
        if (!left && stack.is(GLItems.DOLL_LANCE.get()) && dollRenderer.consumeSpearParticle(doll)) {
            spawnSpearParticle(poseStack, bone, stack, doll, partialTick);
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(doll, stack,
                getTransformTypeForStack(bone, stack, doll), left, poseStack, itemBuffer(bufferSource),
                doll.level(), packedLight, packedOverlay, doll.getId());
        poseStack.popPose();
    }

    private void spawnSpearParticle(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                    DollEntity doll, float partialTick) {
        poseStack.pushPose();
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        var model = itemRenderer.getModel(stack, doll.level(), doll, doll.getId());
        ClientHooks.handleCameraTransforms(poseStack, model, getTransformTypeForStack(bone, stack, doll), false);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        Vec3 tip = dollRenderer.itemPointToWorld(doll, poseStack, new Vector3f(8F / 16, 2.5F / 16, -11F / 16),
                partialTick);
        Vec3 shaft = dollRenderer.itemPointToWorld(doll, poseStack, new Vector3f(8F / 16, 2.5F / 16, 17F / 16),
                partialTick);
        Vec3 direction = tip.subtract(shaft).normalize();
        doll.level().addParticle(GLParticles.SPEAR.get(), tip.x, tip.y, tip.z, direction.x, direction.y, direction.z);
        poseStack.popPose();
    }

    /**
     * While glowing, the model goes through {@link OutlineBufferSource}, whose outline source is one
     * shared buffer: asking it for a new render type ends the previous batch and drops later bones'
     * geometry. The item is no part of the silhouette, so route it to the main buffer source.
     */
    private static MultiBufferSource itemBuffer(MultiBufferSource bufferSource) {
        if (bufferSource instanceof OutlineBufferSource)
            return Minecraft.getInstance().renderBuffers().bufferSource();
        return bufferSource;
    }

}
