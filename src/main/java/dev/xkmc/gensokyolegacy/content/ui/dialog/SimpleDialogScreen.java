package dev.xkmc.gensokyolegacy.content.ui.dialog;

import dev.xkmc.gensokyolegacy.content.rpg.network.SimpleDialogToClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public class SimpleDialogScreen extends DialogScreen {

	public static void open(SimpleDialogToClient data) {
		show(new SimpleDialogScreen(data));
	}

	private final SimpleDialogToClient data;

	/**
	 * The body text of this step and the labels of its options, both as the lang keys the server sent -
	 * the dialog is inline in the data files, so there is nothing to resolve against a registry.
	 */
	private final String text;
	private final List<Component> options;

	public SimpleDialogScreen(SimpleDialogToClient data) {
		super(data.session(), data.character());
		this.data = data;
		this.text = data.text();
		this.options = data.options().stream().<Component>map(Component::translatable).toList();
	}

	@Override
	public void renderDialog(GuiGraphics g, float pt, int mx, int my) {
		super.renderDialog(g, pt, mx, my);
		renderQuestInfo(g, data.quest());
	}

	@Override
	protected List<Component> getOptions() {
		return options;
	}

	@Override
	protected Optional<Component> getBodyText() {
		return Optional.of(Component.translatable(text));
	}

}
