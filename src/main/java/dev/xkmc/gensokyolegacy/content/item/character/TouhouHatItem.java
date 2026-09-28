package dev.xkmc.gensokyolegacy.content.item.character;

import com.google.common.base.Suppliers;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.gensokyolegacy.compat.curios.CuriosManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Touhou hats: plain equipable items (not armor) worn in the vanilla head
 * armor slot, plus the curios head slot when curios is present.
 */
public class TouhouHatItem extends Item implements Equipable, ICurioItem {

	private final Supplier<ItemAttributeModifiers> defaultModifiers = Suppliers.memoize(() -> {
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		addModifiers(builder);
		return builder.build();
	});

	public TouhouHatItem(Properties properties) {
		super(properties.stacksTo(1));
		DispenserBlock.registerBehavior(this, ArmorItem.DISPENSE_ITEM_BEHAVIOR);
	}

	protected void addModifiers(ItemAttributeModifiers.Builder builder) {
	}

	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers() {
		return defaultModifiers.get();
	}

	@Override
	public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
		Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
		for (var e : getDefaultAttributeModifiers().modifiers()) {
			if (e.slot() == EquipmentSlotGroup.HEAD) {
				map.put(e.attribute(), e.modifier());
			}
		}
		return map;
	}

	@Override
	public EquipmentSlot getEquipmentSlot() {
		return EquipmentSlot.HEAD;
	}

	@Override
	public EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return EquipmentSlot.HEAD;
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
		if (entity instanceof Player player && slotId == 39) tick(stack, level, player);
	}

	@Override
	public void curioTick(SlotContext slotContext, ItemStack stack) {
		if (slotContext.entity() instanceof Player player) tick(stack, player.level(), player);
	}

	protected void tick(ItemStack stack, Level level, Player player) {
	}

	public boolean support(DyeColor color) {
		return false;
	}

	public DamageSource modifyDamageType(ItemStack stack, LivingEntity le, IDanmakuEntity danmaku, DamageSource type) {
		return type;
	}

	public void onHurtTarget(ItemStack head, DamageSource source, LivingEntity target) {
	}

	/**
	 * All hats worn by the entity: the vanilla head armor slot plus curios head slots.
	 */
	public static List<ItemStack> getEquippedHats(LivingEntity le) {
		List<ItemStack> ans = new ArrayList<>(2);
		ItemStack head = le.getItemBySlot(EquipmentSlot.HEAD);
		if (head.getItem() instanceof TouhouHatItem) ans.add(head);
		ans.addAll(CuriosManager.getCurioHats(le));
		return ans;
	}

}
