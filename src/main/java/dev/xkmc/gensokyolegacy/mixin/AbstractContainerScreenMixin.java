package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.event.GLClientEventHandlers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	@Inject(method = "renderSlot", at = @At("TAIL"))
	public void gensokyolegacy$renderSlotContent(GuiGraphics g, Slot slot, CallbackInfo ci) {
		GLClientEventHandlers.onSlotRender(g, slot.getItem(), slot);
	}

}
