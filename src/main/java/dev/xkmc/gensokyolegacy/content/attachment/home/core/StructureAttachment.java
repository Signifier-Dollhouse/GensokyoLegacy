package dev.xkmc.gensokyolegacy.content.attachment.home.core;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import dev.xkmc.gensokyolegacy.content.attachment.home.custom.CustomHomeData;
import dev.xkmc.gensokyolegacy.content.attachment.home.structure.StructureHomeData;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.l2library.capability.entity.GeneralCapabilityTemplate;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.LinkedHashMap;
import java.util.Map;

@SerialClass
public class StructureAttachment extends GeneralCapabilityTemplate<LevelChunk, StructureAttachment> {

	public static final Capability<StructureAttachment> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

	@SerialField
	public final Map<StructureKey, StructureHomeData> data = new LinkedHashMap<>();

	@SerialField
	public final Map<BlockPos, CustomHomeData> custom = new LinkedHashMap<>();

}
