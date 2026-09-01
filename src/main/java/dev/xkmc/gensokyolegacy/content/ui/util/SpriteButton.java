package dev.xkmc.gensokyolegacy.content.ui.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SpriteButton extends Button {

	private final ResourceLocation normal, hover, pressed;
	private boolean isDown;

	public SpriteButton(ResourceLocation normal, ResourceLocation hover, ResourceLocation pressed,
	                    int x, int y, int w, int h, OnPress onPress) {
		super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
		this.normal = normal;
		this.hover = hover;
		this.pressed = pressed;
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
		if (!isHoveredOrFocused()) isDown = false;
		ResourceLocation tex = isHoveredOrFocused() ? (isDown ? pressed : hover) : normal;
		g.blitSprite(tex, getX(), getY(), getWidth(), getHeight());
	}

	@Override
	public void onClick(double mx, double my) {
		isDown = true;
		super.onClick(mx, my);
	}

	@Override
	public void onRelease(double mx, double my) {
		isDown = false;
		super.onRelease(mx, my);
	}

}
