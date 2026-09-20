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

@SerialClass
public class DollData {

	/**
	 * item -> {@link DollData} conversion: reads the {@code DOLL_DATA}/{@code DOLL_COLOR}/
	 * {@code CUSTOM_NAME} components off the stack (a component-less stack is treated as a fresh
	 * doll, {@link DollItemData#fresh()}). The doll type is fixed ({@link DollItem#TYPE}),
	 * position/dimension/facing are supplied at the summon/deploy site, and state is left to the
	 * caller — SUMMONED for a player summon, STORED for a controller install.
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
	 * Server-only ledger entry. Nothing here is synced via the capability:
	 * the sidebar roster rides {@code DollRosterToClient} (entity ids) and all
	 * per-doll display state (tint, health, validity, action status) rides
	 * synced entity data on the doll entity. Every field is
	 * {@code toClient = false} so capability sync stays empty.
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
	 * Doll tint (west-lead dye). Null on entries predating color tracking is treated as
	 * {@link DyeColor#RED} everywhere it is read.
	 */
	@SerialField(toClient = false)
	public DyeColor color = DyeColor.RED;

	@SerialField(toClient = false)
	public long lastUpdate;

	/**
	 * Authoritative loadout, live and mutable. Survives park/restore/logout; mirrored
	 * to clients via synced entity data on the doll entity. Carried on the doll item
	 * in its own {@code DOLL_LOADOUT} component (never inside {@code DollItemData}),
	 * converting to/from the immutable {@link DollInventory} at the boundary.
	 */
	@SerialField(toClient = false)
	public MutableDollInventory inventory = new MutableDollInventory();

	/**
	 * Follow-formation slot cache. Server-only and never serialized (no
	 * {@code SerialField}): restamped over the live summoned entries in ledger
	 * order every tick by the roster rebuild, read by the follow goal. Stale
	 * by at most a tick; entries without a live entity don't move anyway.
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
	 * Red is the fallback for entries that predate color tracking or deserialized without the
	 * field (matches {@link dev.xkmc.gensokyolegacy.content.item.doll.DollItem#colorOf(ItemStack)}).
	 */
	public DyeColor getColor() {
		return color == null ? DyeColor.RED : color;
	}

}