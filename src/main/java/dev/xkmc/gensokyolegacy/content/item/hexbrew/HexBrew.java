package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.gensokyolegacy.content.fluid.GLFluidType;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import dev.xkmc.gensokyolegacy.content.fluid.VirtualFluidBuilder;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLFluids;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.Locale;

public enum HexBrew {
	MUNDANE(0xFF9E9E9E),
	EXPLOSIVE(0xFFE8453C, new ExplosiveHandler()),
	MIASMA(0xFF7A4BA1, new MiasmaHandler()),
	STARLIGHT(0xFFFFF7AE),
	HYPHAE(0xFFD98E3A);

	public final FluidEntry<GLHexFluid> fluid;
	public final ItemEntry<HexBrewBottleItem> bottle;
	private final HexBrewHandler handler;

	HexBrew(int color) {
		this(color, new NoOpHandler());
	}

	HexBrew(int color, HexBrewHandler handler) {
		this.handler = handler;
		String id = name().toLowerCase(Locale.ROOT) + "_hexbrew";
		fluid = GensokyoLegacy.REGISTRATE.entry(id, c -> new VirtualFluidBuilder<>(
						GensokyoLegacy.REGISTRATE, GensokyoLegacy.REGISTRATE, id, c,
						GLFluids.WATER_STILL, GLFluids.WATER_FLOW,
						(p, s, f) -> new GLFluidType(p, s, f, color),
						p -> new GLHexFluid(p, this)))
				.defaultLang().register();
		bottle = GensokyoLegacy.REGISTRATE.item(id + "_bottle", p -> new HexBrewBottleItem(this, fluid::getSource, p.stacksTo(16)))
				.model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile("item/generated"))
						.texture("layer0", GensokyoLegacy.loc("item/hexbrew/" + id)))
				.tab(GLItems.TAB.key()).defaultLang().register();
	}

	public boolean isThrowable() {
		return handler.isThrowable();
	}

	public void onHit(Level level, Vec3 pos, Entity thrower) {
		handler.onHit(level, pos, thrower);
	}

	public Fluid getSource() {
		return fluid.getSource();
	}

	public static void register() {

	}

}
