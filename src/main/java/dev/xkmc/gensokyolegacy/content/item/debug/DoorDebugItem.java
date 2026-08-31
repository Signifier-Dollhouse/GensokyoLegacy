package dev.xkmc.gensokyolegacy.content.item.debug;

import dev.xkmc.gensokyolegacy.content.client.debug.CharacterInfoClientManager;
import dev.xkmc.gensokyolegacy.content.client.debug.IDebugOverlayWand;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class DoorDebugItem extends Item implements IDebugOverlayWand {

	private static final int SEARCH_RADIUS = 16;

	public DoorDebugItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			YoukaiEntity youkai = level.getEntitiesOfClass(YoukaiEntity.class,
							player.getBoundingBox().inflate(SEARCH_RADIUS), e -> e.isAlive())
					.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
			if (youkai == null) {
				player.displayClientMessage(GLLang.ItemDebug.DOOR_DEBUG_NO_YOUKAI.get().withStyle(ChatFormatting.RED), true);
				return InteractionResultHolder.success(stack);
			}
			stack.set(GLItems.DC_DEBUG_YOUKAI, youkai.getUUID());
			player.displayClientMessage(GLLang.ItemDebug.DOOR_DEBUG_BOUND.get(youkai.getName().getString())
					.withStyle(ChatFormatting.GREEN), true);
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
			ItemStack stack = context.getItemInHand();
			UUID id = stack.get(GLItems.DC_DEBUG_YOUKAI);
			if (id == null) {
				sp.displayClientMessage(GLLang.ItemDebug.DOOR_DEBUG_UNBOUND.get().withStyle(ChatFormatting.RED), true);
				return InteractionResult.SUCCESS;
			}
			Entity e = sl.getEntity(id);
			if (e instanceof SmartYoukaiEntity youkai) {
				BrainUtils.clearMemory(youkai, GLBrains.MEM_PATH.get());
				BrainUtils.clearMemory(youkai, MemoryModuleType.PATH);
				BrainUtils.clearMemory(youkai, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
				BrainUtils.setMemory(youkai, MemoryModuleType.WALK_TARGET,
						new WalkTarget(context.getClickedPos(), 1, 1));
				sp.displayClientMessage(GLLang.ItemDebug.DOOR_DEBUG_MOVING.get(
						context.getClickedPos().getX(), context.getClickedPos().getY(),
						context.getClickedPos().getZ()).withStyle(ChatFormatting.GREEN), true);
			} else {
				sp.displayClientMessage(GLLang.ItemDebug.DOOR_DEBUG_MISSING.get().withStyle(ChatFormatting.RED), true);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void addTooltip(Player player, ItemStack stack, List<Component> lines, long gameTime) {
		UUID id = stack.get(GLItems.DC_DEBUG_YOUKAI);
		if (id == null) {
			lines.add(GLLang.ItemDebug.DOOR_DEBUG_UNBOUND.get().withStyle(ChatFormatting.GRAY));
		} else {
			Entity e = player.level().getEntitiesOfClass(YoukaiEntity.class,
							player.getBoundingBox().inflate(64), e2 -> e2.getUUID().equals(id))
					.stream().findFirst().orElse(null);
			if (e instanceof YoukaiEntity youkai) {
				CharacterInfoClientManager.doorTooltip(lines, gameTime, youkai);
			} else {
				lines.add(GLLang.ItemDebug.DOOR_DEBUG_MISSING.get().withStyle(ChatFormatting.GRAY));
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemDebug.DOOR_DEBUG_USE.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemDebug.DOOR_DEBUG_CLICK.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemDebug.DOOR_DEBUG_OVERLAY.get().withStyle(ChatFormatting.GRAY));
	}

}
