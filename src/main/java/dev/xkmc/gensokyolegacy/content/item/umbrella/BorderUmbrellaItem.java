package dev.xkmc.gensokyolegacy.content.item.umbrella;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.util.CreativeModeTabModifier;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderSlot;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaMode;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaUnlock;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.init.data.L2Keys;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

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
		var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(stack, BorderUmbrellaUnlock.DEFAULT);
		list.add(GLLang.ItemUmbrella.MODE.get(mode.displayName()).withStyle(ChatFormatting.GRAY));
		if (mode.showsSlot()) {
			var selected = GLItems.UMBRELLA_SLOT_SELECTED.getOrDefault(stack, 0);
			var slot = getSelectedSlotData(stack);
			var sel = GLLang.ItemUmbrella.SLOT.get(String.valueOf(selected)).withStyle(ChatFormatting.GRAY).append(Component.literal(" -> "));
			if (!slot.isEmptySlot()) {
				list.add(sel.append(slot.displayName().copy().withStyle(ChatFormatting.YELLOW)));
			} else {
				list.add(sel.append(GLLang.ItemUmbrella.SLOT_EMPTY_ITEM.get()));
			}
		} else if (mode.showsDistance()) {
			int dist = stack.getOrDefault(GLItems.UMBRELLA_DISTANCE.get(), 1000);
			list.add(GLLang.ItemUmbrella.DISTANCE.get(String.valueOf(dist)).withStyle(ChatFormatting.GRAY));
		}
		list.add(GLLang.ItemUmbrella.WHEEL.get(L2Keys.WHEEL.map.getKey().getDisplayName()).withStyle(ChatFormatting.GRAY));
		if (unlock.travelUnlocked()) {
			list.add(GLLang.ItemUmbrella.UNLOCKED_TRAVEL.get());
		}
		if (unlock.captureUnlocked()) {
			list.add(GLLang.ItemUmbrella.UNLOCKED_CAPTURE.get());
		}
		list.add(mode.description());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		var stack = context.getItemInHand();
		var mode = GLItems.UMBRELLA_TYPE.getOrDefault(stack, BorderUmbrellaMode.RECORD);
		return mode.handleUseOn(context, stack, this);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		return mode.handleUse(level, player, hand, stack, this);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		var result = mode.handleInteractLiving(stack, player, target, hand, this);
		if (result != InteractionResult.PASS) return result;
		return super.interactLivingEntity(stack, player, target, hand);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		int d = mode.getUseDuration(stack);
		return d != 0 ? d : super.getUseDuration(stack, entity);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		var anim = mode.getUseAnimation(stack);
		return anim != UseAnim.NONE ? anim : super.getUseAnimation(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		mode.onUseTick(level, entity, stack, remainingUseDuration, this);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		mode.onReleaseUsing(stack, level, entity, timeLeft, this);
		super.releaseUsing(stack, level, entity, timeLeft);
	}

}
