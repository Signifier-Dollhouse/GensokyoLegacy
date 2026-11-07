package dev.xkmc.gensokyolegacy.content.item.ghost;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

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
		return YuyukoFanItem.iconStack(this);
	}

	/**
	 * Held texture for this mode under {@code item/yuyuko_fan/}: summon and haunt borrow the
	 * haunting ghost's skin, scatter has its own, absorb keeps the default fan.
	 */
	public String textureName() {
		return switch (this) {
			case SUMMON, HAUNT -> "haunting";
			case SCATTER -> "scatter";
			case ABSORB -> "yuyuko_fan";
		};
	}

	/**
	 * Sub-model id suffix for this mode's selector icon ({@code item/yuyuko_fan/<name>_icon}).
	 */
	public String iconName() {
		return switch (this) {
			case HAUNT -> "haunting";
			default -> name().toLowerCase(Locale.ROOT);
		};
	}

	public static YuyukoFanMode byId(int id) {
		return id < 0 || id >= VALUES.length ? SUMMON : VALUES[id];
	}

}
