package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollMeleeBehavior;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * The dolls' polearm, and the only melee weapon {@link DollMeleeBehavior} will charge with (control.md §5.1b);
 * a sword no longer arms a doll, so it is deliberately a plain {@link Item}.
 * <p>
 * {@link #ATTRIBUTES} is a flat main-hand pair in vanilla's melee shape, so the tooltip, a player's
 * swing and the doll charge read the same numbers; the vanilla base modifier ids matter as much
 * as the amounts.
 *
 * @see "doc/design/doll/item.md"
 */
public class DollLanceItem extends Item {

	public static final double DAMAGE = 4.0;

	/**
	 * A swing a second: the player base of 4 lands on 1, which is what the tooltip prints.
	 */
	public static final double ATTACK_SPEED = -3.0;

	public static final ItemAttributeModifiers ATTRIBUTES = ItemAttributeModifiers.builder()
			.add(Attributes.ATTACK_DAMAGE,
					new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, DAMAGE, AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.MAINHAND)
			.add(Attributes.ATTACK_SPEED,
					new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.MAINHAND)
			.build();

	public DollLanceItem(Properties properties) {
		super(properties.attributes(ATTRIBUTES).stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.DOLL_LANCE_LORE.get());
	}

}