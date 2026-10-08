package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.gensokyolegacy.content.item.common.InvClickItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The original, leaky Mini Hakkero: it smelts whatever sits next to it in the inventory
 * without ever holding anything itself. The finished {@link Hakkero} is the same idea with
 * the leaks sealed.
 *
 * <p>Its four channels are the four diagonal neighbours of its own slot in the inventory,
 * and each stores the recipe it is cooking by id so it survives the item moving around.
 */
public class HakkeroPrototype extends Item implements InvClickItem {

	public HakkeroPrototype(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		var data = GLItems.DC_HAKKERO_PROTOTYPE.getOrDefault(stack, Data.DEF);
		list.add(GLLang.ItemLores.FURNACE_1_LORE.get());
		list.add(GLLang.ItemFurnace.FURNACE_1_USE.get());
		if (data.state() == HakkeroMode.OFF) list.add(GLLang.ItemFurnace.FURNACE_1_OFF.get());
		else list.add(GLLang.ItemFurnace.FURNACE_1_DESC.get(
				Component.translatable(data.state().block().getDescriptionId()).withStyle(ChatFormatting.WHITE)));
	}

	@Override
	public void handleClick(ServerPlayer sp, PlayerSlot<?> slot) {
		ItemStack stack = slot.getItem(sp);
		var data = GLItems.DC_HAKKERO_PROTOTYPE.getOrDefault(stack, Data.DEF);
		stack.set(GLItems.DC_HAKKERO_PROTOTYPE, data.rotate());
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		if (slotId < 9 || slotId >= 36) return;
		if (!(entity instanceof ServerPlayer sp)) return;
		if (sp.getInventory().getItem(slotId) != stack) return;
		int r = (slotId - 9) / 9;
		int c = slotId % 9;
		test(sp, stack, 0, r, c - 1, r - 1, c - 1);
		test(sp, stack, 1, r - 1, c, r - 1, c + 1);
		test(sp, stack, 2, r, c + 1, r + 1, c + 1);
		test(sp, stack, 3, r + 1, c, r + 1, c - 1);
	}

	private void test(ServerPlayer sp, ItemStack stack, int index, int r0, int c0, int r1, int c1) {
		if (r0 < 0 || r0 >= 3 || c0 < 0 || c0 >= 9 || r1 < 0 || r1 >= 3 || c1 < 0 || c1 >= 9) return;
		int s0 = r0 * 9 + c0 + 9;
		int s1 = r1 * 9 + c1 + 9;
		var data = GLItems.DC_HAKKERO_PROTOTYPE.getOrDefault(stack, Data.DEF);
		if (data.data().length != 4) {
			data = new Data(data.state(), new Entry[4]);
		}
		var entry = data.data()[index];
		if (entry == null && data.state().getType() != null) {
			var rec = data.state().findRecipeHolder(sp.serverLevel(), sp.getInventory().getItem(s0));
			if (rec != null) {
				entry = new Entry(rec.id(), 0, rec.value().getCookingTime() * 2);
			}
		}
		if (entry != null) {
			entry = entry.match(sp, data.state(), s0, s1);
		}
		if (entry != data.data()[index]) {
			stack.set(GLItems.DC_HAKKERO_PROTOTYPE, data.with(index, entry));
		}
	}

	public record Data(HakkeroMode state, Entry[] data) {

		public static final Data DEF = new Data(HakkeroMode.OFF, new Entry[4]);

		public Data with(int index, @Nullable Entry entry) {
			var ans = data.clone();
			ans[index] = entry;
			return new Data(state, ans);
		}

		public Data rotate() {
			return new Data(state.next(), data);
		}
	}

	public record Entry(ResourceLocation recipe, int time, int max) {

		@Nullable
		public Entry match(ServerPlayer sp, HakkeroMode mode, int s0, int s1) {
			if (recipe == null) return null;
			ItemStack in = sp.getInventory().getItem(s0);
			if (in.isEmpty()) return null;
			if (!in.isStackable() && !in.getComponentsPatch().isEmpty()) return null;
			ItemStack out = sp.getInventory().getItem(s1);
			var opt = sp.serverLevel().getServer().getRecipeManager().byKey(recipe);
			if (opt.isEmpty()) return null;
			var rec = opt.get().value();
			if (!(rec instanceof AbstractCookingRecipe r)) return null;
			if (mode.getType() != r.getType()) return null;
			var inv = new SingleRecipeInput(in);
			if (!r.matches(inv, sp.serverLevel())) return null;
			ItemStack res = r.assemble(inv, sp.registryAccess());
			if (!out.isEmpty()) {
				if (!ItemStack.isSameItemSameComponents(res, out)) return null;
				if (res.getCount() + out.getCount() > res.getMaxStackSize()) return null;
			}
			if (time + 1 < max) {
				return new Entry(recipe, time + 1, max);
			}
			if (out.isEmpty()) {
				sp.getInventory().setItem(s1, res);
			} else {
				out.grow(res.getCount());
			}
			in.shrink(1);
			if (in.isEmpty()) return null;
			return new Entry(recipe, 0, max);
		}

	}

}
