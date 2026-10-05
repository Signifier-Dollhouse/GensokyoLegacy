package dev.xkmc.gensokyolegacy.content.ui.dialog;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.DialogConfig;
import dev.xkmc.gensokyolegacy.content.rpg.network.FirstDialogToClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FirstDialogScreen extends DialogScreen {

	public static void open(FirstDialogToClient data) {
		show(new FirstDialogScreen(data));
	}

	private final FirstDialogToClient data;

	/**
	 * Option labels, in the order the server sent them - which is the order a
	 * click index refers to.
	 */
	private final List<Component> options;

	public FirstDialogScreen(FirstDialogToClient data) {
		super(data.session(), data.character());
		this.data = data;
		List<Component> labels = new ArrayList<>(data.options().size());
		for (var e : data.options()) {
			labels.add(e.display());
		}
		this.options = labels;
	}

	@Override
	public void renderDialog(GuiGraphics g, float pt, int mx, int my) {
		super.renderDialog(g, pt, mx, my);
		if (sel >= 0) {
			renderQuestInfo(g, data.options().get(sel).quest());
		}
	}

	@Override
	protected List<Component> getOptions() {
		return options;
	}

	@Override
	protected Optional<Component> getBodyText() {
		var body = data.body();
		if (body != null) return Optional.of(body);
		if (character == null) return Optional.empty();
		var cfg = DialogConfig.of(character.getType());
		if (cfg == null) return Optional.empty();
		// A guest is greeted as a guest; her home line would read as if the
		// player had come to see her.
		var key = character.isVisiting() && !cfg.visitGreeting().isEmpty()
				? cfg.visitGreeting()
				: cfg.greeting();
		if (key.isEmpty()) return Optional.empty();
		return Optional.of(Component.translatable(key));
	}

}
