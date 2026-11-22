package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateDataMapProvider;
import dev.xkmc.gensokyolegacy.content.block.deco.donation.DonationBoxData;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.init.registrate.GLMisc;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import dev.xkmc.l2tabs.init.L2Tabs;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

import java.util.List;

public class GLDataMapGen {

	public static void dataMapGen(RegistrateDataMapProvider pvd) {
		GLStructureGen.dataMap(pvd);
		donation(pvd);
		pvd.builder(L2Tabs.ICON.reg())
				.add(GLMisc.QUEST_TAB.id(), Items.BOOK, false);
		pvd.builder(L2Tabs.ORDER.reg())
				.add(GLMisc.QUEST_TAB.id(), 3750, false);
	}

	private static void donation(RegistrateDataMapProvider pvd) {
		var reg = pvd.builder(GLMeta.DONATION_DATA.reg());
		reg.add(GLFurniture.DONATION_BOX, new DonationBoxData(GLStructureGen.HAKUREI_SHRINE, List.of(
				effect(GLEffects.LOOTING, 1),
				effect(GLEffects.SPIRITUAL, 1),
				effect(MobEffects.DAMAGE_RESISTANCE, 1))), false);
		reg.add(GLFurniture.MYOUREN_DONATION_BOX, new DonationBoxData(GLStructureGen.MYOUREN_TEMPLE, List.of(
				effect(GLEffects.SPIRITUAL, 1),
				effect(GLEffects.LIVELY, 1),
				effect(MobEffects.MOVEMENT_SPEED, 2))), false);
	}

	private static DonationBoxData.DonationEffect effect(Holder<MobEffect> effect, int level) {
		return new DonationBoxData.DonationEffect(effect, level, 1);
	}

}
