package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Base class of all gift items. A gift can be given to a character to raise
 * trust (see GiftModule); the same item can also be used by the player
 * directly — subclasses override {@link #use} / {@link #useOn} for their own
 * self-use behaviour.
 */
public abstract class AbstractGift extends Item {

	private final GiftType giftType;
	private final int favor;
	private final int cooldown;

	public AbstractGift(Properties properties, GiftType giftType, int favor) {
		this(properties, giftType, favor, 1000);
	}

	public AbstractGift(Properties properties, GiftType giftType, int favor, int cooldown) {
		super(properties);
		this.giftType = giftType;
		this.favor = favor;
		this.cooldown = cooldown;
	}

	/** Category of this gift; determines per-character preference. */
	public GiftType getGiftType() {
		return giftType;
	}

	/** Base favor gained when given to a character with neutral preference. */
	public int getBaseFavor() {
		return favor;
	}

	/** Cooldown in ticks before the same character can receive another gift. */
	public int getGiftCooldown() {
		return cooldown;
	}

	/** Actual favor for a specific character, applying that character's preference. */
	public int getFavor(YoukaiEntity e) {
		double mult = GiftPreference.get(e, giftType);
		return (int) Math.round(favor * mult);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ITEM$GIFT_FAVOR.get(favor).withStyle(ChatFormatting.GOLD));
		list.add(GLLang.ITEM$GIFT_TYPE.get(giftType.getDisplay()).withStyle(ChatFormatting.GRAY));
	}

}
