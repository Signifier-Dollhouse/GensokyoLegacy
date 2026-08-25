package dev.xkmc.gensokyolegacy.content.entity.behavior.goals;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class LookAtDollOwnerGoal extends Goal {
    private final BaseDollEntity doll;
    private Player owner;

    public LookAtDollOwnerGoal(BaseDollEntity doll) {
        this.doll = doll;
    }

    @Override
    public boolean canUse() {
        this.owner = this.doll.getOwnerPlayerObject();
        return this.owner != null;
    }

    @Override
    public void tick() {
        this.doll.getLookControl().setLookAt(this.owner);
    }
}
