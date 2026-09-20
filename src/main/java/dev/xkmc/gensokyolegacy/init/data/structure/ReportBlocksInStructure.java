package dev.xkmc.gensokyolegacy.init.data.structure;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Comparator;

public class ReportBlocksInStructure {

	private static final Logger LOGGER = LogManager.getLogger();

	public static void report() {
		try {
			report("marisa_house.nbt");
			for (var part : new String[]{"root", "road", "gate", "warehouse",
					"path0", "path1", "path2", "path3", "path4", "path5",
					"stone", "tree0", "tree1", "tree2", "tree2_top", "tree3"}) {
				report("hakurei_shrine/" + part + ".nbt");
			}
			report("morichika_shop.nbt");
		} catch (Exception e) {
			LOGGER.throwing(e);
		}
	}

	public static void report(String path) throws IOException {
		var stream = ReportBlocksInStructure.class.getResourceAsStream("/data/gensokyolegacy/structure/" + path);
		if (stream == null) {
			LOGGER.warn("Structure template not on classpath: {}", path);
			return;
		}
		var tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
		Object2IntLinkedOpenHashMap<ResourceLocation> count = new Object2IntLinkedOpenHashMap<>();
		ListTag palette = tag.getList("palette", 10);
		ResourceLocation[] ids = new ResourceLocation[palette.size()];
		for (int i = 0; i < palette.size(); i++) {
			ids[i] = ResourceLocation.parse(palette.getCompound(i).getString("Name"));
		}
		ListTag blocks = tag.getList("blocks", 10);
		for (int i = 0; i < blocks.size(); i++) {
			int state = blocks.getCompound(i).getInt("state");
			count.computeInt(ids[state], (k, c) -> (c == null ? 0 : c) + 1);
		}
		System.out.println("--- Report for Structure <" + path + "> ---");
		count.object2IntEntrySet().stream().sorted(Comparator.comparingInt(e -> -e.getIntValue()))
				.forEach(e -> System.out.println(e.getKey() + " - " + e.getIntValue()));
		System.out.println("---------");
	}

}
