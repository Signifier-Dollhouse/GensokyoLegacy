package dev.xkmc.gensokyolegacy.compat.curios;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanCurioItem;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public class CuriosManager {

	public static boolean hasWings(LivingEntity le, Item item, boolean checkRender) {
		if (le.getItemBySlot(EquipmentSlot.CHEST).is(item))
			return true;
		if (ModList.get().isLoaded("curios")) {
			return CuriosApi.getCuriosInventory(le)
					.flatMap(e -> e.findFirstCurio(item))
					.map(e -> !checkRender || e.slotContext().visible())
					.orElse(false);
		}
		return false;
	}

	public static boolean hasAnyWings(LivingEntity le) {
		if (le.getItemBySlot(EquipmentSlot.CHEST).is(GLTagGen.TOUHOU_WINGS))
			return true;
		if (ModList.get().isLoaded("curios")) {
			return CuriosApi.getCuriosInventory(le)
					.flatMap(e -> e.findFirstCurio(s -> s.is(GLTagGen.TOUHOU_WINGS)))
					.isPresent();
		}
		return false;
	}

	public static List<ItemStack> getEquippedTalismans(LivingEntity le) {
		List<ItemStack> ans = new ArrayList<>();
		if (ModList.get().isLoaded("curios")) {
			CuriosApi.getCuriosInventory(le).ifPresent(inv ->
					inv.getStacksHandler("charm").ifPresent(handler -> {
						var stacks = handler.getStacks();
						for (int i = 0; i < stacks.getSlots(); i++) {
							ItemStack curio = stacks.getStackInSlot(i);
							if (curio.getItem() instanceof TalismanCurioItem) {
								ans.add(curio);
							}
						}
					}));
		}
		return ans;
	}

}