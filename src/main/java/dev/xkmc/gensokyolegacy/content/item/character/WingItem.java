package dev.xkmc.gensokyolegacy.content.item.character;

import com.google.common.base.Suppliers;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.function.Supplier;

/**
 * A pair of wings: plain equipables (not armor) worn in the vanilla chest slot, and in the curios back slot when
 * curios is present. The two are alternatives, and a wearer may well carry a pair in each.
 *
 * <p>The attribute plumbing is why this base exists. Vanilla never looks at curios: it reads
 * {@link ItemAttributeModifiers} off the stack by {@link EquipmentSlotGroup}, while curios ignores that component and
 * asks {@link ICurioItem#getAttributeModifiers}. A subclass declares its modifiers once through
 * {@link #addModifiers} and both systems serve them - under separate ids, because {@link AttributeInstance} keys
 * modifiers by id, so a pair in each slot costs twice over rather than one replacing the other.
 */
public abstract class WingItem extends Item implements Equipable, ICurioItem {

	/** Two seconds: long enough not to refresh every tick, short enough to lapse on removal. */
	protected static final int DURATION = 40;

	/**
	 * {@link Inventory#INVENTORY_SIZE} plus {@link EquipmentSlot#CHEST}'s index in the armor list. The list is
	 * built from {@code EquipmentSlot} rather than in slot order, so this is 38 and not the head slot's 39.
	 */
	private static final int CHEST_SLOT = Inventory.INVENTORY_SIZE + EquipmentSlot.CHEST.getIndex();

	private final Supplier<ItemAttributeModifiers> defaultModifiers = Suppliers.memoize(() -> {
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		addModifiers(builder);
		return builder.build();
	});

	public WingItem(Properties properties) {
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
	public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
			SlotContext slotContext, ResourceLocation id, ItemStack stack) {
		Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
		for (var e : getDefaultAttributeModifiers().modifiers()) {
			if (e.slot() != EquipmentSlotGroup.CHEST) continue;
			var mod = e.modifier();
			map.put(e.attribute(), new AttributeModifier(id, mod.amount(), mod.operation()));
		}
		return map;
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

	/**
	 * Both slots feed this and nothing watches for the wings coming off either, so anything applied here has to
	 * run out on its own: refresh on the {@link #DURATION} leash and let it lapse.
	 */
	protected void tick(Player player) {
	}

}