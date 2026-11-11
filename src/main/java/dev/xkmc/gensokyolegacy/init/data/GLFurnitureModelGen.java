package dev.xkmc.gensokyolegacy.init.data;

import com.google.common.hash.Hashing;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record GLFurnitureModelGen(PackOutput output, ExistingFileHelper helper) implements DataProvider {

	private static final List<String> MODELS = List.of(
			"deco/flagpole", "deco/flagpole_base",
			"deco/myouren_flag", "furniture/bed", "furniture/bed_foot", "furniture/bed_head",
			"furniture/green_frame_lantern", "utensil/myouren_donation_box");

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		var paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
		for (String model : MODELS) {
			var id = GensokyoLegacy.loc("custom/" + model);
			try (var input = helper.getResource(id, PackType.CLIENT_RESOURCES, ".json", "models").open()) {
				byte[] data = input.readAllBytes();
				cache.writeIfNeeded(paths.json(id), data, Hashing.sha1().hashBytes(data));
			} catch (IOException e) {
				return CompletableFuture.failedFuture(e);
			}
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public String getName() {
		return "Gensokyo Legacy furniture models";
	}
}
