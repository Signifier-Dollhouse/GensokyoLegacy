package dev.xkmc.gensokyolegacy.compat.curios;

import dev.xkmc.gensokyolegacy.content.item.character.TouhouHatItem;
import dev.xkmc.gensokyolegacy.content.item.character.WingItem;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanCurioItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CuriosManager {

	public static boolean hasItem(LivingEntity le, Item item, boolean checkRender) {
		return CuriosApi.getCuriosInventory(le)
				.flatMap(e -> e.findFirstCurio(item))
				.map(e -> !checkRender || e.slotContext().visible())
				.orElse(false);
	}

	/** The pair of wings a wearer has on, whichever of the two slots it is in. Both wing items draw the same
	 * rig, so the draw asks for the stack rather than for a yes/no: which item it is picks the texture.
	 */
	public static Optional<ItemStack> getWings(LivingEntity le, boolean checkRender) {
		ItemStack chest = le.getItemBySlot(EquipmentSlot.CHEST);
		if (chest.getItem() instanceof WingItem) return Optional.of(chest);
		if (ModList.get().isLoaded("curios")) {
			return CuriosApi.getCuriosInventory(le)
					.flatMap(e -> e.findFirstCurio(s -> s.getItem() instanceof WingItem))
					.filter(e -> !checkRender || e.slotContext().visible())
					.map(SlotResult::stack);
		}
		return Optional.empty();
	}

	public static List<ItemStack> getCurioHats(LivingEntity le) {
		List<ItemStack> ans = new ArrayList<>();
		if (ModList.get().isLoaded("curios")) {
			CuriosApi.getCuriosInventory(le).ifPresent(inv ->
					inv.getStacksHandler("head").ifPresent(handler -> {
						var stacks = handler.getStacks();
						for (int i = 0; i < stacks.getSlots(); i++) {
							ItemStack curio = stacks.getStackInSlot(i);
							if (curio.getItem() instanceof TouhouHatItem) {
								ans.add(curio);
							}
						}
					}));
		}
		return ans;
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