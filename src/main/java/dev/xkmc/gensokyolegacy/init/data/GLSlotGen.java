package dev.xkmc.gensokyolegacy.init.data;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import top.theillusivec4.curios.api.CuriosDataProvider;

import java.util.concurrent.CompletableFuture;

public class GLSlotGen extends CuriosDataProvider {

	public GLSlotGen(PackOutput output, ExistingFileHelper fileHelper, CompletableFuture<HolderLookup.Provider> registries) {
		super(GensokyoLegacy.MODID, output, fileHelper, registries);
	}

	@Override
	public void generate(HolderLookup.Provider provider, ExistingFileHelper existingFileHelper) {
		createEntities("player").addEntities(EntityType.PLAYER).addSlots("back", "charm");
	}

}
