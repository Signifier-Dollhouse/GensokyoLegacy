package dev.xkmc.gensokyolegacy.content.item.umbrella.wheel;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Raw wheel textures ({@code item/tool/border_umbrella_icon_*}) blitted
 * directly: these entries are wheel-only (never item-selector stacks), so no
 * model override is involved. Drawn 16x16 like an item icon.
 */
public final class UmbrellaWheelIcons {

	public static final ResourceLocation SELECT =
			GensokyoLegacy.loc("textures/item/tool/border_umbrella_icon_select.png");
	public static final ResourceLocation EDIT =
			GensokyoLegacy.loc("textures/item/tool/border_umbrella_icon_edit.png");

	private UmbrellaWheelIcons() {
	}

	public static ResourceLocation distanceIcon(int distance) {
		return switch (distance) {
			case 10, 100, 1000, 10000 ->
					GensokyoLegacy.loc("textures/item/tool/border_umbrella_icon_" + distance + ".png");
			default -> GensokyoLegacy.loc("textures/item/tool/border_umbrella_icon_1000.png");
		};
	}

	public static void render(GuiGraphics g, ResourceLocation texture) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		g.blit(texture, -8, -8, 16, 16, 0, 0, 16, 16, 16, 16);
	}

}
