package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What the fan does on a right-click in the air: tend every summoned ghost, collect them all,
 * scatter butterfly danmaku, or send a haunting ghost at the marked target.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public enum YuyukoFanMode {
	SUMMON(GLLang.ItemYuyukoFan.MODE_SUMMON, new SummonFanHandler()),
	ABSORB(GLLang.ItemYuyukoFan.MODE_ABSORB, new AbsorbFanHandler()),
	SCATTER(GLLang.ItemYuyukoFan.MODE_SCATTER, new ScatterFanHandler()),
	HAUNT(GLLang.ItemYuyukoFan.MODE_HAUNT, new HauntFanHandler());

	private static final YuyukoFanMode[] VALUES = values();

	private final GLLang.ItemYuyukoFan name;
	public final YuyukoFanModeHandler handler;

	YuyukoFanMode(GLLang.ItemYuyukoFan name, YuyukoFanModeHandler handler) {
		this.name = name;
		this.handler = handler;
	}

	public Component getDisplayName() {
		return name.get();
	}

	/**
	 * A selector icon for this mode, named after it.
	 */
	public ItemStack icon() {
		ItemStack stack = handler.icon();
		stack.set(DataComponents.ITEM_NAME, getDisplayName());
		return stack;
	}

	public static YuyukoFanMode byId(int id) {
		return id < 0 || id >= VALUES.length ? SUMMON : VALUES[id];
	}

}
