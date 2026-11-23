package dev.xkmc.gensokyolegacy.content.item.devil;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * What the scroll's active half does: nothing, the superhuman melee package, or the devil's lasers.
 * The hand decides the attributes; the mode only decides which abilities run.
 *
 * @see "doc/design/item/devils_scroll.md"
 */
public enum DevilScrollMode {
	PASSIVE(GLLang.ItemDevilScroll.MODE_PASSIVE, GLLang.ItemDevilScroll.DESC_PASSIVE),
	SUPERHUMAN(GLLang.ItemDevilScroll.MODE_SUPERHUMAN, GLLang.ItemDevilScroll.DESC_SUPERHUMAN),
	DEVIL(GLLang.ItemDevilScroll.MODE_DEVIL, GLLang.ItemDevilScroll.DESC_DEVIL);

	private static final DevilScrollMode[] VALUES = values();

	private final GLLang.ItemDevilScroll name;
	private final GLLang.ItemDevilScroll desc;

	DevilScrollMode(GLLang.ItemDevilScroll name, GLLang.ItemDevilScroll desc) {
		this.name = name;
		this.desc = desc;
	}

	public Component getDisplayName() {
		return name.get();
	}

	public MutableComponent getDescription() {
		return desc.get();
	}

	/**
	 * A wheel icon stack for this mode: the scroll with the mode set and the mode's own texture.
	 */
	public ItemStack icon() {
		return DevilScrollItem.displayStack(this);
	}

	public static DevilScrollMode byId(int id) {
		return id < 0 || id >= VALUES.length ? PASSIVE : VALUES[id];
	}

}
