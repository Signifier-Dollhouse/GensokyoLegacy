package dev.xkmc.gensokyolegacy.content.item.umbrella.wheel;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.wheel.PersistentWheel;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import dev.xkmc.l2itemselector.wheel.WheelContext;
import dev.xkmc.l2itemselector.wheel.WheelKeyHandler;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaMode;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaSelectPacket;

public class BorderUmbrellaModeWheel implements PersistentWheel<BorderUmbrellaModeEntry> {

	private final ItemStack stack;

	public BorderUmbrellaModeWheel(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public boolean isValid(Player player) {
		var held = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		return held != null && held == stack || (held != null && held.getItem() instanceof BorderUmbrellaItem);
	}

	@Override
	public List<BorderUmbrellaModeEntry> getWheelContent() {
		var modes = BorderUmbrellaSelectionListener.getAvailableModes(stack);
		return modes.stream().map(BorderUmbrellaModeEntry::new).toList();
	}

	@Override
	public int getIndex(Player player) {
		ItemStack held = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		if (held == null) return -1;
		var mode = held.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		var avail = BorderUmbrellaSelectionListener.getAvailableModes(held);
		return avail.indexOf(mode);
	}

	@Override
	public void select(int index) {
		GensokyoLegacy.HANDLER.toServer(new BorderUmbrellaSelectPacket(0, index));
		// optimistic update
		var avail = BorderUmbrellaSelectionListener.getAvailableModes(stack);
		if (index >= 0 && index < avail.size()) {
			stack.set(GLItems.UMBRELLA_TYPE.get(), avail.get(index));
		}
	}

	@Override
	public WheelKeyHandler getInputHandler() {
		return new UmbrellaWheelKeyHandler();
	}

	@Override
	public @Nullable WheelAdaptor<?> getAtIndex(Player player, int index, boolean main) {
		if (!main) {
			if (index == 1) {
				ItemStack held = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
				if (held != null) return new BorderUmbrellaSlotWheel(held);
			}
			if (index == -1) {
				ItemStack held = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
				if (held != null) return new BorderUmbrellaDistanceWheel(held);
			}
			if (index == 2) {
				// fake wheel at index 2 for editing, mirrors GolemModeWheel side fake wheels
				return new UmbrellaFakeWheel(new ItemStack(Items.NAME_TAG), GLLang.ItemUmbrella.WHEEL_EDIT.get());
			}
		}
		return PersistentWheel.super.getAtIndex(player, index, main);
	}

	@Override
	public void renderImpl(GuiGraphics g, Player player, List<BorderUmbrellaModeEntry> list, WheelContext ctx) {
		PersistentWheel.super.renderImpl(g, player, list, ctx);
		int index = ctx.hover() >= 0 ? ctx.hover() : ctx.sel();
		if (index < 0) index = getIndex(player);
		if (index < 0 || index >= list.size()) return;
		int x0 = g.guiWidth() / 2, y0 = g.guiHeight() / 2;
		float r = Math.min(x0 / 1.5f, y0) / 1.5f;
		float s = r * 0.02f;
		g.pose().pushPose();
		g.pose().translate(x0, y0, 0);
		g.pose().scale(s, s, s);
		g.pose().translate(0, -8, 0);
		var entry = list.get(index);
		ItemStack icon = entry.mode().icon();
		g.renderItem(icon, -8, -8);
		g.pose().popPose();
		var text = entry.mode().displayName();
		var font = Minecraft.getInstance().font;
		int y = (int) (y0 + s * 3);
		for (var line : font.split(text, (int) r)) {
			g.drawString(font, line, x0 - font.width(line) / 2, y, 0xffffff, true);
			y += font.lineHeight + 1;
		}
	}

	@Override
	public void renderIcon(GuiGraphics g, int x0, int y0, boolean left, float sideWidth, boolean hover) {
		// wheel icon for main wheel: umbrella item
		ItemStack icon = new ItemStack(GLItems.BORDER_UMBRELLA.get());
		float cx = left ? sideWidth / 2f : g.guiWidth() - sideWidth / 2f;
		float r = Math.min((float) x0 / 1.5f, (float) y0) / 1.5f;
		float rs = r * 0.025f;
		float r0 = Math.min(sideWidth / 2f, r * 0.75f) * (hover ? 0.3f : 0.15f);
		if (r0 > 4) {
			g.pose().pushPose();
			g.pose().translate(cx, y0, 0);
			g.pose().scale(rs, rs, rs);
			g.renderItem(icon, -8, -8);
			g.pose().popPose();
		}
		if (hover) {
			int cx2 = left ? (int) (sideWidth / 2) : g.guiWidth() - (int) (sideWidth / 2);
			int ty = y0 + (int) r0 * 2;
			var font = Minecraft.getInstance().font;
			var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
			var text = mode.displayName();
			for (var line : font.split(text, (int) (sideWidth - 4))) {
				g.drawString(font, line, cx2 - font.width(line) / 2, ty, 0xffffff, true);
				ty += font.lineHeight + 1;
			}
		}
	}
}