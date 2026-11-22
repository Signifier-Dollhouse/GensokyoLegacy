package dev.xkmc.gensokyolegacy.content.item.treasure;

import dev.xkmc.gensokyolegacy.content.entity.misc.TreasureTowerEntity;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualItem;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Sends the tower out as a {@link TreasureTowerEntity}: it flies to a spot in front of the thrower,
 * fires the beam at the aimed stone, and comes back to the hand. Reusable: the pool and the gems are
 * the price, the tower itself never wears out.
 *
 * @see "doc/design/item/tower.md"
 */
public class TreasureTower extends SpiritualItem {

	public static final int MAX_POWER = 1000;
	public static final int COST = 100;
	public static final int COOLDOWN = 10;
	/**
	 * Target reach: far past the arm's own range.
	 */
	public static final double REACH = 24;

	public TreasureTower(Properties properties) {
		super(properties.stacksTo(1), MAX_POWER);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, context, list, flag);
		list.add(GLLang.ItemTreasureTower.TOOLTIP.get());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide) return InteractionResultHolder.success(stack);
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
		if (SpiritualPower.get(stack) < COST) {
			player.displayClientMessage(GLLang.ItemTreasureTower.NEED_POWER.get(COST), true);
			return InteractionResultHolder.fail(stack);
		}
		if (!(player.pick(REACH, 1.0F, false) instanceof BlockHitResult hit)
				|| hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.fail(stack);

		TreasureTowerData data = TreasureTowerManager.get(level.getServer());
		if (TreasureTowerEntity.pickGem(player, level.getBlockState(hit.getBlockPos()), data).isEmpty())
			return InteractionResultHolder.fail(stack);

		TreasureTowerEntity.launch(level, player, hand, stack.copy(), hit.getBlockPos());
		player.setItemInHand(hand, ItemStack.EMPTY);
		player.getCooldowns().addCooldown(this, COOLDOWN);
		return InteractionResultHolder.success(ItemStack.EMPTY);
	}

}
