package dev.xkmc.gensokyolegacy.content.item.umbrella.mode;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Polymorphic behavior for umbrella modes.
 * Each enum constant holds an instance of this class, eliminating switch/if on BorderUmbrellaMode.
 */
public abstract class UmbrellaMode {

	public abstract ItemStack icon();

	public abstract Component displayName();

	public abstract Component description();

	public abstract boolean isHiddenWhenLocked();

	public boolean isAvailable(ItemStack stack) {
		return true;
	}

	public boolean showsSlot() {
		return false;
	}

	public boolean showsDistance() {
		return false;
	}

	// interaction handling — default no-op
	public InteractionResult handleUseOn(UseOnContext ctx, ItemStack stack, dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem item) {
		return InteractionResult.PASS;
	}

	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem item) {
		return InteractionResultHolder.pass(stack);
	}

	public InteractionResult handleInteractLiving(ItemStack stack, Player player, LivingEntity target, InteractionHand hand, dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem item) {
		return InteractionResult.PASS;
	}

	public int getUseDuration(ItemStack stack) {
		return 0;
	}

	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.NONE;
	}

	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration, dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem item) {
	}

	public void onReleaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft, dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem item) {
		stack.remove(dev.xkmc.gensokyolegacy.init.registrate.GLItems.UMBRELLA_TRAVEL.get());
		if (entity instanceof Player player) {
			for (var handStack : java.util.List.of(player.getMainHandItem(), player.getOffhandItem())) {
				if (handStack.getItem() instanceof dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem) {
					handStack.remove(dev.xkmc.gensokyolegacy.init.registrate.GLItems.UMBRELLA_TRAVEL.get());
				}
			}
		}
	}
}
