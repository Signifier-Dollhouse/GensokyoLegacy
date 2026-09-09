package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.gensokyolegacy.content.item.talisman.kinds.*;
import dev.xkmc.gensokyolegacy.content.item.talisman.pocket.TalismanPocketData;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.init.reg.simple.DCReg;
import dev.xkmc.l2core.init.reg.simple.DCVal;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.List;
import java.util.function.Supplier;

public class GLTalismans {

	public static final ItemEntry<HealTalisman> HEAL_TALISMAN;
	public static final ItemEntry<SpeedTalisman> SPEED_TALISMAN;
	public static final ItemEntry<HydrophobicTalisman> HYDROPHOBIC_TALISMAN;
	public static final ItemEntry<LavaAffinityTalisman> LAVA_TALISMAN;
	public static final ItemEntry<ShelterTalisman> SHELTER_TALISMAN;
	public static final ItemEntry<FoldedPaperTalisman> FOLDED_PAPER_TALISMAN;

	private static final DCReg DC = DCReg.of(GensokyoLegacy.REG);

	public static final DCVal<Holder<Item>> DC_TALISMAN_PAPER = DC.registry("talisman_paper", BuiltInRegistries.ITEM);
	public static final DCVal<Integer> DC_TALISMAN_DURABILITY = DC.intVal("talisman_durability");
	public static final DCVal<TalismanPocketData> DC_TALISMAN_POCKET = DC.reg("talisman_pocket", TalismanPocketData.class, false);

	static {
		var reg = GensokyoLegacy.REGISTRATE;

		HEAL_TALISMAN = reg.item("heal_talisman", HealTalisman::new)
				.model((ctx, pvd) -> genLayeredItemModel(ctx.getName(), pvd, "life_talisman_paper"))
				.tab(GLItems.TAB.key())
				.lang("Healing Talisman Paper").register();

		SPEED_TALISMAN = reg.item("speed_talisman", SpeedTalisman::new)
				.model((ctx, pvd) -> genLayeredItemModel(ctx.getName(), pvd, "speed_talisman_paper"))
				.tab(GLItems.TAB.key())
				.lang("Speed Boost Talisman Paper").register();

		HYDROPHOBIC_TALISMAN = reg.item("hydrophobic_talisman", HydrophobicTalisman::new)
				.model((ctx, pvd) -> genLayeredItemModel(ctx.getName(), pvd, "attack_talisman_paper"))
				.tab(GLItems.TAB.key())
				.lang("Hydrophobic Talisman Paper").register();

		LAVA_TALISMAN = reg.item("lava_talisman", LavaAffinityTalisman::new)
				.model((ctx, pvd) -> genLayeredItemModel(ctx.getName(), pvd, "attack_talisman_paper"))
				.tab(GLItems.TAB.key())
				.lang("Lava Affinity Talisman Paper").register();

		SHELTER_TALISMAN = reg.item("shelter_talisman", ShelterTalisman::new)
				.model((ctx, pvd) -> genLayeredItemModel(ctx.getName(), pvd, "life_talisman_paper"))
				.tab(GLItems.TAB.key())
				.lang("Shelter Talisman Paper").register();

		FOLDED_PAPER_TALISMAN = reg.item("folded_paper_talisman", FoldedPaperTalisman::new)
				.model((ctx, pvd) -> genLayeredItemModel(ctx.getName(), pvd, "folded_paper_talisman"))
				.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "charm")))
				.properties(p -> p.stacksTo(1))
				.tab(GLItems.TAB.key())
				.lang("Folded Paper Talisman").register();
	}

	private static void genLayeredItemModel(String name, RegistrateItemModelProvider pvd, String tex) {
		pvd.getBuilder(name)
				.parent(new ModelFile.UncheckedModelFile("item/generated"))
				.texture("layer0", pvd.modLoc("item/talisman/" + tex))
				.texture("layer1", pvd.modLoc("item/talisman/" + tex + "_overlay"));
	}

	public static final List<Supplier<? extends Item>> TALISMAN_PAPERS = List.of(
			HEAL_TALISMAN, SPEED_TALISMAN, HYDROPHOBIC_TALISMAN, LAVA_TALISMAN, SHELTER_TALISMAN);

	public static void register() {
	}

}