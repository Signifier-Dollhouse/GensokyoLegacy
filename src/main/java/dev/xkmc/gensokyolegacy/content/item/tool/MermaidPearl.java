package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MermaidPearl extends Item {

	public MermaidPearl(Properties properties) {
		super(properties.durability(16));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer sp) {
			sp.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 1200, 0, false, false, true), sp);
			sp.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 1200, 0, false, false, true), sp);
			if (!player.isCreative()) {
				stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
			}
			player.getCooldowns().addCooldown(this, 100);
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.MERMAID_PEARL_LORE.get());
	}

}
