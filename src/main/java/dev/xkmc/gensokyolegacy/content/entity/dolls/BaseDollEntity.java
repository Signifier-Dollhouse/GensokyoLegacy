package dev.xkmc.gensokyolegacy.content.entity.dolls;

import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.FollowOwnerGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public abstract class BaseDollEntity extends PathfinderMob implements GeoEntity {
    protected static final RawAnimation IDLE_L = RawAnimation.begin().thenLoop("hover_idle_l");
    protected static final RawAnimation IDLE_R = RawAnimation.begin().thenLoop("hover_idle_r");
    protected static final RawAnimation MOVE_L = RawAnimation.begin().thenLoop("hover_move_l");
    protected static final RawAnimation MOVE_R = RawAnimation.begin().thenLoop("hover_move_r");

    private boolean previousIdleR = false;
    private boolean previousMoveR = false;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Player owner;

    public BaseDollEntity(EntityType<? extends BaseDollEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.FOLLOW_RANGE, 48);
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

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Owner")) {
            UUID ownerId = compound.getUUID("Owner");
            if (this.level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(ownerId);
                if (owner != null) {
                    this.setOwner(owner);
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.owner != null) {
            compound.putUUID("Owner", this.owner.getUUID());
        }
    }

    protected <E extends BaseDollEntity> PlayState dollAnimController(final AnimationState<E> event) {
        RawAnimation selectedAnim;
        if (event.isMoving()) {
            if (previousMoveR) {
                selectedAnim = MOVE_L;
                previousMoveR = false;
            }
            else {
                selectedAnim = MOVE_R;
                previousMoveR = true;
            }
        }
        else {
            if (previousIdleR) {
                selectedAnim = IDLE_L;
                previousIdleR = false;
            }
            else {
                selectedAnim = IDLE_R;
                previousIdleR = true;
            }
        }
        return event.setAndContinue(selectedAnim);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "all", 3, this::dollAnimController));
        }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
