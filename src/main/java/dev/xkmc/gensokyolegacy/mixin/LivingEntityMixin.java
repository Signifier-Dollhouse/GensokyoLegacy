package dev.xkmc.gensokyolegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.xkmc.gensokyolegacy.content.entity.foundation.ISleepOffsetEntity;
import dev.xkmc.gensokyolegacy.event.MixinHookHandlers;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.util.LavaEffectsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Shadow
	public abstract boolean hasEffect(MobEffect effect);

	@Inject(at = @At("HEAD"), method = "setPosToBed", cancellable = true, require = 0)
	public void gensokyolegacy$setPosToBed(BlockPos pos, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		// Vanilla hardcodes 0.6875 (vanilla mattress top 9/16 + 2/16 gap);
		// our beds are flat, so ask the entity for its own sleep height.
		if (self instanceof ISleepOffsetEntity e) {
			e.setPosToBedImpl(pos);
			ci.cancel();
		}
	}

	@Inject(at = @At("HEAD"), method = "canBeSeenAsEnemy", cancellable = true)
	public void gensokyolegacy$canBeSeenAsEnemy$unconscious(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player player && hasEffect(GLEffects.UNCONSCIOUS.value())) {
			cir.setReturnValue(false);
		}
	}

	@Inject(at = @At("HEAD"), method = "canAttack", cancellable = true)
	public void gensokyolegacy$canAttack$stun(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
		if (hasEffect(GLEffects.STUN.value())) {
			cir.setReturnValue(false);
		}
	}

	// 1.20.1 has no getGravity(); travel reads the ENTITY_GRAVITY attribute's value directly.
	@WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getValue()D", ordinal = 0))
	public double gensokyoLegacy$gravity(AttributeInstance instance, Operation<Double> original) {
		return MixinHookHandlers.getGravity((LivingEntity) (Object) this, original.call(instance));
	}

	@WrapOperation(method = "getFrictionInfluencedSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFlyingSpeed()F"))
	public float gensokyoLegacy$flyingSpeed(LivingEntity instance, Operation<Float> original) {
		return MixinHookHandlers.getFlyingSpeed(instance, original.call(instance));
	}

	@WrapOperation(method = "dropFromLootTable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"))
	public void gensokyoLegacy$dropIncrease(LootTable instance, LootParams params, long seed, Consumer<ItemStack> output, Operation<Void> original, @Local(argsOnly = true) DamageSource source) {
		MixinHookHandlers.alterDrops(source, seed, output, (s, c) -> original.call(instance, params, s, c));
	}

	// 1.20.1 keeps the jump/sink defaults on IForgeLivingEntity, which Mixin 0.8.5 cannot inject into.
	// Declaring them here overrides the defaults for LivingEntity and every subclass.
	public void jumpInFluid(FluidType type) {
		LivingEntity self = (LivingEntity) (Object) this;
		double y = 0.04F * self.getAttributeValue(ForgeMod.SWIM_SPEED.get());
		if (type == ForgeMod.LAVA_TYPE.get()) y = LavaEffectsHelper.lavaSwim(self, y);
		self.setDeltaMovement(self.getDeltaMovement().add(0, y, 0));
	}

	public void sinkInFluid(FluidType type) {
		LivingEntity self = (LivingEntity) (Object) this;
		double y = -0.04F * self.getAttributeValue(ForgeMod.SWIM_SPEED.get());
		if (type == ForgeMod.LAVA_TYPE.get()) y = LavaEffectsHelper.lavaSwim(self, y);
		self.setDeltaMovement(self.getDeltaMovement().add(0, y, 0));
	}

	@WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V", ordinal = 1))
	public void gensokyolegacy$travel$lavaSwim(LivingEntity instance, float v, Vec3 vec3, Operation<Void> original) {
		v = LavaEffectsHelper.lavaSwim(instance, v);
		original.call(instance, v, vec3);
	}

}
