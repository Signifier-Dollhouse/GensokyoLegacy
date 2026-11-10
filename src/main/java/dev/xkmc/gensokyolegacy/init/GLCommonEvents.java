package dev.xkmc.gensokyolegacy.init;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GLCommonEvents {

	@SubscribeEvent
	public static void attachChunkCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
		GensokyoLegacy.attach(event);
	}

	@SubscribeEvent
	public static void attachLevelCapabilities(AttachCapabilitiesEvent<Level> event) {
		GensokyoLegacy.attach(event);
	}

}
