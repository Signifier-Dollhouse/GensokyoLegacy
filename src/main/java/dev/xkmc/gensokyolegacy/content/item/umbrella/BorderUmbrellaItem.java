package dev.xkmc.gensokyolegacy.content.item.umbrella;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.util.CreativeModeTabModifier;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.*;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaOpenRenamePacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

public class BorderUmbrellaItem extends Item {

	public BorderUmbrellaItem(Properties props) {
		super(props.stacksTo(1));
	}

	public static BorderSlot getSelectedSlotData(ItemStack stack) {
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		int idx = GLItems.UMBRELLA_SLOT_SELECTED.getOrDefault(stack, 0);
		return slots.get(idx);
	}

	public static void fillCreativeModeTab(DataGenContext<Item, BorderUmbrellaItem> a, CreativeModeTabModifier b) {
		b.accept(a.get());
		var stack = new ItemStack(a.get());
		GLItems.UMBRELLA_UNLOCK.set(stack, new BorderUmbrellaUnlock(true, true));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		b.accept(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		var selected = GLItems.UMBRELLA_SLOT_SELECTED.getOrDefault(stack, 0);
		var slot = getSelectedSlotData(stack);
		var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(stack, BorderUmbrellaUnlock.DEFAULT);
		list.add(GLLang.ITEM$UMBRELLA_MODE.get(mode.displayName()).withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ITEM$UMBRELLA_SLOT.get(String.valueOf(selected)).withStyle(ChatFormatting.GRAY));
		if (!slot.isEmptySlot()) {
			list.add(Component.literal(" -> ").append(slot.displayName()).withStyle(ChatFormatting.YELLOW));
		} else {
			list.add(GLLang.ITEM$UMBRELLA_SLOT_EMPTY.get());
		}
		if (!unlock.travelUnlocked()) {
			list.add(GLLang.ITEM$UMBRELLA_LOCKED_TRAVEL.get());
		}
		if (!unlock.captureUnlocked()) {
			list.add(GLLang.ITEM$UMBRELLA_LOCKED_CAPTURE.get());
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		var stack = context.getItemInHand();
		if (GLItems.UMBRELLA_TYPE.getOrDefault(stack, BorderUmbrellaMode.RECORD) == BorderUmbrellaMode.RECORD) {
			if (context.getPlayer() instanceof ServerPlayer sp) {
				recordPosition(sp, stack, context.getClickedPos().relative(context.getClickedFace()));
				if (!sp.isCreative()) sp.getCooldowns().addCooldown(this, 10);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(stack, BorderUmbrellaUnlock.DEFAULT);
		switch (mode) {
			case RECORD -> {
				if (player instanceof ServerPlayer sp) {
					recordPosition(sp, stack, sp.blockPosition());
					if (!sp.isCreative()) sp.getCooldowns().addCooldown(this, 10);
					return InteractionResultHolder.success(stack);
				}
				return InteractionResultHolder.success(stack);
			}
			case WAYPOINT -> {
				if (player instanceof ServerPlayer sp) {
					teleportToSlot(sp, stack);
					if (!sp.isCreative()) sp.getCooldowns().addCooldown(this, 10);
				}
				return InteractionResultHolder.success(stack);
			}
			case TRAVEL -> {
				if (!unlock.travelUnlocked()) {
					if (!level.isClientSide)
						player.displayClientMessage(GLLang.ITEM$UMBRELLA_LOCKED_TRAVEL.get(), true);
					return InteractionResultHolder.fail(stack);
				}
				// record original position / orientation / target position in data component for orientation lock
				{
					int dist = stack.getOrDefault(GLItems.UMBRELLA_DISTANCE.get(), 1000);
					Vec3 look = player.getLookAngle().normalize();
					Vec3 start = player.position();
					Vec3 tentative = start.add(look.scale(dist));
					BlockPos tpos = BlockPos.containing(tentative);
					var travelData = new BorderUmbrellaTravelData(start, player.getYRot(), player.getXRot(), tpos);
					stack.set(GLItems.UMBRELLA_TRAVEL.get(), travelData);
				}
				// initiate chunk load on use, not in tick, with off-thread flag
				if (player instanceof ServerPlayer sp) {
					var travelData = stack.get(GLItems.UMBRELLA_TRAVEL.get());
					int dist = stack.getOrDefault(GLItems.UMBRELLA_DISTANCE.get(), 1000);
					BlockPos tpos = travelData != null ? travelData.target() : BlockPos.containing(sp.position().add(sp.getLookAngle().normalize().scale(dist)));
					ServerLevel sl = sp.serverLevel();
					ChunkPos cpos = new ChunkPos(tpos);
					sl.getChunkSource().addRegionTicket(TicketType.PORTAL, cpos, 2, tpos);
					sl.getChunkSource().getChunk(cpos.x, cpos.z, ChunkStatus.FULL, false);
					sp.displayClientMessage(GLLang.ITEM$UMBRELLA_TRAVEL_START.get(), true);
				}
				player.startUsingItem(hand);
				return InteractionResultHolder.consume(stack);
			}
			case CAPTURE -> {
				if (!unlock.captureUnlocked()) {
					if (!level.isClientSide)
						player.displayClientMessage(GLLang.ITEM$UMBRELLA_LOCKED_CAPTURE.get(), true);
					return InteractionResultHolder.fail(stack);
				}
				// capture via ray trace? For now, require entity interaction, so fail if no target
				return InteractionResultHolder.pass(stack);
			}
		}
		return InteractionResultHolder.pass(stack);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		if (mode == BorderUmbrellaMode.CAPTURE) {
			var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(stack, BorderUmbrellaUnlock.DEFAULT);
			if (!unlock.captureUnlocked()) {
				if (!player.level().isClientSide)
					player.displayClientMessage(GLLang.ITEM$UMBRELLA_LOCKED_CAPTURE.get(), true);
				return InteractionResult.FAIL;
			}
			if (player instanceof ServerPlayer sp) {
				var slot = getSelectedSlotData(stack);
				if (slot.isEmptySlot()) {
					player.displayClientMessage(GLLang.ITEM$UMBRELLA_SLOT_EMPTY.get(), true);
					return InteractionResult.FAIL;
				}
				if (target instanceof Player || target.isMultipartEntity() ||
						target.getType().is(GLTagGen.UMBRELLA_CAPTURE_BLACKLIST)) {
					sp.displayClientMessage(GLLang.ITEM$UMBRELLA_CAPTURE_FAIL.get(), true);
					return InteractionResult.FAIL;
				}
				teleportEntityToSlot(sp, target, slot, stack);
				if (!player.isCreative()) player.getCooldowns().addCooldown(this, 20);
			}
			return InteractionResult.SUCCESS;
		}
		return super.interactLivingEntity(stack, player, target, hand);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		if (stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD) == BorderUmbrellaMode.TRAVEL)
			return 72000;
		return super.getUseDuration(stack, entity);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		if (stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD) == BorderUmbrellaMode.TRAVEL)
			return UseAnim.BOW;
		return super.getUseAnimation(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
		if (level.isClientSide) {
			if (entity instanceof Player player && stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD) == BorderUmbrellaMode.TRAVEL) {
				var data = stack.get(GLItems.UMBRELLA_TRAVEL.get());
				if (data != null) {
					// make player look at target position instead of just applying stored orientation
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
		if (stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD) != BorderUmbrellaMode.TRAVEL)
			return;
		int used = getUseDuration(stack, entity) - remainingUseDuration;
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
			// still waiting for chunk load, keep holding. Re-request off-thread to keep ticket alive.
			if (remainingUseDuration % 10 == 0)
				sl.getChunkSource().getChunk(cpos.x, cpos.z, ChunkStatus.FULL, false);
			return;
		}
		// chunk loaded and min ticks passed -> teleport to surface that fits player
		Vec3 dst = TravelModeUtil.findSafePosition(sl, tpos);
		TravelModeUtil.teleportPlayer(sp, sl, dst);
		sp.getCooldowns().addCooldown(this, 20);
		sp.displayClientMessage(GLLang.ITEM$UMBRELLA_TRAVEL_DONE.get(), true);
		stack.remove(GLItems.UMBRELLA_TRAVEL.get());
		for (var handStack : List.of(sp.getMainHandItem(), sp.getOffhandItem())) {
			if (handStack.getItem() instanceof BorderUmbrellaItem) {
				handStack.remove(GLItems.UMBRELLA_TRAVEL.get());
			}
		}
		sp.stopUsingItem();
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		// remove travel data on release / cancel
		stack.remove(GLItems.UMBRELLA_TRAVEL.get());
		if (entity instanceof Player player) {
			for (var handStack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
				if (handStack.getItem() instanceof BorderUmbrellaItem) {
					handStack.remove(GLItems.UMBRELLA_TRAVEL.get());
				}
			}
		}
		if (entity instanceof ServerPlayer sp && stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD) == BorderUmbrellaMode.TRAVEL) {
			if (!sp.getCooldowns().isOnCooldown(this)) {
				sp.displayClientMessage(GLLang.ITEM$UMBRELLA_TRAVEL_CANCELLED.get(), true);
			}
		}
		super.releaseUsing(stack, level, entity, timeLeft);
	}

	private void recordPosition(ServerPlayer sp, ItemStack stack, BlockPos pos) {
		int idx = GLItems.UMBRELLA_SLOT_SELECTED.getOrDefault(stack, 0);
		ResourceLocation dim = sp.level().dimension().location();
		BlockPos below = pos.below();
		BlockState belowState = sp.level().getBlockState(below);
		ItemStack icon;
		if (belowState.isAir()) {
			icon = new ItemStack(Items.STONE);
		} else {
			icon = new ItemStack(belowState.getBlock().asItem());
			if (icon.isEmpty()) icon = new ItemStack(Items.STONE);
		}
		String defaultName = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
		BorderSlot slot = new BorderSlot(pos, dim, defaultName, icon);
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		stack.set(GLItems.UMBRELLA_SLOTS.get(), slots.with(idx, slot));
		// send packet to client to open rename editor
		GensokyoLegacy.HANDLER.toClientPlayer(new BorderUmbrellaOpenRenamePacket(idx, defaultName), sp);
		sp.displayClientMessage(GLLang.ITEM$UMBRELLA_RECORDED.get(idx, defaultName), true);
	}

	private void teleportToSlot(ServerPlayer sp, ItemStack stack) {
		var slot = getSelectedSlotData(stack);
		if (slot.isEmptySlot()) {
			sp.displayClientMessage(GLLang.ITEM$UMBRELLA_SLOT_EMPTY.get(), true);
			return;
		}
		ServerLevel targetLevel = sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, slot.dim()));
		if (targetLevel == null) {
			sp.displayClientMessage(GLLang.ITEM$UMBRELLA_DIM_MISSING.get(slot.dim().toString()), true);
			return;
		}
		// teleport directly to slot position without safe check
		Vec3 dst = Vec3.atBottomCenterOf(slot.pos());
		TravelModeUtil.teleportPlayer(sp, targetLevel, dst);
		sp.displayClientMessage(GLLang.ITEM$UMBRELLA_WAYPOINT.get(slot.name()), true);
	}

	private void teleportEntityToSlot(ServerPlayer sp, LivingEntity target, BorderSlot slot, ItemStack stack) {
		ServerLevel targetLevel = sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, slot.dim()));
		if (targetLevel == null) {
			sp.displayClientMessage(GLLang.ITEM$UMBRELLA_DIM_MISSING.get(slot.dim().toString()), true);
			return;
		}
		// teleport directly to slot position without safe check
		Vec3 dst = Vec3.atBottomCenterOf(slot.pos());
		// if entity is in different dimension, need to teleport across dimensions
		if (target.level() != targetLevel) {
			if (target instanceof Entity e) {
				e.teleportTo(targetLevel, dst.x, dst.y, dst.z, Set.of(), target.getYRot(), target.getXRot());
			}
		} else {
			target.teleportTo(dst.x, dst.y, dst.z);
			if (target instanceof ServerPlayer tp) {
				tp.connection.resetPosition();
			}
		}
		sp.displayClientMessage(GLLang.ITEM$UMBRELLA_CAPTURED.get(target.getDisplayName(), slot.name()), true);
	}

	// Called by server when renaming slot
	public static void renameSlot(ItemStack stack, int idx, String name) {
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		var old = slots.get(idx);
		if (old == null || old.isEmptySlot()) return;
		BorderSlot ns = new BorderSlot(old.pos(), old.dim(), name, old.icon());
		stack.set(GLItems.UMBRELLA_SLOTS.get(), slots.with(idx, ns));
	}

	public static void deleteSlot(ItemStack stack, int idx) {
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		stack.set(GLItems.UMBRELLA_SLOTS.get(), slots.with(idx, BorderSlot.empty()));
	}

	public static void swapSlots(ItemStack stack, int a, int b) {
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		var sa = slots.get(a);
		var sb = slots.get(b);
		var ns = slots.with(a, sb).with(b, sa);
		stack.set(GLItems.UMBRELLA_SLOTS.get(), ns);
	}
}
