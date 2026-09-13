package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class LookAtDollOwnerGoal extends Goal {
    private final BaseDollEntity doll;
    private LivingEntity owner;

    public LookAtDollOwnerGoal(BaseDollEntity doll) {
        this.doll = doll;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.owner = this.doll.getOwner();
        return this.owner != null;
    }

    @Override
    public void tick() {
        this.doll.getLookControl().setLookAt(this.owner);
    }
}
