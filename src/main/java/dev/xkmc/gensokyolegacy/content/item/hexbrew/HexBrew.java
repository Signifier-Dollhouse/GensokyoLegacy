package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.gensokyolegacy.content.fluid.GLFluidType;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import dev.xkmc.gensokyolegacy.content.fluid.VirtualFluidBuilder;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLFluids;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.generators.ModelFile;
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
	WITCH_HEXBREW(0xFF7A4BA1, new WitchHandler(false)),
	WITCH_SPLASH(0xFF7A4BA1, new WitchHandler(true));

	public final FluidEntry<GLHexFluid> fluid;
	public final ItemEntry<HexBrewBottleItem> bottle;
	private final HexBrewHandler handler;

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
		bottle = GensokyoLegacy.REGISTRATE.item(id + "_bottle",
						p -> new HexBrewBottleItem(this, fluid::getSource, handler.modify(p)))
				.model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile("item/generated"))
						.texture("layer0", GensokyoLegacy.loc("item/hexbrew/" + id)))
				.tab(GLItems.TAB.key()).defaultLang().register();
	}

	public boolean isThrowable() {
		return handler.isThrowable();
	}

	public boolean isDrinkable() {
		return handler.isDrinkable();
	}

	public void onHit(Level level, Vec3 pos, @Nullable Entity thrower, ItemStack stack) {
		handler.onHit(level, pos, thrower, stack);
	}

	public void onDrink(LivingEntity user, ItemStack stack, Level level) {
		handler.onDrink(user, stack, level);
	}

	public int getUseDuration(ItemStack stack) {
		return handler.getUseDuration(stack);
	}

	public UseAnim getUseAnimation(ItemStack stack) {
		return handler.getUseAnimation(stack);
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

	private static <T> void copyComponent(DataComponentType<T> type, ItemStack from, FluidStack to) {
		T val = from.get(type);
		if (val != null) to.set(type, val);
	}

	private static <T> void copyComponent(DataComponentType<T> type, FluidStack from, ItemStack to) {
		T val = from.get(type);
		if (val != null) to.set(type, val);
	}

	public Fluid getSource() {
		return fluid.getSource();
	}

	public static void register() {

	}

}
