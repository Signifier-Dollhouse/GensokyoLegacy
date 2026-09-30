package dev.xkmc.gensokyolegacy.content.ui.dialog;

import dev.xkmc.gensokyolegacy.content.rpg.dialog.Dialog;
import dev.xkmc.gensokyolegacy.content.rpg.network.SimpleDialogToClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SimpleDialogScreen extends DialogScreen {

	public static void open(SimpleDialogToClient data) {
		show(new SimpleDialogScreen(data));
	}

	private final SimpleDialogToClient data;

	/**
	 * The dialog this step is showing, resolved out of the client's own synced
	 * registry. Null when it is gone, e.g. a datapack reload dropped it.
	 */
	private final @Nullable Dialog dialog;

	/**
	 * Option labels, in the order the server sent them - which is the order a
	 * click index refers to.
	 */
	private final List<Component> options;

	public SimpleDialogScreen(SimpleDialogToClient data) {
		super(data.session(), data.character());
		this.data = data;
		this.dialog = resolveDialog(data.dialog());
		List<Component> labels = new ArrayList<>();
		if (this.dialog != null) {
			var all = this.dialog.options();
			for (var i : data.options()) {
				if (i >= 0 && i < all.size()) labels.add(all.get(i).display());
			}
		}
		this.options = labels;
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
		return Optional.ofNullable(dialog).map(e -> Component.translatable(e.text()));
	}

}
