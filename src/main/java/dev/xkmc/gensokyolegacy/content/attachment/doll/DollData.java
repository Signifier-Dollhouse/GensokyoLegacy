package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity.CombatData;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItem;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItemData;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * One doll's ledger entry: significant values only, never an entity NBT snapshot.
 *
 * @see doc/design/doll/pairing.md
 */
@SerialClass
public class DollData {

	/**
	 * item -> {@link DollData}: reads the {@code DOLL_DATA}/{@code DOLL_COLOR}/{@code CUSTOM_NAME}/
	 * {@code DOLL_LOADOUT} components off the stack; a component-less stack is a fresh doll
	 * ({@link DollItemData#fresh()}). The doll type is fixed ({@link DollItem#TYPE}), while
	 * position, dimension, facing and state are supplied by the caller (item.md §2).
	 */
	public static DollData fromItemData(ItemStack stack, Vec3 pos, ResourceLocation dim, float yRot) {
		DollItemData item = stack.get(GLItems.DOLL_DATA.get());
		if (item == null) item = DollItemData.fresh();
		DollInventory loadout = stack.get(GLItems.DOLL_LOADOUT.get());
		DollData data = new DollData();
		data.type = DollItem.TYPE;
		data.dimension = dim;
		data.position = pos;
		data.yRot = yRot;
		data.combat = item.combat();
		data.customName = stack.get(DataComponents.CUSTOM_NAME);
		data.color = DollItem.colorOf(stack);
		data.inventory = loadout == null ? new MutableDollInventory() : MutableDollInventory.fromInventory(loadout);
		return data;
	}

	/**
	 * Server-only ledger entry. Nothing here syncs through the capability: the sidebar roster rides
	 * {@code DollRosterToClient} and all per-doll display state (tint, health, validity, action
	 * status) rides synced entity data on the doll entity. Every field is {@code toClient = false}.
	 */
	@SerialField(toClient = false)
	public UUID uuid;

	@SerialField(toClient = false)
	public ResourceLocation type;

	@SerialField(toClient = false)
	public DollState state;

	@SerialField(toClient = false)
	public ResourceLocation dimension;

	@SerialField(toClient = false)
	public Vec3 position;

	@SerialField(toClient = false)
	public float yRot;

	@SerialField(toClient = false)
	public CombatData combat = new CombatData(20, 0);

	@SerialField(toClient = false)
	public Component customName;

	/**
	 * Doll tint (west-lead dye). Entries predating color tracking read null and are
	 * treated as {@link DyeColor#RED} everywhere.
	 */
	@SerialField(toClient = false)
	public DyeColor color = DyeColor.RED;

	@SerialField(toClient = false)
	public long lastUpdate;

	/**
	 * Authoritative loadout, live and mutable: survives park/restore/logout and mirrors to clients
	 * through synced entity data. The item carries it in its own {@code DOLL_LOADOUT} component
	 * (loadout.md §2).
	 */
	@SerialField(toClient = false)
	public MutableDollInventory inventory = new MutableDollInventory();

	/**
	 * Follow-formation slot cache, restamped over the live summoned entries in ledger order every
	 * tick by the roster rebuild and read by the follow goal. Server-only and never serialized;
	 * entries without a live entity don't move anyway.
	 */
	public int formationIndex;
	public int formationTotal = 1;

	public boolean isSummoned() {
		return state == DollState.SUMMONED && uuid != null && type != null;
	}

	public float getHealth() {
		return combat.amount();
	}

	/**
	 * Red is the fallback for entries that predate color tracking or deserialized
	 * without the field (matches {@link DollItem#colorOf(ItemStack)}).
	 */
	public DyeColor getColor() {
		return color == null ? DyeColor.RED : color;
	}

}