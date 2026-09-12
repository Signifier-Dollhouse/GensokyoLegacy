package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class DollMoveControl extends MoveControl {
    public DollMoveControl(BaseDollEntity doll) {
        super(doll);
    }

    @Override
    public void tick() {
        if (this.operation == Operation.MOVE_TO) {
            Vec3 toNextNode = new Vec3(this.wantedX - mob.getX(), this.wantedY - this.mob.getY(), this.wantedZ - this.mob.getZ());
            double distance = toNextNode.length();
            if (distance < 0.25) {
                this.operation = Operation.WAIT;
                this.mob.setDeltaMovement(0, 0, 0);
                return;
            }
            double targetSpeed = Math.min(this.speedModifier, distance * 0.3);
            Vec3 targetVelocity = toNextNode.scale(targetSpeed / distance);
            Vec3 currentVelocity = this.mob.getDeltaMovement();
            this.mob.setDeltaMovement(currentVelocity.scale(0.8).add(targetVelocity.scale(0.2)));
            mob.setYRot(-(float)(Mth.atan2(currentVelocity.x, currentVelocity.z)) * (180F / (float)Math.PI));
            mob.yBodyRot = mob.getYRot();
        }
    }
}
