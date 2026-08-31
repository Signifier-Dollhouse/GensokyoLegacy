package dev.xkmc.gensokyolegacy.content.block.pot.overlay;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyPotBlockEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2itemselector.overlay.OverlayUtil;
import dev.xkmc.l2modularblock.core.DelegateBlockImpl;
import dev.xkmc.l2modularblock.type.SingletonBlockMethod;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AlchemyHintOverlay implements LayeredDraw.Layer {

	private int startTick = 0;
	private BlockPos lastPos = null;

	@Override
	public void render(GuiGraphics g, DeltaTracker delta) {
		int prev = startTick;
		startTick = 0;
		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.screen != null) return;
		if (!(mc.hitResult instanceof BlockHitResult bhit)) return;
		BlockPos pos = bhit.getBlockPos();
		Level level = mc.player.level();
		var be = getHint(level, pos);
		if (be == null) return;
		if (prev == 0 || lastPos == null || !lastPos.equals(pos))
			prev = mc.player.tickCount;
		lastPos = pos;
		startTick = prev;
		int time = mc.player.tickCount - startTick;
		if (time < 15) return;
		List<Ingredient> hints = be.getHints(level, pos);
		if (hints.isEmpty()) return;
		var stacks = compile(hints);
		int total = stacks.size();
		int n = Math.min(total, AlchemyPotBlockEntity.MAX_SLOTS);
		ItemStack[] display = new ItemStack[n];
		for (int i = 0; i < n; i++) {
			var arr = stacks.get(i);
			if (arr.length == 0) {
				display[i] = ItemStack.EMPTY;
				continue;
			}
			display[i] = arr[time / 15 % arr.length];
		}
		int w = g.guiWidth(), h = g.guiHeight();
		new ImageBox(g, (int) (w * 0.7), (int) (h * 0.5), 0).render(display, Math.min(4, n), Math.min(3, (n - 1) / 4 + 1), total - n);
	}

	@Nullable
	private IHintable getHint(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof IHintable be) return be;
		var state = level.getBlockState(pos);
		if (state.getBlock() instanceof IHintable b) return b;
		if (state.getBlock() instanceof DelegateBlockImpl del) {
			var opt = del.getImpl().one(IHintable.class);
			if (opt.isPresent()) return opt.get();
		}
		return null;
	}

	// Exposed for AlchemyPotBlockEntity to implement
	public interface IHintable extends SingletonBlockMethod {
		List<Ingredient> getHints(Level level, BlockPos pos);
	}

	private List<ItemStack[]> compile(List<Ingredient> list) {
		Int2ObjectLinkedOpenHashMap<ItemStack[]> map = new Int2ObjectLinkedOpenHashMap<>();
		for (var ing : list) {
			if (ing.isEmpty()) {
				map.put(1, new ItemStack[0]);
				continue;
			}
			var stacks = ing.getItems();
			int hash = 1;
			for (var s : stacks) {
				int h;
				if (s.isEmpty()) h = 0;
				else {
					h = BuiltInRegistries.ITEM.getId(s.getItem());
					var patch = s.getComponentsPatch();
					if (!patch.isEmpty()) h += patch.hashCode() * 15;
				}
				hash = 31 * hash + h;
			}
			map.put(hash, stacks);
		}
		return new ArrayList<>(map.values());
	}

	public static class ImageBox extends OverlayUtil {
		public ImageBox(GuiGraphics g, int x0, int y0, int maxW) {
			super(g, x0, y0, maxW);
		}

		public void render(ItemStack[] stacks, int w, int h, int extra) {
			List<ClientTooltipComponent> tooltip = new ArrayList<>();
			tooltip.add(new ClientTextTooltip(GLLang.Alchemy.ALLOW.get().getVisualOrderText()));
			tooltip.add(new TileClientTooltip(List.of(stacks), List.of(), w, h));
			if (extra > 0) {
				tooltip.add(new ClientTextTooltip(GLLang.Alchemy.EXTRA.get(extra).getVisualOrderText()));
			}
			renderTooltipInternal(Minecraft.getInstance().font, tooltip);
		}
	}
}
