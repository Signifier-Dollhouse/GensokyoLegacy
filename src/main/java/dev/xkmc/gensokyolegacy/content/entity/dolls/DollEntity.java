package dev.xkmc.gensokyolegacy.content.entity.dolls;

import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.FollowOwnerGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class DollEntity extends PathfinderMob {
    private Player owner;

    public DollEntity(EntityType<? extends DollEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FollowOwnerGoal(this, 1.0f, 3.0f, 0.5f, this.owner));
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return this.owner;
    }
}
