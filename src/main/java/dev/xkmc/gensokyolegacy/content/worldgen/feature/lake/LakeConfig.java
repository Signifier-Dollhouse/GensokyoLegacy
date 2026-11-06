package dev.xkmc.gensokyolegacy.content.worldgen.feature.lake;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

/**
 * Everything one lake is made of, for both the pond dug into the magical forest's floor and the pool standing in a
 * hell cavern: where it sits ({@link Mode}), how much open air a room has to carry ({@code clearance}), the union
 * of ellipsoids it is carved from ({@link Shape}), the blocks it is laid out of ({@link Material}) and what is
 * planted on its waterline.
 *
 * @see LakeFeature
 * @see LakeMaker
 * @see "doc/design/gensokyo_biomes.md" §2
 */
public record LakeConfig(Mode mode, int clearance, Shape shape, Material material, List<Deco> decorations)
		implements FeatureConfiguration {

	public static final Codec<LakeConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
			Mode.CODEC.fieldOf("mode").forGetter(LakeConfig::mode),
			Codec.intRange(0, 64).optionalFieldOf("clearance", 0).forGetter(LakeConfig::clearance),
			Shape.CODEC.fieldOf("shape").forGetter(LakeConfig::shape),
			Material.CODEC.fieldOf("material").forGetter(LakeConfig::material),
			Deco.CODEC.listOf().optionalFieldOf("decorations", List.of()).forGetter(LakeConfig::decorations)
	).apply(i, LakeConfig::new));

	/**
	 * Where the lake sits, and so what the maker has to satisfy there. {@link #SURFACE} is a pond dug into open
	 * ground: it is given the ground to sit on and has to bring its own rim. {@link #UNDERGROUND} is a pool standing
	 * in a cavern, which has no rim to lay, and is handed the floors of the rooms it can be poured into instead.
	 */
	public enum Mode {

		SURFACE("surface"), UNDERGROUND("underground");

		public static final Codec<Mode> CODEC = Codec.STRING.comapFlatMap(s -> switch (s) {
			case "surface" -> DataResult.success(SURFACE);
			case "underground" -> DataResult.success(UNDERGROUND);
			default -> DataResult.error(() -> "Unknown lake mode: " + s);
		}, Mode::serialized);

		private final String serialized;

		Mode(String serialized) {
			this.serialized = serialized;
		}

		public String serialized() {
			return serialized;
		}

	}

	/**
	 * The ellipsoid union. {@code maxWidth} and {@code maxHeight} are the box it is grown in, {@code minComponent}
	 * and {@code maxComponent} how many blobs are merged into it, {@code componentRadius} how big each one is and
	 * {@code margin} how far the rim grows past the body. {@code depth} is the waterline inside the box: below it
	 * the body is fluid, above it air.
	 * <p>
	 * {@code maxHeight} has to leave room for the water, for the air the maker cuts above it and for the clearance a
	 * cavern lake checks, or the box checks its own clearance against blocks it was never going to look at.
	 * <p>
	 * {@code clearance} is read on the cavern floor, since that is the surface the pool is dug under: a room carries
	 * that many open blocks over its floor, and the air the maker cuts above the waterline counts towards the same
	 * number. A pond is on open ground and leaves it at zero.
	 */
	public record Shape(int depth, int maxWidth, int maxHeight, int minComponent, int maxComponent,
						int componentRadius, int margin) implements FeatureConfiguration {

		public Shape {
			// RandomSource draws the component count from an exclusive bound, so an empty range is a crash
			if (minComponent >= maxComponent)
				throw new IllegalArgumentException("min_component must be below max_component");
		}

		public static final Codec<Shape> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.intRange(1, 32).fieldOf("depth").forGetter(Shape::depth),
				Codec.intRange(8, 64).fieldOf("max_width").forGetter(Shape::maxWidth),
				Codec.intRange(4, 64).fieldOf("max_height").forGetter(Shape::maxHeight),
				Codec.intRange(1, 15).fieldOf("min_component").forGetter(Shape::minComponent),
				Codec.intRange(2, 16).fieldOf("max_component").forGetter(Shape::maxComponent),
				Codec.intRange(1, 32).fieldOf("component_radius").forGetter(Shape::componentRadius),
				Codec.intRange(0, 16).fieldOf("margin").forGetter(Shape::margin)
		).apply(i, Shape::new));

	}

	/**
	 * The blocks a lake is laid out of: {@code fluid} fills it, {@code padding} is the ground it is dug into and the
	 * shore above the waterline, {@code barrier} the wet shore under it, and {@code surface} what caps the padding
	 * where the sky can see it. A cavern pool lays only its bed, which is {@code padding}, and leaves the other two
	 * out.
	 */
	public record Material(BlockState fluid, BlockState barrier, BlockState padding, BlockState surface)
			implements FeatureConfiguration {

		public static final Codec<Material> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockState.CODEC.fieldOf("fluid").forGetter(Material::fluid),
				BlockState.CODEC.optionalFieldOf("barrier", Blocks.AIR.defaultBlockState())
						.forGetter(Material::barrier),
				BlockState.CODEC.fieldOf("padding").forGetter(Material::padding),
				BlockState.CODEC.optionalFieldOf("surface", Blocks.AIR.defaultBlockState())
						.forGetter(Material::surface)
		).apply(i, Material::new));

	}

	/**
	 * One waterline plant: {@code block} at the water surface or shore top, {@code upper} above it for double-height
	 * plants, on soil matching {@code soil}.
	 * <p>
	 * The block is a provider rather than a single state, because a lake may grow a mix: the water lilies are the
	 * small pad and the large one in each of its looks, and one state would be one of them.
	 */
	public record Deco(Target target, BlockStateProvider block, BlockState upper, float chance, TagKey<Block> soil)
			implements FeatureConfiguration {

		public static final Codec<Deco> CODEC = RecordCodecBuilder.create(i -> i.group(
				Target.CODEC.fieldOf("target").forGetter(Deco::target),
				BlockStateProvider.CODEC.fieldOf("block").forGetter(Deco::block),
				BlockState.CODEC.optionalFieldOf("upper", Blocks.AIR.defaultBlockState()).forGetter(Deco::upper),
				Codec.FLOAT.fieldOf("chance").forGetter(Deco::chance),
				TagKey.hashedCodec(Registries.BLOCK).optionalFieldOf("soil", BlockTags.DIRT).forGetter(Deco::soil)
		).apply(i, Deco::new));

	}

	/**
	 * Where a decoration goes: on open water, in 1-deep water, or on the shore next to water. A cavern pool is a
	 * flooded floor rather than a waterline, so it is the only mode with nothing to plant here.
	 */
	public enum Target {

		FLOATING("floating"), SHALLOW("shallow"), SHORE("shore");

		public static final Codec<Target> CODEC = Codec.STRING.comapFlatMap(s -> switch (s) {
			case "floating" -> DataResult.success(FLOATING);
			case "shallow" -> DataResult.success(SHALLOW);
			case "shore" -> DataResult.success(SHORE);
			default -> DataResult.error(() -> "Unknown lake decoration target: " + s);
		}, Target::serialized);

		private final String serialized;

		Target(String serialized) {
			this.serialized = serialized;
		}

		public String serialized() {
			return serialized;
		}

	}

}
