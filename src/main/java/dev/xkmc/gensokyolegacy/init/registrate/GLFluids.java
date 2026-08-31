package dev.xkmc.gensokyolegacy.init.registrate;

import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.gensokyolegacy.content.fluid.GLFluidType;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import dev.xkmc.gensokyolegacy.content.fluid.HexbrewBottleItem;
import dev.xkmc.gensokyolegacy.content.fluid.VirtualFluidBuilder;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.minecraft.world.level.material.Fluid;

import java.util.Locale;

public class GLFluids {

	public static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
	public static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

	public enum Hexbrew {
		MUNDANE(0xFF9E9E9E),
		EXPLOSIVE(0xFFE8453C),
		MIASMA(0xFF7A4BA1),
		STARLIGHT(0xFFFFF7AE),
		HYPHAE(0xFFD98E3A);

		public final FluidEntry<GLHexFluid> fluid;
		public final ItemEntry<HexbrewBottleItem> bottle;

		Hexbrew(int color) {
			String id = name().toLowerCase(Locale.ROOT) + "_hexbrew";
			fluid = GensokyoLegacy.REGISTRATE.entry(id, c -> new VirtualFluidBuilder<>(
							GensokyoLegacy.REGISTRATE, GensokyoLegacy.REGISTRATE, id, c,
							WATER_STILL, WATER_FLOW,
							(p, s, f) -> new GLFluidType(p, s, f, color),
							p -> new GLHexFluid(p)))
					.defaultLang().register();
			bottle = GensokyoLegacy.REGISTRATE.item(id + "_bottle", p -> new HexbrewBottleItem(fluid::getSource, p.stacksTo(16)))
					.model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
							.parent(new ModelFile.UncheckedModelFile("item/generated"))
							.texture("layer0", GensokyoLegacy.loc("item/hexbrew/" + id)))
					.tab(GLItems.TAB.key()).defaultLang().register();
		}

		public Fluid getSource(){
			return fluid.getSource();
		}

		public static void register() {
		}
	}

	public static void register() {
		Hexbrew.register();
	}
}
