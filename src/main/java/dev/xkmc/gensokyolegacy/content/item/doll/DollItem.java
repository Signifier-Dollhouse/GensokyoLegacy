package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.menu.DollItemLoadoutProvider;
import dev.xkmc.gensokyolegacy.content.item.common.InvClickItem;
import dev.xkmc.gensokyolegacy.content.item.common.InvTooltip;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2menustacker.init.L2MenuStacker;
import dev.xkmc.l2menustacker.screen.packets.CacheMouseToClient;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The item form of a doll: while a doll is an item, the item is the sole authoritative copy.
 *
 * @see "doc/design/doll/item.md"
 */
public class DollItem extends Item implements InvClickItem {

	/**
	 * The doll entity kind the plain doll item materializes. A gadget item names its own
	 * ({@link #dollType()}), which is the only way a ledger entry can be a kind other than this.
	 */
	public static final ResourceLocation TYPE = GensokyoLegacy.loc("doll");

	/**
	 * Every doll item by the entity kind it materializes, so itemization finds the right item back (item.md §2).
	 */
	private static final Map<ResourceLocation, DollItem> BY_TYPE = new HashMap<>();

	/**
	 * The entity kind this item summons.
	 */
	private final ResourceLocation type;

	public DollItem(Properties props) {
		this(props, TYPE);
	}

	/**
	 * Every item names its kind here rather than after registration: an entry's {@code .get()} is
	 * unbound until registration ends, and an overridden {@link #dollType()} would read a field the
	 * subclass has not been able to assign yet. Constructing the item is the one moment it owns.
	 */
	protected DollItem(Properties props, ResourceLocation type) {
		super(props);
		this.type = type;
		BY_TYPE.put(type, this);
	}

	public ResourceLocation dollType() {
		return type;
	}

	/**
	 * Builds the item an entry comes back as: the item that declared that entry's entity kind,
	 * falling back to the plain doll for an entry that names no kind. A null entry yields
	 * {@link #blank()}.
	 */
	public static ItemStack makeItem(@Nullable DollData data) {
		DollItem item = data == null ? GLItems.DOLL.get() : BY_TYPE.getOrDefault(data.type, GLItems.DOLL.get());
		return item.makeStack(data);
	}

	/**
	 * This item's own stack from a {@link DollData}, the inverse of {@link DollData#fromItemData}:
	 * combat into {@code DOLL_DATA}, name into {@code CUSTOM_NAME}, tint into {@code DOLL_COLOR},
	 * loadout into {@code DOLL_LOADOUT}. {@code null} (and {@link #blank()}) yields a fresh,
	 * component-less doll.
	 */
	protected ItemStack makeStack(@Nullable DollData data) {
		if (data == null) return new ItemStack(this);
		ItemStack stack = new ItemStack(this);
		stack.set(GLItems.DOLL_DATA.get(), new DollItemData(data.combat));
		if (data.customName != null) stack.set(DataComponents.CUSTOM_NAME, data.customName);
		stack.set(GLItems.DOLL_COLOR.get(), data.getColor());
		if (data.inventory != null && !data.inventory.isEmpty())
			stack.set(GLItems.DOLL_LOADOUT.get(), data.inventory.toInventory());
		return stack;
	}

	/**
	 * A freshly made doll: pristine, carrying no components (and thus no durability bar).
	 */
	public static ItemStack blank() {
		return new ItemStack(GLItems.DOLL.get());
	}

	/**
	 * The doll's tint from the {@code DOLL_COLOR} component; a component-less stack (blank or
	 * hand-made) falls back to red.
	 */
	public static DyeColor colorOf(ItemStack stack) {
		DyeColor color = stack.get(GLItems.DOLL_COLOR.get());
		if (color != null) return color;
		return DyeColor.RED;
	}

	public static int getColor(ItemStack stack, int tintIndex) {
		if (tintIndex != 1) return -1;
		return 0xff000000 | colorOf(stack).getTextColor();
	}

	// A component-less doll is full health and shows no bar; a damaged doll shows remaining health
	// scaled over the doll entity's default max health.

	@Override
	public boolean isBarVisible(ItemStack stack) {
		DollItemData data = stack.get(GLItems.DOLL_DATA.get());
		return data != null && data.combat().amount() < BaseDollEntity.DEFAULT_MAX_HEALTH;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		DollItemData data = stack.get(GLItems.DOLL_DATA.get());
		if (data == null) return 0;
		return Mth.clamp(Math.round(13.0F * data.combat().amount() / BaseDollEntity.DEFAULT_MAX_HEALTH), 0, 13);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		DollItemData data = stack.get(GLItems.DOLL_DATA.get());
		float frac = data == null ? 0.5F : Mth.clamp(data.combat().amount() / BaseDollEntity.DEFAULT_MAX_HEALTH, 0, 1);
		return Mth.hsvToRgb(frac / 3.0F, 1.0F, 1.0F);
	}

	/**
	 * Why an item summon was refused, for feedback. Null means summonable.
	 */
	@Nullable
	private static Component summonBlocker(ServerPlayer sp, ItemStack stack) {
		DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
		DollItemData data = stack.get(GLItems.DOLL_DATA.get());
		if (data != null && data.combat().amount() <= 0)
			return GLLang.Doll.BROKEN.get();
		if (att.summonCapped())
			return GLLang.Doll.TOO_MANY.get(DollAttachment.MAX_SUMMONED);
		return null;
	}

	/**
	 * Consumes the summoned item, mirroring ModularGolems {@code GolemHolder}: shrink, then
	 * force-write the hand slot so the consumption sticks even in creative (a bare shrink alone
	 * does not). Doll items are singletons, so the slot always ends up empty.
	 */
	private static void consumeSummoned(ServerPlayer sp, InteractionHand hand, ItemStack stack) {
		stack.shrink(1);
		sp.setItemInHand(hand, stack);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
		if (player instanceof ServerPlayer sp) {
			Vec3 pos = player.position().add(0, 1, 0).add(player.getLookAngle().scale(2.0));
			DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
			if (att.summon(sp, stack, pos)) {
				consumeSummoned(sp, hand, stack);
				return InteractionResultHolder.consume(stack);
			}
			Component blocker = summonBlocker(sp, stack);
			if (blocker != null) sp.displayClientMessage(blocker, true);
		}
		return InteractionResultHolder.fail(stack);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (ctx.getPlayer() instanceof ServerPlayer sp) {
			BlockPos clicked = ctx.getClickedPos();
			BlockState blockstate = level.getBlockState(clicked);
			BlockPos spawnPos = blockstate.getCollisionShape(level, clicked).isEmpty()
					? clicked : clicked.relative(ctx.getClickedFace());
			Vec3 pos = new Vec3(spawnPos.getX() + 0.5, spawnPos.getY() + 0.05, spawnPos.getZ() + 0.5);
			DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
			ItemStack stack = ctx.getItemInHand();
			if (att.summon(sp, stack, pos)) {
				consumeSummoned(sp, ctx.getHand(), stack);
				return InteractionResult.CONSUME;
			}
			Component blocker = summonBlocker(sp, stack);
			if (blocker != null) sp.displayClientMessage(blocker, true);
		}
		return InteractionResult.FAIL;
	}

	@Override
	public void handleClick(ServerPlayer sp, PlayerSlot<?> slot) {
		L2MenuStacker.PACKET_HANDLER.toClientPlayer(new CacheMouseToClient(), sp);
		DollItemLoadoutProvider.open(sp, slot);
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (Screen.hasShiftDown()) return Optional.empty();
		var data = stack.get(GLItems.DOLL_LOADOUT.get());
		if (data == null || data.isEmpty()) return Optional.empty();
		List<ItemStack> list = List.of(
				data.get(DollSlot.MAIN_HAND), data.get(DollSlot.OFF_HAND),
				data.get(DollSlot.CORE), data.get(DollSlot.CLOTH));
		return Optional.of(new InvTooltip(list, 4, 1));
	}

}