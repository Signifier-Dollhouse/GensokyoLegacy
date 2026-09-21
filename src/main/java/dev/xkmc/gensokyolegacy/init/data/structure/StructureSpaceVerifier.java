package dev.xkmc.gensokyolegacy.init.data.structure;

import com.mojang.logging.LogUtils;
import dev.xkmc.gensokyolegacy.content.attachment.home.structure.RoomBoxScanner;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only cross-check: re-runs the datagen room scan against the live
 * {@link StructureTemplateManager} (root only for jigsaw) and reports
 * in-room / under-roof / outdoor counts plus the baked room boxes.
 * Game code never scans; it uses the precalculated datagen values.
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class StructureSpaceVerifier {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final List<String> TEMPLATES = List.of(
			"marisa_house",
			"hakurei_shrine/root",
			"morichika_shop");

	private record SpaceReport(String id, int sx, int sy, int sz, RoomBoxScanner.RoomScan scan,
							   List<BlockPos> inRoomSamples, List<BlockPos> underRoofSamples,
							   List<BlockPos> outdoorSamples) {

		String line() {
			var rooms = new StringBuilder();
			for (int i = 0; i < scan.rooms().size(); i++) {
				var box = scan.rooms().get(i).box();
				rooms.append(" room%d=[(%d,%d,%d)-(%d,%d,%d)]".formatted(i,
						box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
			}
			return "%s size=%sx%sx%s occupied=%d inRoom=%d underRoof=%d outdoor=%d rooms=%d%s roofedColumns=%d/%d".formatted(
					id, sx, sy, sz, scan.occupied(), scan.inRoom(), scan.underRoof(), scan.outdoor(),
					scan.rooms().size(), rooms, scan.roofedColumns(), sx * sz);
		}

	}

	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent event) {
		MinecraftServer server = event.getServer();
		Thread thread = new Thread(() -> verify(server), "gensokyolegacy-space");
		thread.setDaemon(true);
		thread.start();
		LOGGER.info("[StructureSpaceVerifier] verification running in background, report will land in server directory");
	}

	public static void verify(MinecraftServer server) {
		StructureTemplateManager manager = server.getStructureManager();
		HolderLookup<Block> blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
		StringBuilder out = new StringBuilder();
		for (String id : TEMPLATES) {
			var template = manager.get(GensokyoLegacy.loc(id));
			if (template.isEmpty()) {
				LOGGER.warn("[StructureSpaceVerifier] template {} not found, skipping", id);
				continue;
			}
			var report = scan(id, template.get(), blocks);
			LOGGER.info("[StructureSpaceVerifier] {}", report.line());
			out.append(report.line()).append('\n');
			out.append("  in-room samples: ").append(report.inRoomSamples()).append('\n');
			out.append("  under-roof samples: ").append(report.underRoofSamples()).append('\n');
			out.append("  outdoor samples: ").append(report.outdoorSamples()).append('\n');
		}
		if (out.isEmpty()) return;
		Path path = server.getServerDirectory().resolve("gensokyolegacy_space.txt");
		try {
			Files.writeString(path, out.toString());
			LOGGER.info("[StructureSpaceVerifier] report written to {}", path);
		} catch (IOException e) {
			LOGGER.warn("[StructureSpaceVerifier] failed to write report", e);
		}
	}

	private static SpaceReport scan(String id, StructureTemplate template, HolderLookup<Block> blocks) {
		CompoundTag tag = template.save(new CompoundTag());
		var result = RoomBoxDatagen.run(tag, blocks);
		var sizeTag = tag.getList("size", Tag.TAG_INT);
		int sx = sizeTag.getInt(0), sy = sizeTag.getInt(1), sz = sizeTag.getInt(2);
		List<BlockPos> inRoomSamples = new ArrayList<>();
		List<BlockPos> underRoofSamples = new ArrayList<>();
		List<BlockPos> outdoorSamples = new ArrayList<>();
		var cursor = new BlockPos.MutableBlockPos();
		for (int x = 0; x < sx; x++)
			for (int y = 0; y < sy; y++)
				for (int z = 0; z < sz; z++) {
					byte kind = result.cells()[x][y][z];
					if (kind == RoomBoxScanner.SOLID) continue;
					cursor.set(x, y, z);
					if (kind == RoomBoxScanner.IN_ROOM) {
						if (inRoomSamples.size() < 8)
							inRoomSamples.add(cursor.immutable());
					} else if (kind == RoomBoxScanner.UNDER_ROOF) {
						if (underRoofSamples.size() < 8)
							underRoofSamples.add(cursor.immutable());
					} else {
						if (outdoorSamples.size() < 8)
							outdoorSamples.add(cursor.immutable());
					}
				}
		return new SpaceReport(id, sx, sy, sz, result,
				List.copyOf(inRoomSamples), List.copyOf(underRoofSamples), List.copyOf(outdoorSamples));
	}

}
