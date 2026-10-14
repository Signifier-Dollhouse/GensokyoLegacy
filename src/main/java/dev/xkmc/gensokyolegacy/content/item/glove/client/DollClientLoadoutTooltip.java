package dev.xkmc.gensokyolegacy.content.item.glove.client;

import dev.xkmc.gensokyolegacy.content.entity.dolls.menu.DollLoadoutMenu;
import dev.xkmc.l2core.base.menu.base.MenuLayoutConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Renders {@link DollLoadoutTooltip} in the loadout menu's cross layout — core top-center, main left, cloth
 * center, off right — mirroring ModularGolems' equipment tooltip. Every cell draws the atlas' {@code slot}
 * frame, plus its {@code slotbg_*} ghost at +1,+1 when empty; a filled cell draws the item with decorations.
 */
public record DollClientLoadoutTooltip(DollLoadoutTooltip inv) implements ClientTooltipComponent {

	@Override
	public int getHeight() {
		return 2 * 18 + 2;
	}

	@Override
	public int getWidth(Font font) {
		return 3 * 18;
	}

	@Override
	public void renderImage(Font font, int mx, int my, GuiGraphics g) {
		renderSlot(g, font, mx + 18, my, inv.core(), "slotbg_core");
		renderSlot(g, font, mx, my + 18, inv.main(), "slotbg_main");
		renderSlot(g, font, mx + 18, my + 18, inv.cloth(), "slotbg_cloth");
		renderSlot(g, font, mx + 36, my + 18, inv.off(), "slotbg_off");
	}

	private static void renderSlot(GuiGraphics g, Font font, int x, int y, ItemStack stack, String bg) {
		blit(g, x, y, "slot");
		if (stack.isEmpty()) {
			blit(g, x + 1, y + 1, bg);
			return;
		}
		g.renderItem(stack, x + 1, y + 1, 0);
		g.renderItemDecorations(font, stack, x + 1, y + 1);
	}

	private static void blit(GuiGraphics g, int x, int y, String name) {
		var level = Minecraft.getInstance().level;
		if (level == null) return;
		var sprite = DollLoadoutMenu.MANAGER;
		var tex = MenuLayoutConfig.getTexture(sprite.id());
		var side = sprite.get(level.registryAccess()).getSide(name);
		g.blit(tex, x, y, side.x, side.y, side.w, side.h);
	}

}
