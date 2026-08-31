package dev.xkmc.gensokyolegacy.content.item.umbrella.mode;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.TravelModeUtil;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaTravelData;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaUnlock;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TravelMode extends UmbrellaMode {

	@Override
	public ItemStack icon() {
		return new ItemStack(Items.ENDER_PEARL);
	}

	@Override
	public Component displayName() {
		return GLLang.ItemUmbrella.MODE_TRAVEL.get();
	}

	@Override
	public Component description() {
		return GLLang.ItemUmbrella.DESC_TRAVEL.get();
	}

	@Override
	public boolean isHiddenWhenLocked() {
		return true;
	}

	@Override
	public boolean isAvailable(ItemStack stack) {
		var unlock = GLItems.UMBRELLA_UNLOCK.get(stack);
		if (unlock == null) return false;
		return unlock.travelUnlocked();
	}

	@Override
	public boolean showsDistance() {
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, BorderUmbrellaItem item) {
		var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(stack, BorderUmbrellaUnlock.DEFAULT);
		if (!unlock.travelUnlocked()) {
			if (!level.isClientSide)
				player.displayClientMessage(GLLang.ItemUmbrella.LOCKED_TRAVEL.get(), true);
			return InteractionResultHolder.fail(stack);
		}
		{
			int dist = stack.getOrDefault(GLItems.UMBRELLA_DISTANCE.get(), 1000);
			Vec3 look = player.getLookAngle().normalize();
			Vec3 start = player.position();
			Vec3 tentative = start.add(look.scale(dist));
			BlockPos tpos = BlockPos.containing(tentative);
			var travelData = new BorderUmbrellaTravelData(start, player.getYRot(), player.getXRot(), tpos);
			stack.set(GLItems.UMBRELLA_TRAVEL.get(), travelData);
		}
		if (player instanceof ServerPlayer sp) {
			var travelData = stack.get(GLItems.UMBRELLA_TRAVEL.get());
			int dist = stack.getOrDefault(GLItems.UMBRELLA_DISTANCE.get(), 1000);
			BlockPos tpos = travelData != null ? travelData.target() : BlockPos.containing(sp.position().add(sp.getLookAngle().normalize().scale(dist)));
			ServerLevel sl = sp.serverLevel();
			ChunkPos cpos = new ChunkPos(tpos);
			sl.getChunkSource().addRegionTicket(TicketType.PORTAL, cpos, 2, tpos);
			sl.getChunkSource().getChunk(cpos.x, cpos.z, ChunkStatus.FULL, false);
			sp.displayClientMessage(GLLang.ItemUmbrella.TRAVEL_START.get(), true);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration, BorderUmbrellaItem item) {
		if (level.isClientSide) {
			if (entity instanceof Player player) {
				var data = stack.get(GLItems.UMBRELLA_TRAVEL.get());
				if (data != null) {
					Vec3 eye = player.getEyePosition();
					Vec3 target = Vec3.atCenterOf(data.target());
					Vec3 diff = target.subtract(eye);
					if (diff.lengthSqr() < 9.0) return;
					double dx = diff.x;
					double dy = diff.y;
					double dz = diff.z;
					double horiz = Math.sqrt(dx * dx + dz * dz);
					float yaw = (float) (Mth.atan2(dz, dx) * 180.0F / Math.PI) - 90.0F;
					float pitch = (float) (-(Mth.atan2(dy, horiz) * 180.0F / Math.PI));
					yaw = Mth.wrapDegrees(yaw);
					pitch = Mth.clamp(pitch, -90.0F, 90.0F);
					player.setYRot(yaw);
					player.setXRot(pitch);
					player.yHeadRot = yaw;
					player.yBodyRot = yaw;
				} else {
					player.setYRot(player.yRotO);
					player.setXRot(player.xRotO);
					player.yHeadRot = player.yHeadRotO;
				}
			}
			return;
		}
		if (!(entity instanceof ServerPlayer sp)) return;
		int used = getUseDuration(stack) - remainingUseDuration;
		if (used < TravelModeUtil.TRAVEL_MIN_TICKS) return;
		ServerLevel sl = sp.serverLevel();
		var data = stack.get(GLItems.UMBRELLA_TRAVEL.get());
		BlockPos tpos;
		if (data != null) {
			tpos = data.target();
		} else {
			int dist = stack.getOrDefault(GLItems.UMBRELLA_DISTANCE.get(), 1000);
			Vec3 look = sp.getLookAngle().normalize();
			Vec3 start = sp.position();
			Vec3 tentative = start.add(look.scale(dist));
			tpos = BlockPos.containing(tentative);
		}
		ChunkPos cpos = new ChunkPos(tpos);
		if (!sl.getChunkSource().hasChunk(cpos.x, cpos.z)) {
			if (remainingUseDuration % 10 == 0)
				sl.getChunkSource().getChunk(cpos.x, cpos.z, ChunkStatus.FULL, false);
			return;
		}
		Vec3 dst = TravelModeUtil.findSafePosition(sl, tpos);
		TravelModeUtil.teleportPlayer(sp, sl, dst);
		sp.getCooldowns().addCooldown(item, 20);
		sp.displayClientMessage(GLLang.ItemUmbrella.TRAVEL_DONE.get(), true);
		stack.remove(GLItems.UMBRELLA_TRAVEL.get());
		for (var handStack : List.of(sp.getMainHandItem(), sp.getOffhandItem())) {
			if (handStack.getItem() instanceof BorderUmbrellaItem) {
				handStack.remove(GLItems.UMBRELLA_TRAVEL.get());
			}
		}
		sp.stopUsingItem();
	}

	@Override
	public void onReleaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft, BorderUmbrellaItem item) {
		super.onReleaseUsing(stack, level, entity, timeLeft, item);
		if (entity instanceof ServerPlayer sp) {
			if (!sp.getCooldowns().isOnCooldown(item)) {
				sp.displayClientMessage(GLLang.ItemUmbrella.TRAVEL_CANCELLED.get(), true);
			}
		}
	}
}
