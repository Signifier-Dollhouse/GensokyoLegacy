package dev.xkmc.gensokyolegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.RaidTrigger;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Raid.class)
public class RaidMixin {

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/critereon/PlayerTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;)V"))
	private void gensokyolegacy$onRaidWin(PlayerTrigger instance, ServerPlayer player, Operation<Void> original) {
		original.call(instance, player);
		GLMeta.QUEST.type().getOrCreate(player).dispatch(player, new RaidTrigger(player));
	}

}