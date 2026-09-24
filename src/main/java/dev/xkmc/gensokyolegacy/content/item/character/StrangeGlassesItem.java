package dev.xkmc.gensokyolegacy.content.item.character;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Strange glasses: worn in the head armor slot (right-click to equip).
 * While worn, sealing-pot borders render and every entity glows white (client side only).
 */
public class StrangeGlassesItem extends Item {

	public StrangeGlassesItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public @Nullable EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return EquipmentSlot.HEAD;
	}

	public static boolean isWearing(LivingEntity le) {
		if (le.getItemBySlot(EquipmentSlot.HEAD).is(GLItems.STRANGE_GLASSES.get()))
			return true;
		if (ModList.get().isLoaded("curios")) {
			var inv = CuriosApi.getCuriosInventory(le);
			if (inv.isPresent()) {
				var handler = inv.get();
				// dedicated head slot first
				var head = handler.getStacksHandler("head");
				if (head.isPresent() && containsGlasses(head.get().getStacks()))
					return true;
				// any other curios slot also counts as wearing
				if (handler.findFirstCurio(GLItems.STRANGE_GLASSES.get()).isPresent())
					return true;
			}
		}
		return false;
	}

	private static boolean containsGlasses(IItemHandlerModifiable stacks) {
		for (int i = 0; i < stacks.getSlots(); i++) {
			if (stacks.getStackInSlot(i).is(GLItems.STRANGE_GLASSES.get()))
				return true;
		}
		return false;
	}

}
