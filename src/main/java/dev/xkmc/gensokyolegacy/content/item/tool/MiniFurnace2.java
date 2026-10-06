package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.gensokyolegacy.content.item.common.InvClickItem;
import dev.xkmc.gensokyolegacy.content.ui.furnace.MiniFurnace2Provider;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2menustacker.init.L2MenuStacker;
import dev.xkmc.l2menustacker.screen.packets.CacheMouseToClient;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The finished Mini Hakkero, an improved copy of {@link MiniFurnace1}. The prototype leaked
 * heat into the surrounding inventory and smelted whatever sat next to it; this one keeps its
 * fire inside, so it is opened as a menu ({@link MiniFurnace2Provider}) and only smelts the
 * items placed in its own slots, ten times faster than the block it imitates.
 */
public class MiniFurnace2 extends Item implements InvClickItem {

	/**
	 * Cooking time divisor. The prototype used {@code cookingTime * 2} ticks for a "slow" smelt;
	 * this one divides the recipe time, which is the 10x speed the finished device is supposed to have.
	 */
	public static final int SPEED = 10;

	/**
	 * The three moods of the finished hakkero. No {@code OFF}: a firebox that is sealed does
	 * not leak, and the menu is only open while the player is using it.
	 */
	public enum Mode {

		SMOKE(Blocks.SMOKER), FURNACE(Blocks.FURNACE), BLAST(Blocks.BLAST_FURNACE);

		private final Block block;

		Mode(Block block) {
			this.block = block;
		}

		public Block block() {
			return block;
		}

		public RecipeType<? extends AbstractCookingRecipe> getType() {
			return switch (this) {
				case SMOKE -> RecipeType.SMOKING;
				case FURNACE -> RecipeType.SMELTING;
				case BLAST -> RecipeType.BLASTING;
			};
		}

		@Nullable
		public RecipeHolder<? extends AbstractCookingRecipe> getRecipe(ServerPlayer sp, SingleRecipeInput inv) {
			return sp.serverLevel().getRecipeManager().getRecipeFor(getType(), inv, sp.level()).orElse(null);
		}

		public Mode next() {
			var vals = values();
			return vals[(ordinal() + 1) % vals.length];
		}

	}

	public MiniFurnace2(Properties properties) {
		super(properties.stacksTo(1));
	}

	public static Mode getMode(ItemStack stack) {
		return GLItems.DC_FURNACE_2.getOrDefault(stack, Mode.FURNACE);
	}

	public static ItemStack setMode(ItemStack stack, Mode mode) {
		stack.set(GLItems.DC_FURNACE_2, mode);
		return stack;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer sp) {
			PlayerSlot<?> slot = heldSlot(sp, sp.getItemInHand(hand));
			if (slot != null) MiniFurnace2Provider.open(sp, slot);
		}
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
	}

	@Override
	public void handleClick(ServerPlayer sp, PlayerSlot<?> slot) {
		L2MenuStacker.PACKET_HANDLER.toClientPlayer(new CacheMouseToClient(), sp);
		MiniFurnace2Provider.open(sp, slot);
	}

	/**
	 * Which inventory slot holds the hakkero, so the open menu can point back at it and know
	 * when the player moved or dropped it. Only the hotbar and the offhand are reachable by hand.
	 */
	@Nullable
	private static PlayerSlot<?> heldSlot(ServerPlayer sp, ItemStack held) {
		if (sp.getInventory().offhand.getFirst() == held)
			return PlayerSlot.ofInventory(sp.getInventory().getContainerSize() - 1);
		for (int i = 0; i < 9; i++) {
			if (sp.getInventory().items.get(i) == held) return PlayerSlot.ofInventory(i);
		}
		return null;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.FURNACE_2_LORE.get());
		list.add(GLLang.ItemFurnace.FURNACE_2_USE.get());
		list.add(GLLang.ItemFurnace.FURNACE_1_DESC.get(
				Component.translatable(getMode(stack).block().getDescriptionId()).withStyle(ChatFormatting.WHITE)));
	}

}
