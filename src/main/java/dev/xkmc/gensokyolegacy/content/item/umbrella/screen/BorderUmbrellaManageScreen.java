package dev.xkmc.gensokyolegacy.content.item.umbrella.screen;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.UmbrellaUtil;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaDeletePacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaRenamePacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaReorderPacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class BorderUmbrellaManageScreen extends Screen {

	private static final int PANEL_WIDTH = 320;
	private static final int ROW_HEIGHT = 20;
	private static final int ROWS = BorderUmbrellaSlots.MAX_SLOTS;

	// mouse cache for wheel → screen transition, mirrors GolemWheelHandler.press
	private static boolean viaWheelCache = false;
	private boolean suppressNextClick = false;

	private int editingIndex = -1;
	private EditBox editBox;
	private Button saveButton;
	private Button cancelButton;

	public BorderUmbrellaManageScreen() {
		super(GLLang.ItemUmbrella.MANAGE_TITLE.get());
	}

	public static void open() {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new BorderUmbrellaManageScreen());
	}

	public static void openViaWheel() {
		viaWheelCache = true;
		open();
	}

	private ItemStack getHeld() {
		var player = Minecraft.getInstance().player;
		if (player == null) return null;
		return BorderUmbrellaSelectionListener.getHeldUmbrella(player);
	}

	private BorderUmbrellaSlots getSlots(ItemStack held) {
		if (held == null) return BorderUmbrellaSlots.defaultSlots();
		return held.getOrDefault(GLItems.UMBRELLA_SLOTS.get(), BorderUmbrellaSlots.defaultSlots());
	}

	@Override
	protected void setInitialFocus() {
		if (editingIndex >= 0 && editBox != null) {
			setInitialFocus(editBox);
			return;
		}
		super.setInitialFocus();
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		// mouse cache like GolemWheelHandler.press — consume the wheel side click that opened this screen
		if (viaWheelCache) {
			viaWheelCache = false;
			suppressNextClick = true; // also suppress the following mouseReleased
			return true;
		}
		if (suppressNextClick) {
			suppressNextClick = false;
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (suppressNextClick) {
			suppressNextClick = false;
			return true;
		}
		return super.mouseReleased(mx, my, button);
	}

	@Override
	protected void init() {
		clearWidgets();
		// handle mouse cache from wheel open
		if (viaWheelCache) {
			// keep viaWheelCache for first mouseClicked, but also ensure EditBox will be focused
			// don't clear here, let mouseClicked consume it
		}
		int centerX = width / 2;
		int startY = 30;
		int panelLeft = centerX - PANEL_WIDTH / 2;

		if (editingIndex >= 0) {
			// editing overlay — editBox must be first widget for setInitialFocus tab navigation
			int ew = 200;
			int eh = 20;
			int ex = centerX - ew / 2;
			int ey = height / 2 - 10;
			ItemStack held = getHeld();
			String cur = "";
			if (held != null) {
				var slot = getSlots(held).get(editingIndex);
				if (!slot.isEmptySlot()) cur = slot.name();
			}
			editBox = new EditBox(font, ex, ey, ew, eh, Component.literal("name"));
			editBox.setMaxLength(32);
			editBox.setValue(cur);
			editBox.setCanLoseFocus(false);
			editBox.setBordered(true);
			addRenderableWidget(editBox);
			saveButton = Button.builder(Component.translatable("gui.done"), b -> onSaveEdit())
					.bounds(ex, ey + 30, ew / 2 - 5, 20).build();
			cancelButton = Button.builder(Component.translatable("gui.cancel"), b -> cancelEdit())
					.bounds(ex + ew / 2 + 5, ey + 30, ew / 2 - 5, 20).build();
			addRenderableWidget(saveButton);
			addRenderableWidget(cancelButton);
			setInitialFocus(editBox);
			editBox.setFocused(true);
			return;
		}

		// Done button at bottom
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(centerX - 50, startY + ROWS * (ROW_HEIGHT + 2) + 10, 100, 20).build());

		// row buttons
		for (int i = 0; i < ROWS; i++) {
			int y = startY + i * (ROW_HEIGHT + 2);
			int btnY = y + 1;
			// captured index for lambda
			final int idx = i;
			ItemStack held = getHeld();
			var slot = getSlots(held).get(i);
			boolean empty = slot.isEmptySlot();
			boolean canUp = i > 0;
			boolean canDown = i < ROWS - 1;

			// Edit button
			Button edit = Button.builder(Component.literal("✎").withStyle(s -> s), b -> startEdit(idx))
					.bounds(panelLeft + PANEL_WIDTH - 100, btnY, 20, 18).build();
			edit.active = !empty;
			addRenderableWidget(edit);

			// Delete button
			Button del = Button.builder(Component.literal("X"), b -> onDelete(idx))
					.bounds(panelLeft + PANEL_WIDTH - 78, btnY, 20, 18).build();
			del.active = !empty;
			addRenderableWidget(del);

			// Up button
			Button up = Button.builder(Component.literal("↑"), b -> onMove(idx, idx - 1))
					.bounds(panelLeft + PANEL_WIDTH - 56, btnY, 18, 18).build();
			up.active = canUp;
			addRenderableWidget(up);

			// Down button
			Button down = Button.builder(Component.literal("↓"), b -> onMove(idx, idx + 1))
					.bounds(panelLeft + PANEL_WIDTH - 36, btnY, 18, 18).build();
			down.active = canDown;
			addRenderableWidget(down);
		}
	}

	private void startEdit(int idx) {
		editingIndex = idx;
		suppressNextClick = true; // cache mouse that clicked Edit button, like GolemWheelHandler.press
		rebuildWidgets();
	}

	private void cancelEdit() {
		editingIndex = -1;
		rebuildWidgets();
	}

	private void onSaveEdit() {
		if (editingIndex < 0 || editBox == null) return;
		String name = editBox.getValue();
		if (name == null) name = "";
		if (name.length() > 32) name = name.substring(0, 32);
		ItemStack held = getHeld();
		if (held != null) {
			var slot = getSlots(held).get(editingIndex);
			if (!slot.isEmptySlot()) {
				// optimistic client update
				UmbrellaUtil.renameSlot(held, editingIndex, name);
				GensokyoLegacy.HANDLER.toServer(new BorderUmbrellaRenamePacket(editingIndex, name));
			}
		}
		editingIndex = -1;
		rebuildWidgets();
	}

	private void onDelete(int idx) {
		ItemStack held = getHeld();
		if (held == null) return;
		var slot = getSlots(held).get(idx);
		if (slot.isEmptySlot()) return;
		UmbrellaUtil.deleteSlot(held, idx);
		GensokyoLegacy.HANDLER.toServer(new BorderUmbrellaDeletePacket(idx));
		rebuildWidgets();
	}

	private void onMove(int from, int to) {
		if (from < 0 || from >= ROWS || to < 0 || to >= ROWS) return;
		ItemStack held = getHeld();
		if (held == null) return;
		UmbrellaUtil.swapSlots(held, from, to);
		GensokyoLegacy.HANDLER.toServer(new BorderUmbrellaReorderPacket(from, to));
		rebuildWidgets();
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		super.render(g, mx, my, pt);
		g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
		if (editingIndex >= 0) {
			// dim background behind edit overlay is handled by super.render transparent background
			g.drawCenteredString(font, GLLang.ItemUmbrella.RENAME_TITLE.get(), width / 2, height / 2 - 40, 0xFFFFFF);
			// show which slot is being edited
			g.drawCenteredString(font, Component.literal("Slot " + editingIndex), width / 2, height / 2 - 28, 0xAAAAAA);
			return;
		}
		int centerX = width / 2;
		int startY = 30;
		int panelLeft = centerX - PANEL_WIDTH / 2;
		ItemStack held = getHeld();
		if (held == null) {
			g.drawCenteredString(font, Component.literal("No umbrella held"), centerX, startY + 20, 0xFF5555);
			return;
		}
		var slots = getSlots(held);
		for (int i = 0; i < ROWS; i++) {
			int y = startY + i * (ROW_HEIGHT + 2);
			var slot = slots.get(i);
			// row background
			int bg = (i % 2 == 0) ? 0x20FFFFFF : 0x30FFFFFF;
			// highlight selected slot
			int selected = held.getOrDefault(GLItems.UMBRELLA_SLOT_SELECTED.get(), 0);
			if (i == selected) bg = 0x40FFFF55;
			g.fill(panelLeft, y, panelLeft + PANEL_WIDTH, y + ROW_HEIGHT, bg);
			// index
			g.drawString(font, String.valueOf(i), panelLeft + 4, y + 6, 0xAAAAAA, false);
			// icon
			ItemStack icon = slot.isEmptySlot() ? ItemStack.EMPTY : slot.displayIcon();
			if (!icon.isEmpty()) {
				g.renderItem(icon, panelLeft + 18, y + 2);
			}
			// name and pos
			int textX = panelLeft + 38;
			if (slot.isEmptySlot()) {
				g.drawString(font, GLLang.ItemUmbrella.SLOT_EMPTY.get(), textX, y + 6, 0x777777, false);
			} else {
				Component name = Component.literal(slot.name());
				// truncate if too long
				int maxW = PANEL_WIDTH - 140;
				String nameStr = font.plainSubstrByWidth(name.getString(), maxW);
				g.drawString(font, nameStr, textX, y + 3, 0xFFFFFF, false);
				String posStr = slot.pos().getX() + "," + slot.pos().getY() + "," + slot.pos().getZ() + " [" + slot.dim().getPath() + "]";
				// dim path short
				g.drawString(font, posStr, textX, y + 12, 0xAAAAAA, false);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean keyPressed(int key, int scan, int mod) {
		if (editingIndex >= 0 && editBox != null) {
			if (key == 257 || key == 335) { // enter
				onSaveEdit();
				return true;
			}
			if (key == 256) { // esc
				cancelEdit();
				return true;
			}
			// route to editBox like AnvilScreen does
			if (editBox.keyPressed(key, scan, mod) || editBox.canConsumeInput()) {
				return true;
			}
		}
		return super.keyPressed(key, scan, mod);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (editingIndex >= 0 && editBox != null && editBox.canConsumeInput()) {
			return editBox.charTyped(codePoint, modifiers);
		}
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public void onClose() {
		if (editingIndex >= 0) {
			cancelEdit();
			return;
		}
		super.onClose();
	}
}
