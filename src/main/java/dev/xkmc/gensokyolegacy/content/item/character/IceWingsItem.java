package dev.xkmc.gensokyolegacy.content.item.character;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.l2core.base.effects.EffectUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Cirno's wings, given away as an item. Worn in the vanilla chest armor slot or the curios back slot;
 * both paths feed the same {@link #tick}, so the effects do not care which, and nothing watches for the
 * wings coming off either - they refresh on a two second leash and simply run out.
 *
 * <p>{@link GLEffects#BAKA} is Cirno's own: she counts to nine and nothing else, and a player in her
 * wings counts the same way, which is the joke.
 */
public class IceWingsItem extends Item implements Equipable, ICurioItem {

	/** Two seconds: long enough not to refresh every tick, short enough to lapse on removal. */
	private static final int DURATION = 40;

	/**
	 * {@link Inventory#INVENTORY_SIZE} plus {@link EquipmentSlot#CHEST}'s index in the armor list. The list is
	 * built from {@code EquipmentSlot} rather than in slot order, so this is 38 and not the head slot's 39.
	 */
	private static final int CHEST_SLOT = Inventory.INVENTORY_SIZE + EquipmentSlot.CHEST.getIndex();

	public IceWingsItem(Properties properties) {
		super(properties.stacksTo(1));
		DispenserBlock.registerBehavior(this, ArmorItem.DISPENSE_ITEM_BEHAVIOR);
	}

	@Override
	public EquipmentSlot getEquipmentSlot() {
		return EquipmentSlot.CHEST;
	}

	@Override
	public EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return EquipmentSlot.CHEST;
	}

	@Override
	public Holder<SoundEvent> getEquipSound() {
		return SoundEvents.ARMOR_EQUIP_LEATHER;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return swapWithEquipmentSlot(this, level, player, hand);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		if (slotId == CHEST_SLOT && entity instanceof Player player) tick(player);
	}

	@Override
	public void curioTick(SlotContext slotContext, ItemStack stack) {
		if (slotContext.entity() instanceof Player player) tick(player);
	}

	private static void tick(Player player) {
		EffectUtil.refreshEffect(player, new MobEffectInstance(GLEffects.BAKA, DURATION, 0, true, true), player);
		EffectUtil.refreshEffect(player, new MobEffectInstance(GLEffects.FLOATING, DURATION, 1, true, true), player);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.ICE_WINGS_LORE.get());
	}

}