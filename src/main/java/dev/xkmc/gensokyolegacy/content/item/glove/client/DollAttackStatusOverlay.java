package dev.xkmc.gensokyolegacy.content.item.glove.client;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionStatus;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity.CombatData;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItemData;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveMode;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2itemselector.overlay.OverlayUtil;
import dev.xkmc.l2itemselector.overlay.SelectionSideBar;
import dev.xkmc.l2itemselector.overlay.SideBar;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Doll roster (glove.md §2c): while holding the glove in any mode with no
 * screen open, the right side lists every summoned doll in ledger order as
 * its doll item icon. In task modes (volley, super, suicide, heal-mark) the
 * icon carries a status frame — hidden for invalid (no valid weapon for this
 * task) or untracked dolls, white idle, yellow preparing, red attacking, green
 * done, light blue auto, purple when the doll is busy with a task foreign to
 * the held mode — and the stack it would use for that task renders in the slot
 * to its left.
 * Summon / stop show no frames. In every mode, the doll under the crosshair
 * is framed orange instead.
 *
 * <p>Entity-only reads: the roster (entity ids) arrives via
 * {@code DollRosterToClient} into the client {@code DollAttachment} instance; validity and
 * status are server-computed synced entity data. No capability access.
 */
public class DollAttackStatusOverlay extends SelectionSideBar<DollAttackStatusOverlay.Entry, DollAttackStatusOverlay.AttackSig> {

	/** Frame for a doll busy with a task foreign to the held glove mode (ARGB). */
	private static final int PURPLE_FRAME = 0xFFAA00AA;

	/** Frame for the doll under the crosshair, in every mode (ARGB). */
	private static final int ORANGE_FRAME = 0xFFFFAA00;

	/** No-frame marker: task-less modes and non-hovered rows. */
	private static final int NO_FRAME = 0;

	public record Entry(ItemStack icon, boolean valid, int frameColor, ItemStack use) {
	}

	private record Resolved(int id, ItemStack icon, boolean valid, DollActionStatus status, int frameColor, ItemStack use) {
	}

	public record EntrySig(int id, boolean valid, DollActionStatus status, int frameColor, boolean useEmpty) {
	}

	public record AttackSig(List<EntrySig> entries) implements Signature<AttackSig> {

		@Override
		public boolean shouldRefreshIdle(SideBar<?> sideBar, @Nullable AttackSig old) {
			return !this.equals(old);
		}

	}

	public DollAttackStatusOverlay() {
		super(36000, 10);
	}

	@Override
	public Pair<List<Entry>, Integer> getItems() {
		List<Entry> out = new ArrayList<>();
		for (Resolved r : collect()) {
			out.add(new Entry(r.icon(), r.valid(), r.frameColor(), r.use()));
		}
		return Pair.of(out, -1);
	}

	@Override
	public boolean isAvailable(Entry entry) {
		return entry.valid();
	}

	@Override
	public boolean onCenter() {
		return false;
	}

	@Override
	protected void renderEntry(Context ctx, Entry entry, int index, int select) {
		int y = 18 * index + ctx.y0();
		ctx.renderItem(entry.use(), ctx.x0(), y);
		if (entry.frameColor() != NO_FRAME &&
				(entry.valid() || entry.frameColor() == ORANGE_FRAME)) {
			OverlayUtil.drawRect(ctx.g(), ctx.x0() + 18, y, 16, 16, entry.frameColor());
		}
		ctx.renderItem(entry.icon(), ctx.x0() + 18, y);
	}

	@Override
	public AttackSig getSignature() {
		List<EntrySig> out = new ArrayList<>();
		for (Resolved r : collect()) {
			out.add(new EntrySig(r.id(), r.valid(), r.status(), r.frameColor(), r.use().isEmpty()));
		}
		return new AttackSig(out);
	}

	@Override
	public boolean isScreenOn() {
		if (Minecraft.getInstance().screen != null) return false;
		if (Minecraft.getInstance().player == null) return false;
		return !collect().isEmpty();
	}

	@Override
	protected int getXOffset(int width) {
		float progress = (max_ease - ease_time) / max_ease;
		return Math.round(progress * width / 2 + width - 42);
	}

	@Override
	protected int getYOffset(int height) {
		return Math.max(0, (height - collect().size() * 18) / 2);
	}

	private static List<Resolved> collect() {
		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return List.of();
		DollGloveMode mode = heldGloveMode(mc.player);
		if (mode == null) return List.of();
		@Nullable DollActionType type = typeForGlove(mode);
		Level level = mc.level;
		List<Resolved> out = new ArrayList<>();
		for (var entry : GLMeta.DOLL.type().getOrCreate(mc.player).getRoster()) {
			if (!(level.getEntity(entry.id()) instanceof DollEntity doll)) continue;
			if (!doll.getUUID().equals(entry.uuid())) continue;
			DollActionStatus status = doll.getActionStatus();
			if (type != null && status == DollActionStatus.DONE && doll.getActionStatusType() != type.ordinal())
				status = DollActionStatus.IDLE;
			out.add(new Resolved(entry.id(), makeIcon(doll), type == null || doll.isValidFor(type),
					status, frameFor(mode, type, doll, status), useFor(type, doll)));
		}
		return out;
	}

	/**
	 * Frame color for a row: the doll under the crosshair draws orange in
	 * every mode; otherwise task modes use status colors with purple for a
	 * task foreign to the held mode, and summon/stop draw none.
	 */
	private static int frameFor(DollGloveMode mode, @Nullable DollActionType type,
								DollEntity doll, DollActionStatus status) {
		if (isHovered(doll)) return ORANGE_FRAME;
		return switch (mode) {
			case SUMMON, STOP -> NO_FRAME;
			default -> {
				boolean executing = status == DollActionStatus.PREPARING ||
						status == DollActionStatus.ATTACKING || status == DollActionStatus.AUTO;
				if (executing && doll.getActionStatusType() != type.ordinal()) yield PURPLE_FRAME;
				yield status.frameColor();
			}
		};
	}

	private static boolean isHovered(DollEntity doll) {
		var mc = Minecraft.getInstance();
		return mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == doll;
	}

	/**
	 * The synced loadout stack this doll would use for the held task mode,
	 * main/off-hand priority mirroring the authoritative server pick. Empty
	 * in task-less modes or when the doll carries nothing valid.
	 */
	private static ItemStack useFor(@Nullable DollActionType type, DollEntity doll) {
		return type == null ? ItemStack.EMPTY : findUsing(doll, type);
	}

	private static ItemStack findUsing(DollEntity doll, DollActionType type) {
		return DollBehaviorRegistry.findUsing(
				doll.getLoadoutItem(DollSlot.MAIN_HAND),
				doll.getLoadoutItem(DollSlot.OFF_HAND), type);
	}

	/**
	 * Sidebar icon: doll item tint + health bar only. No loadout component
	 * (the sidebar renders the icon, never the tooltip) and no custom name
	 * (the row shows no text) — both stay server-side.
	 */
	private static ItemStack makeIcon(DollEntity doll) {
		ItemStack stack = new ItemStack(GLItems.DOLL.get());
		CombatData combat = doll.getCombatData();
		if (combat == null) combat = new CombatData(doll.getCombatProgress(), 0);
		stack.set(GLItems.DOLL_DATA.get(), new DollItemData(combat));
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(doll.getColor().getTextColor(), false));
		return stack;
	}

	@Nullable
	private static DollGloveMode heldGloveMode(Player player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof DollGloveItem) return DollGloveItem.getMode(main);
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof DollGloveItem) return DollGloveItem.getMode(off);
		return null;
	}

	/**
	 * The {@link DollActionType} a glove mode commands, or null for task-less
	 * modes (summon, stop) that just manage the roster.
	 */
	@Nullable
	private static DollActionType typeForGlove(DollGloveMode mode) {
		return switch (mode) {
			case VOLLEY -> DollActionType.REGULAR_ATTACK;
			case SUPER -> DollActionType.SUPER_ATTACK;
			case SUICIDE -> DollActionType.SUICIDE_ATTACK;
			case HEAL_MARK -> DollActionType.HEAL;
			default -> null;
		};
	}

}
