package dev.xkmc.gensokyolegacy.content.item.gift;

import net.minecraft.world.item.Item;

/**
 * An obscure, hard-to-read magic book (GiftType.BOOK). Can be given to
 * characters, or burned as furnace fuel — roughly two lava buckets.
 */
public class MagicBookItem extends Item {

	public MagicBookItem(Properties properties) {
		super(properties.stacksTo(1));
	}

}
