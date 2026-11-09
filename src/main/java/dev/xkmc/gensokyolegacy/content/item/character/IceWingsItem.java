package dev.xkmc.gensokyolegacy.content.item.character;

import net.minecraft.world.level.Level;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.l2library.base.effects.EffectUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Cirno's wings, given away as an item. Worn in the vanilla chest armor slot or the curios back slot;
 * both paths feed the same {@link #tick}, so the effects do not care which.
 *
 * <p>{@link GLEffects#BAKA} is Cirno's own: she counts to nine and nothing else, and a player in her
 * wings counts the same way, which is the joke.
 */
public class IceWingsItem extends WingItem {

	public IceWingsItem(Properties properties) {
		super(properties);
	}

	@Override
	protected void tick(Player player) {
		EffectUtil.refreshEffect(player, new MobEffectInstance(GLEffects.BAKA.get(), DURATION, 0, true, true),
				EffectUtil.AddReason.SELF, player);
		EffectUtil.refreshEffect(player, new MobEffectInstance(GLEffects.FLOATING.get(), DURATION, 1, true, true),
				EffectUtil.AddReason.SELF, player);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.ICE_WINGS_LORE.get());
	}

}