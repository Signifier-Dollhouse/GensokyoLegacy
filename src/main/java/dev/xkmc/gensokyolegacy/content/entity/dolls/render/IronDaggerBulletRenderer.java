package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.danmakuapi.content.entity.ItemBulletRenderer;
import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class IronDaggerBulletRenderer extends ItemBulletRenderer<IronDaggerBulletEntity> {
    public IronDaggerBulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    public Vec3 getRenderOffset(IronDaggerBulletEntity e, float f) {
        return new Vec3(0.0F, 1.0F, 0.0F);
    }
}
