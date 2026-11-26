package dev.xkmc.gensokyolegacy.content.item.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

/**
 * An item held for a while before it hands back what it holds: the stored stack if it has one, otherwise
 * one roll from its loot table. Subclasses supply their own lore.
 */
public abstract class HoldToOpenItem extends Item {

	private final int holdTicks;

	protected HoldToOpenItem(Properties properties, int holdTicks) {
		super(properties);
		this.holdTicks = holdTicks;
	}

	protected ItemStack contentOf(ItemStack stack) {
		return ItemStack.EMPTY;
	}

	/**
	 * The table rolled on opening, or {@code null} for an item that only ever holds a stack.
	 */
	@Nullable
	protected ResourceLocation lootTableOf(ItemStack stack) {
		return null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (contentOf(stack).isEmpty() && lootTableOf(stack) == null) return InteractionResultHolder.pass(stack);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return holdTicks;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BRUSH;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack content = contentOf(stack);
		if (!content.isEmpty()) return content;
		if (!(level instanceof ServerLevel server)) return stack;
		ResourceLocation table = lootTableOf(stack);
		if (table == null) return stack;
		LootTable loot = server.getServer().getLootData().getLootTable(table);
		if (loot == LootTable.EMPTY) return stack;
		var params = new LootParams.Builder(server)
				.withParameter(LootContextParams.ORIGIN, entity.position())
				.withParameter(LootContextParams.THIS_ENTITY, entity)
				.create(LootContextParamSets.GIFT);
		var items = loot.getRandomItems(params, entity.getRandom().nextLong());
		if (items.isEmpty()) return stack;
		ItemStack result = items.get(0);
		if (!(entity instanceof Player player)) return result;
		if (stack.getCount() <= 1) return result;
		ItemStack rest = stack.copy();
		rest.shrink(1);
		if (!player.getInventory().add(result)) player.drop(result, false);
		return rest;
	}

}
