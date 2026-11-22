package dev.xkmc.gensokyolegacy.content.ui.util;

import dev.xkmc.gensokyolegacy.util.GuiSprites;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

/**
 * Three-state sprite button: normal, hover (also used while selected) and disabled.
 */
public class SpriteButton extends Button {

	private final ResourceLocation normal, hover, disabled;
	private final BooleanSupplier selected;

	public SpriteButton(ResourceLocation normal, ResourceLocation hover, ResourceLocation disabled,
						int x, int y, int w, int h, OnPress onPress) {
		this(normal, hover, disabled, x, y, w, h, () -> false, onPress);
	}

	public SpriteButton(ResourceLocation normal, ResourceLocation hover, ResourceLocation disabled,
						int x, int y, int w, int h, BooleanSupplier selected, OnPress onPress) {
		super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
		this.normal = normal;
		this.hover = hover;
		this.disabled = disabled;
		this.selected = selected;
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
		boolean highlighted = selected.getAsBoolean() || isHovered();
		GuiSprites.blit(g, !active ? disabled : highlighted ? hover : normal, getX(), getY(), getWidth(), getHeight());
	}

}
