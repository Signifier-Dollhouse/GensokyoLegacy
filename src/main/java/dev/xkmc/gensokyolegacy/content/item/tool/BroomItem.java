package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.gensokyolegacy.content.entity.broom.BroomEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Summons the rideable {@link BroomEntity} on use. The item is never consumed
 * and the stack stays where it is: the entity is the vehicle, and the broom in
 * hand is only the token that keeps it alive — the entity discards itself once
 * neither hand holds one.
 */
public class BroomItem extends Item {

	/** How far ahead of the player the broom is conjured, in blocks. */
	private static final double MOUNT_DISTANCE = 1.2;

	public BroomItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	/**
	 * Right-click to mount, right-click again to get off. Sneaking is read as
	 * "don't use", matching every other interaction item.
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
		if (player.getVehicle() instanceof BroomEntity) {
			if (!level.isClientSide) player.stopRiding();
			return InteractionResultHolder.success(stack);
		}
		if (level.isClientSide) return InteractionResultHolder.success(stack);
		BroomEntity broom = GLEntities.BROOM.get().create(level);
		if (broom == null) return InteractionResultHolder.fail(stack);
		Vec3 ahead = Vec3.directionFromRotation(0, player.getYRot()).scale(MOUNT_DISTANCE);
		broom.moveTo(player.getX() + ahead.x, player.getY() + 0.6, player.getZ() + ahead.z,
				player.getYRot(), player.getXRot());
		level.addFreshEntity(broom);
		return player.startRiding(broom) ? InteractionResultHolder.success(stack)
				: InteractionResultHolder.fail(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemTools.BROOM_LORE.get());
		list.add(GLLang.ItemTools.BROOM_USE.get());
	}

}