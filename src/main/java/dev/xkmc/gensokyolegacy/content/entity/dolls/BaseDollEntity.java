package dev.xkmc.gensokyolegacy.content.entity.dolls;

import dev.xkmc.gensokyolegacy.content.entity.behavior.goals.LookAtDollOwnerGoal;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.DollMoveControl;
import dev.xkmc.gensokyolegacy.content.entity.behavior.goals.FollowDollOwnerGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.UUID;

public abstract class BaseDollEntity extends PathfinderMob implements GeoEntity {
    protected static final RawAnimation IDLE_L = RawAnimation.begin().thenLoop("hover_idle_l");
    protected static final RawAnimation IDLE_R = RawAnimation.begin().thenLoop("hover_idle_r");
    protected static final RawAnimation MOVE_L = RawAnimation.begin().thenLoop("hover_move_l");
    protected static final RawAnimation MOVE_R = RawAnimation.begin().thenLoop("hover_move_r");
    private boolean isLeftie;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUUID;
    public final double stopDistance = 1.0;
    public final double speedModifier = 1.0;

    public BaseDollEntity(EntityType<? extends BaseDollEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        Random LEFTIE_ROLLER = new Random();
        this.isLeftie = LEFTIE_ROLLER.nextBoolean();
        this.moveControl = new DollMoveControl(this);
        this.setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(true);
        nav.setCanPassDoors(true);
        nav.setCanFloat(true);
        return nav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FollowDollOwnerGoal(this));
        this.goalSelector.addGoal(99999, new LookAtDollOwnerGoal(this));
    }

    public void setOwner(Player owner) {
        this.ownerUUID = owner.getUUID();
    }

    @Nullable
    public Player getOwnerPlayerObject() {
        if (this.ownerUUID == null) return null;
        if (this.level().isClientSide) return null;
        return this.level().getServer().getPlayerList().getPlayer(this.ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setNoGravity(true);
        if (compound.hasUUID("OwnerUUID")) {
            this.ownerUUID = compound.getUUID("OwnerUUID");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.ownerUUID != null) {
            compound.putUUID("OwnerUUID", this.ownerUUID);
        }
    }

    protected <E extends BaseDollEntity> PlayState dollAnimController(final AnimationState<E> event) {
        RawAnimation selectedAnim;
        if (event.isMoving()) {
            if (this.isLeftie) {
                selectedAnim = MOVE_L;
            }
            else {
                selectedAnim = MOVE_R;
            }
        }
        else {
            if (this.isLeftie) {
                selectedAnim = IDLE_L;
            }
            else {
                selectedAnim = IDLE_R;
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
