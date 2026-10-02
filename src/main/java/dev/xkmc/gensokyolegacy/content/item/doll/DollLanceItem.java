package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollMeleeBehavior;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The dolls' polearm, and the only melee weapon {@link DollMeleeBehavior} will charge with
 * ({@code DollBehaviors}, doc/design/doll/control.md §5.1b) — a sword in the hand no longer
 * arms a doll at all.
 * <p>
 * {@link #ATTRIBUTES} is a flat main-hand pair in the shape vanilla gives every melee weapon,
 * so the same numbers come out of three places: the tooltip, a player's own swing, and the
 * doll charge, whose damage sums the held stack's main-hand attack-damage modifiers rather
 * than reading a damage getter off the item. The vanilla modifier ids matter as much as the
 * amounts — {@code ItemStack.addModifierTooltip} folds the holder's own attribute base into
 * any modifier carrying {@link Item#BASE_ATTACK_DAMAGE_ID} / {@link Item#BASE_ATTACK_SPEED_ID},
 * which is what makes the line read "6 Attack Damage, 1 Attack Speed" instead of "+6 / -3".
 * <p>
 * No durability: {@code Properties.durability} is never called, so a stack has no
 * {@code max_damage} and no swing can wear it. It stacks like any other non-tool item.
 */
public class DollLanceItem extends Item {

	/**
	 * Damage over a bare hand. On a player (base 1) that totals 7 per swing; a doll charge adds
	 * its own 2.0 base on top, for 8.
	 */
	public static final double DAMAGE = 6.0;

	/**
	 * A swing a second: the player base of 4 lands on 1, which is what the tooltip prints.
	 * The reach is meant to make up for the pace.
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
		super(properties.attributes(ATTRIBUTES));
	}

}