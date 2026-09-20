package dev.xkmc.gensokyolegacy.content.item.glove;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.menu.DollLoadoutProvider;
import dev.xkmc.gensokyolegacy.content.item.glove.client.GloveTargetCache;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveMode;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.init.data.L2Keys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class DollGloveItem extends Item {

	public DollGloveItem(Properties props) {
		super(props.stacksTo(1));
	}

	public static DollGloveMode getMode(ItemStack stack) {
		int i = GLItems.DOLL_GLOVE_MODE.getOrDefault(stack, 0);
		var modes = DollGloveMode.values();
		// floorMod also migrates pre-removal EDITOR gloves (old ordinal 6) to SUMMON
		return modes[Math.floorMod(i, modes.length)];
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		var mode = getMode(stack);
		list.add(GLLang.ItemGlove.MODE.get(mode.displayName()).withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemGlove.WHEEL.get(L2Keys.WHEEL.map.getKey().getDisplayName()).withStyle(ChatFormatting.GRAY));
		list.add(mode.description());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		var stack = context.getItemInHand();
		var player = context.getPlayer();
		if (player == null) return InteractionResult.PASS;
		var result = getMode(stack).handleUse(context.getLevel(), player, context.getHand(), stack, this);
		return result.getResult();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		return getMode(stack).handleUse(level, player, hand, stack, this);
	}

	/**
	 * Right-click on a doll entity at vanilla reach: open its loadout when
	 * owned (or in creative), in every mode. Right-click air or a block runs
	 * the mode action, which first tries the same open through a fresh
	 * 16-block server trace ({@code DollGloveHandler.tryOpenEditor}). Both
	 * paths bypass the glove target cache (glove.md §2b) — the cache only
	 * carries 48-block attack/heal targets.
	 */
	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (target instanceof DollEntity doll && (doll.isOwner(player) || player.getAbilities().instabuild)) {
			if (player instanceof ServerPlayer sp) {
				DollLoadoutProvider.open(sp, doll);
				if (!sp.isCreative()) sp.getCooldowns().addCooldown(this, 10);
			}
			return InteractionResult.sidedSuccess(player.level().isClientSide());
		}
		return InteractionResult.PASS;
	}

	/**
	 * Left-click on an entity in an attack mode: issue the mode command at the
	 * punched entity (never the editor) and suppress the vanilla punch. Other
	 * modes punch normally.
	 */
	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		var mode = getMode(stack);
		if (!mode.isAttackCommand()) return false;
		if (player instanceof ServerPlayer sp && !sp.getCooldowns().isOnCooldown(this)) {
			mode.performAttackOn(sp, entity instanceof LivingEntity living ? living : null,
					InteractionHand.MAIN_HAND, stack, this);
		}
		return true;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide() && entity instanceof Player player) {
			GloveTargetCache.onInventoryTick(player, stack);
		}
	}

}
