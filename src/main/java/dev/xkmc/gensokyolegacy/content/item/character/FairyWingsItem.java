package dev.xkmc.gensokyolegacy.content.item.character;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.l2core.base.effects.EffectUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * A fairy's wings, given away as an item: the same hover as {@link IceWingsItem} one tier lower, paid for with
 * half of the wearer's health. The chest slot and the back slot each apply that on their own, so a wearer with a
 * pair in both is owed two halves and has none left.
 */
public class FairyWingsItem extends WingItem {

	/** A multiplier rather than a flat cut, so a health bonus from elsewhere is halved along with the base. */
	private static final double HEALTH = -0.5;

	public FairyWingsItem(Properties properties) {
		super(properties);
	}

	@Override
	protected void addModifiers(ItemAttributeModifiers.Builder builder) {
		builder.add(Attributes.MAX_HEALTH, new AttributeModifier(GensokyoLegacy.loc("fairy_wings"), HEALTH,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.CHEST);
	}

	@Override
	protected void tick(Player player) {
		EffectUtil.refreshEffect(player, new MobEffectInstance(GLEffects.FLOATING, DURATION, 0, true, true), player);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.FAIRY_WINGS_LORE.get());
	}

}