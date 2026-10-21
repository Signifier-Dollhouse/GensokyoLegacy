package dev.xkmc.gensokyolegacy.content.item.character;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.l2core.base.effects.EffectUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class KoishiHatItem extends TouhouHatItem {

	public KoishiHatItem(Properties properties) {
		super(properties);
	}

	@Override
	public Holder<SoundEvent> getEquipSound() {
		return SoundEvents.ARMOR_EQUIP_IRON;
	}

	@Override
	public boolean isModeledHat() {
		return true;
	}

	@Override
	protected void tick(ItemStack stack, Level level, Player player) {
		if (player.getCooldowns().isOnCooldown(this)) return;
		EffectUtil.refreshEffect(player, new MobEffectInstance(GLEffects.UNCONSCIOUS, 40, 0,
				true, true), player);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.KOISHI_HAT_LORE.get());
	}

	@Override
	public boolean support(DyeColor color) {
		return color == DyeColor.RED || color == DyeColor.BLUE;
	}

}
