package dev.xkmc.gensokyolegacy.content.item.character;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

/**
 * Cirno's hairband. No longer draws anything: the band is a bone of her own geo rig
 * (see {@code CirnoModel}), so there is no separate model or texture left for this
 * item to point at. What it still does is the magic-hit freeze.
 */
public class CirnoHairbandItem extends TouhouHatItem {

	public CirnoHairbandItem(Properties properties) {
		super(properties);
	}

	@Override
	protected void tick(ItemStack stack, Level level, Player player) {
		//if (player.tickCount % 20 == 0)
		//	GLMechanics.ICE_FAIRY.get().startOrAdvance(player, 2000, 20);
	}

	@Override
	public void onHurtTarget(ItemStack head, DamageSource source, LivingEntity target) {
		if (source.is(Tags.DamageTypes.IS_MAGIC)) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
			if (target.canFreeze()) {
				target.setTicksFrozen(target.getTicksFrozen() + 120);
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		//RolePlayHandler.addTooltips(list, GLLang.ItemCommon.USAGE_CIRNO_HAIRBAND.get(GLMechanics.ICE_FAIRY.get().getName()), null);
	}

	@Override
	public boolean support(DyeColor color) {
		return color == DyeColor.LIGHT_BLUE;
	}

}
