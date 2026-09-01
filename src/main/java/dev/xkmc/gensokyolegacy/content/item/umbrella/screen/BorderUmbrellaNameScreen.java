package dev.xkmc.gensokyolegacy.content.item.umbrella.screen;

import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaRenamePacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BorderUmbrellaNameScreen extends Screen {

	private final int slot;
	private final String initialName;
	private EditBox editBox;

	public BorderUmbrellaNameScreen(int slot, String initialName) {
		super(GLLang.ItemUmbrella.RENAME_TITLE.get());
		this.slot = slot;
		this.initialName = initialName;
	}

	public static void open(int slot, String currentName) {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new BorderUmbrellaNameScreen(slot, currentName));
	}

	@Override
	protected void setInitialFocus() {
		if (editBox != null) {
			setInitialFocus(editBox);
			return;
		}
		super.setInitialFocus();
	}

	@Override
	protected void init() {
		int w = 200;
		int h = 20;
		int x = (width - w) / 2;
		int y = height / 2 - 10;
		editBox = new EditBox(font, x, y, w, h, Component.literal("name"));
		editBox.setMaxLength(32);
		editBox.setValue(initialName);
		editBox.setCanLoseFocus(false);
		addRenderableWidget(editBox);
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onDone())
				.bounds(x, y + 30, w / 2 - 5, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(x + w / 2 + 5, y + 30, w / 2 - 5, 20).build());
		setInitialFocus(editBox);
		editBox.setFocused(true);
	}

	private void onDone() {
		String name = editBox.getValue();
		GensokyoLegacy.HANDLER.toServer(new BorderUmbrellaRenamePacket(slot, name));
		onClose();
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		super.render(g, mx, my, pt);
		g.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean keyPressed(int key, int scan, int mod) {
		if (key == 257 || key == 335) { // enter
			onDone();
			return true;
		}
		if (key == 256) { // esc
			onClose();
			return true;
		}
		if (editBox != null && (editBox.keyPressed(key, scan, mod) || editBox.canConsumeInput())) {
			return true;
		}
		return super.keyPressed(key, scan, mod);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (editBox != null && editBox.canConsumeInput()) {
			return editBox.charTyped(codePoint, modifiers);
		}
		return super.charTyped(codePoint, modifiers);
	}
}