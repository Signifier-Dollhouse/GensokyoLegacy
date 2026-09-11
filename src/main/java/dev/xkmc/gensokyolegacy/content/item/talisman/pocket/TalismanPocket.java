package dev.xkmc.gensokyolegacy.content.item.talisman.pocket;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.FoldedPaperTalisman;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanContext;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanCurioItem;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.content.item.tool.InvClickItem;
import dev.xkmc.gensokyolegacy.content.item.tool.InvTooltip;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TalismanPocket extends TalismanCurioItem implements InvClickItem {

	public TalismanPocket(Properties p) {
		super(p);
	}

	@Nullable
	private static PlayerSlot<?> handSlot(ServerPlayer sp, InteractionHand hand) {
		ItemStack held = sp.getItemInHand(hand);
		if (hand == InteractionHand.OFF_HAND) {
			return sp.getInventory().offhand.getFirst() == held
					? PlayerSlot.ofInventory(sp.getInventory().getContainerSize() - 1) : null;
		}
		for (int i = 0; i < 9; i++) {
			if (sp.getInventory().items.get(i) == held) return PlayerSlot.ofInventory(i);
		}
		return null;
	}

	@Override
	public List<TalismanContext> getActiveTalismans(ServerPlayer player, ItemStack stack) {
		var data = GLTalismans.DC_TALISMAN_POCKET.get(stack);
		if (data == null) return List.of();
		List<TalismanContext> ans = new ArrayList<>();
		for (int i = 0; i < data.slots().length; i++) {
			ItemStack folded = data.get(i).foldedStack();
			if (folded.isEmpty()) continue;
			TalismanPaperItem paper = FoldedPaperTalisman.paper(folded);
			if (paper != null) ans.add(new TalismanContext(player, stack, i, folded, paper));
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
			ItemStack paper = slot.paperStack();
			ItemStack one = paper.copy();
			one.setCount(1);
			paper.shrink(1);
			next = next.with(i, new TalismanSlot(FoldedPaperTalisman.fold(one), paper.isEmpty() ? ItemStack.EMPTY : paper));
			changed = true;
		}
		if (changed) {
			GLTalismans.DC_TALISMAN_POCKET.set(stack, next);
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer sp) {
			PlayerSlot<?> slot = handSlot(sp, hand);
			if (slot != null) TalismanPocketProvider.open(sp, slot);
		}
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
	}

	@Override
	public void handleClick(ServerPlayer sp, PlayerSlot<?> slot) {
		TalismanPocketProvider.open(sp, slot);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.Talisman.POCKET_DESC.get());
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (Screen.hasShiftDown()) return Optional.empty();
		var data = GLTalismans.DC_TALISMAN_POCKET.get(stack);
		if (data == null) return Optional.empty();
		List<ItemStack> list = new ArrayList<>(getInvSize());
		boolean has = false;
		for (int i = 0; i < TalismanPocketData.MAX_SLOTS; i++) {
			var folded = data.get(i).foldedStack();
			list.add(folded);
			has |= !folded.isEmpty();
		}
		for (int i = 0; i < TalismanPocketData.MAX_SLOTS; i++) {
			var paper = data.get(i).paperStack();
			list.add(paper);
			has |= !paper.isEmpty();
		}
		if (!has) return Optional.empty();
		return Optional.of(new InvTooltip(list, TalismanPocketData.MAX_SLOTS, 2));
	}

	private static int getInvSize() {
		return TalismanPocketData.MAX_SLOTS * 2;
	}

}