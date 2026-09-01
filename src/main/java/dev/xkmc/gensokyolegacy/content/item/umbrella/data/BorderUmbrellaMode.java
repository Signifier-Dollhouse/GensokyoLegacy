package dev.xkmc.gensokyolegacy.content.item.umbrella.data;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.mode.*;
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

public enum BorderUmbrellaMode {
	RECORD(new RecordMode()),
	WAYPOINT(new WaypointMode()),
	TRAVEL(new TravelMode()),
	CAPTURE(new CaptureMode());

	private final UmbrellaMode handler;

	BorderUmbrellaMode(UmbrellaMode handler) {
		this.handler = handler;
	}

	public ItemStack icon() {
		return handler.icon();
	}

	public Component displayName() {
		return handler.displayName();
	}

	public Component description() {
		return handler.description();
	}

	public boolean isAvailable(ItemStack stack) {
		return handler.isAvailable(stack);
	}

	public boolean isHiddenWhenLocked() {
		return handler.isHiddenWhenLocked();
	}

	public boolean showsSlot() {
		return handler.showsSlot();
	}

	public boolean showsDistance() {
		return handler.showsDistance();
	}

	public InteractionResult handleUseOn(UseOnContext ctx, ItemStack stack, BorderUmbrellaItem item) {
		return handler.handleUseOn(ctx, stack, item);
	}

	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, BorderUmbrellaItem item) {
		return handler.handleUse(level, player, hand, stack, item);
	}

	public InteractionResult handleInteractLiving(ItemStack stack, Player player, LivingEntity target, InteractionHand hand, BorderUmbrellaItem item) {
		return handler.handleInteractLiving(stack, player, target, hand, item);
	}

	public int getUseDuration(ItemStack stack) {
		return handler.getUseDuration(stack);
	}

	public UseAnim getUseAnimation(ItemStack stack) {
		return handler.getUseAnimation(stack);
	}

	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration, BorderUmbrellaItem item) {
		handler.onUseTick(level, entity, stack, remainingUseDuration, item);
	}

	public void onReleaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft, BorderUmbrellaItem item) {
		handler.onReleaseUsing(stack, level, entity, timeLeft, item);
	}
}