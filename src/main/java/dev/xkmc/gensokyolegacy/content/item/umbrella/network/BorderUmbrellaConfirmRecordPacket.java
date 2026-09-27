package dev.xkmc.gensokyolegacy.content.item.umbrella.network;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderSlot;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public record BorderUmbrellaConfirmRecordPacket(int slot, BlockPos pos, ResourceLocation dim,
												String name) implements SerialPacketBase<BorderUmbrellaConfirmRecordPacket> {

	@Override
	public void handle(Player player) {
		if (!(player instanceof ServerPlayer sp)) return;
		ItemStack stack = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		if (stack == null || stack.isEmpty()) return;
		int idx = Math.floorMod(slot, BorderUmbrellaSlots.MAX_SLOTS);
		String nm = name == null ? "" : name;
		if (nm.length() > 32) nm = nm.substring(0, 32);
		if (pos == null || dim == null) return;
		var dimKey = ResourceKey.create(Registries.DIMENSION, dim);
		var level = sp.server.getLevel(dimKey);
		ItemStack icon = new ItemStack(Items.STONE);
		if (level != null) {
			BlockState belowState = level.getBlockState(pos.below());
			if (!belowState.isAir()) {
				var item = belowState.getBlock().asItem();
				if (item != null) {
					var cand = new ItemStack(item);
					if (!cand.isEmpty()) icon = cand;
				}
			}
		}
		String finalName = nm.isBlank() ? pos.getX() + ", " + pos.getY() + ", " + pos.getZ() : nm;
		BorderSlot slotData = new BorderSlot(pos, dim, finalName, icon);
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		stack.set(GLItems.UMBRELLA_SLOTS.get(), slots.with(idx, slotData));
		sp.displayClientMessage(GLLang.ItemUmbrella.RECORDED.get(idx, finalName), true);
	}

}
