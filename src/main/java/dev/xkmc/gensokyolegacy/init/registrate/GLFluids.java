package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import net.minecraft.resources.ResourceLocation;

public class GLFluids {

	public static final ResourceLocation WATER_STILL = new ResourceLocation("minecraft", "block/water_still");
	public static final ResourceLocation WATER_FLOW = new ResourceLocation("minecraft", "block/water_flow");

	public static void register() {
		HexBrew.register();
	}
}
