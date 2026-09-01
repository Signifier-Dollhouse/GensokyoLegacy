package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.gensokyolegacy.content.fluid.GLFluidType;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import dev.xkmc.gensokyolegacy.content.fluid.VirtualFluidBuilder;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLFluids;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum HexBrew {
	MUNDANE_HEXBREW(0xFF9E9E9E),
	EXPLOSIVE_HEXBREW(0xFFE8453C, new ExplosiveHandler()),
	MIASMA_HEXBREW(0xFF7A4BA1, new SimplePotionHandler(true, GLEffects.MIASMA, 1200, 0)),
	STARLIGHT_HEXBREW(0xFFFFF7AE, new StarlightHandler()),
	HYPHAE_HEXBREW(0xFFD98E3A),
	SHIELD_HEXBREW(0xFFFFF7AE, new SimplePotionHandler(false, GLEffects.STARLIGHT_SHIELD, 1200, 0)),
	WITCH_HEXBREW(0xFFFFFFFF, new WitchHandler(false)),
	WITCH_SPLASH(0xFFFFFFFF, new WitchHandler(true));

	public final FluidEntry<GLHexFluid> fluid;
	public final ItemEntry<HexBrewBottleItem> bottle;
	public final HexBrewHandler handler;

	HexBrew(int color) {
		this(color, new NoOpHandler());
	}

	HexBrew(int color, HexBrewHandler handler) {
		this.handler = handler;
		String id = name().toLowerCase(Locale.ROOT);
		fluid = GensokyoLegacy.REGISTRATE.entry(id, c -> new VirtualFluidBuilder<>(
						GensokyoLegacy.REGISTRATE, GensokyoLegacy.REGISTRATE, id, c,
						GLFluids.WATER_STILL, GLFluids.WATER_FLOW,
						(p, s, f) -> new GLFluidType(p, s, f, color),
						p -> new GLHexFluid(p, this)))
				.defaultLang().register();
		var builder = GensokyoLegacy.REGISTRATE.item(id + "_bottle",
						p -> new HexBrewBottleItem(this, fluid::getSource, handler.modify(p)))
				.model((ctx, pvd) -> {
					var b = pvd.getBuilder(ctx.getName())
							.parent(new ModelFile.UncheckedModelFile("item/generated"))
							.texture("layer0", GensokyoLegacy.loc("item/hexbrew/" + id));
					if (handler.potionTexture()) {
						b.texture("layer1", GensokyoLegacy.loc("item/hexbrew/potion_content"));
					}
				});
		handler.build(builder);
		bottle = builder.defaultLang().register();
	}

	public void copyToFluid(ItemStack from, FluidStack to) {
		for (DataComponentType<?> type : handler.getComponentsToCopy()) {
			copyComponent(type, from, to);
		}
	}

	public void copyToItem(FluidStack from, ItemStack to) {
		for (DataComponentType<?> type : handler.getComponentsToCopy()) {
			copyComponent(type, from, to);
		}
	}

	public static <T> void copyComponent(DataComponentType<T> type, MutableDataComponentHolder from, MutableDataComponentHolder to) {
		T val = from.get(type);
		if (val != null) to.set(type, val);
	}

	public Fluid getSource() {
		return fluid.getSource();
	}

	public static void register() {

	}

}
