package dev.xkmc.gensokyolegacy.content.item.talisman.pocket;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.FoldedPaperTalisman;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanCurioItem;
import dev.xkmc.gensokyolegacy.content.ui.talisman.TalismanPocketProvider;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.List;

public class TalismanPocket extends TalismanCurioItem {

	public TalismanPocket(Properties p) {
		super(p);
	}

	@Override
	public List<ItemStack> getActiveTalismans(ItemStack stack) {
		var data = GLTalismans.DC_TALISMAN_POCKET.get(stack);
		if (data == null) return List.of();
		List<ItemStack> ans = new ArrayList<>();
		for (TalismanSlot slot : data.slots()) {
			if (!slot.foldedStack().isEmpty()) ans.add(slot.foldedStack());
		}
		return ans;
	}

	@Override
	public void curioTick(SlotContext slotContext, ItemStack stack) {
		super.curioTick(slotContext, stack);
		if (slotContext.entity() instanceof ServerPlayer) {
			refill(stack);
		}
	}

	private static void refill(ItemStack stack) {
		var data = GLTalismans.DC_TALISMAN_POCKET.get(stack);
		if (data == null) return;
		TalismanPocketData next = data;
		boolean changed = false;
		for (int i = 0; i < data.slots().length; i++) {
			TalismanSlot slot = data.get(i);
			if (!slot.foldedStack().isEmpty() || slot.paperStack().isEmpty()) continue;
			ItemStack one = slot.paperStack().copy();
			one.setCount(1);
			ItemStack rest = slot.paperStack().copy();
			rest.shrink(1);
			next = next.with(i, new TalismanSlot(FoldedPaperTalisman.fold(one), rest.isEmpty() ? ItemStack.EMPTY : rest));
			changed = true;
		}
		if (changed) {
			GLTalismans.DC_TALISMAN_POCKET.set(stack, next);
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer sp) {
			TalismanPocketProvider.open(sp, hand);
		}
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.Talisman.POCKET_DESC.get());
	}

}