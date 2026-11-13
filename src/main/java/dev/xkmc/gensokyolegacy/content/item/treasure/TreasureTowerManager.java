package dev.xkmc.gensokyolegacy.content.item.treasure;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Derives the ore recipe from the datapack: ores from {@code #c:ores}, gems from what they smelt to,
 * stones from the rule tests of the ore features. An ore no feature places is not a target.
 *
 * @see "doc/design/tower.md"
 */
public final class TreasureTowerManager {

	private static @Nullable TreasureTowerData data;
	private static long buildNanos;

	private TreasureTowerManager() {
	}

	public static void clear() {
		data = null;
	}

	public static TreasureTowerData get(MinecraftServer server) {
		if (data == null) {
			long start = System.nanoTime();
			data = new TreasureTowerData(build(server));
			buildNanos = System.nanoTime() - start;
		}
		return data;
	}

	public static long lastBuildNanos() {
		return buildNanos;
	}

	private static List<OreRecipe> build(MinecraftServer server) {
		Map<Item, Item> smelt = smeltMap(server);
		Map<Block, Item> ores = new LinkedHashMap<>();
		for (var holder : BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.ORES)) {
			Block ore = holder.value();
			Item gem = smelt.get(ore.asItem());
			if (gem != null && gem.getDefaultInstance().is(Tags.Items.GEMS)) ores.put(ore, gem);
		}

		Map<Block, Builder> builders = new LinkedHashMap<>();
		for (var holder : server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).holders().toList()) {
			if (!(holder.value().config() instanceof OreConfiguration cfg)) continue;
			for (var target : cfg.targetStates) {
				Item gem = ores.get(target.state.getBlock());
				if (gem == null) continue;
				builders.computeIfAbsent(target.state.getBlock(), e -> new Builder(gem, e)).add(target.target);
			}
		}

		List<Builder> list = new ArrayList<>(builders.values());
		list.sort(Comparator.comparing(e -> id(e.ore)));

		List<OreRecipe> ans = new ArrayList<>();
		for (var e : list) {
			OreRecipe recipe = e.build();
			if (recipe.tags().isEmpty() && recipe.stones().isEmpty()) continue;
			ans.add(recipe);
		}
		ans.sort(Comparator.comparing((OreRecipe e) -> id(e.gem())).thenComparing(e -> id(e.ore())));
		return ans;
	}

	private static Map<Item, Item> smeltMap(MinecraftServer server) {
		Map<Item, Item> ans = new HashMap<>();
		var registries = server.registryAccess();
		for (var holder : server.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
			var result = holder.value().getResultItem(registries);
			if (result.isEmpty()) continue;
			for (var ingredient : holder.value().getIngredients())
				for (var stack : ingredient.getItems())
					ans.putIfAbsent(stack.getItem(), result.getItem());
		}
		return ans;
	}

	private static String id(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}

	private static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	private static final class Builder {

		private final Item gem;
		private final Block ore;
		private final LinkedHashSet<ResourceLocation> tags = new LinkedHashSet<>();
		private final LinkedHashSet<Block> stones = new LinkedHashSet<>();

		private Builder(Item gem, Block ore) {
			this.gem = gem;
			this.ore = ore;
		}

		private void add(RuleTest rule) {
			if (rule instanceof TagMatchTest tag) {
				tags.add(tag.tag.location());
			} else if (rule instanceof BlockMatchTest block) {
				stones.add(block.block);
			} else if (rule instanceof BlockStateMatchTest state) {
				stones.add(state.blockState.getBlock());
			} else {
				// no field to read: ask the rule which blocks it accepts
				var random = RandomSource.create(0);
				for (var block : BuiltInRegistries.BLOCK)
					if (rule.test(block.defaultBlockState(), random)) stones.add(block);
			}
		}

		private OreRecipe build() {
			ArrayList<ResourceLocation> tags = new ArrayList<>(this.tags);
			tags.sort(Comparator.comparing(ResourceLocation::toString));
			ArrayList<Block> stones = new ArrayList<>(this.stones);
			stones.sort(Comparator.comparing(TreasureTowerManager::id));
			return new OreRecipe(gem, ore, tags, stones);
		}

	}

}
