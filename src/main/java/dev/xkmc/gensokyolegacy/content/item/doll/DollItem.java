package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.menu.DollItemLoadoutProvider;
import dev.xkmc.gensokyolegacy.content.item.tool.InvClickItem;
import dev.xkmc.gensokyolegacy.content.item.tool.InvTooltip;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
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
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class DollItem extends Item implements InvClickItem {

	/**
	 * The single doll entity kind these items summon. Not stored on the item: every doll item
	 * materializes this same {@code doll} entity type (doc/design/doll/item.md).
	 */
	public static final ResourceLocation TYPE = GensokyoLegacy.loc("doll");

	public DollItem(Properties props) {
		super(props);
	}

	/**
	 * Builds a doll item from a {@link DollData} (the inverse of {@link DollData#fromItemData}):
	 * combat in the mod's {@code DOLL_DATA} component, name/tint in the vanilla
	 * {@code CUSTOM_NAME} / {@code DYED_COLOR} components. {@code null} (and {@link #blank()})
	 * yields a fresh, component-less doll.
	 */
	public static ItemStack makeItem(@Nullable DollData data) {
		if (data == null) return blank();
		ItemStack stack = new ItemStack(GLItems.DOLL.get());
		stack.set(GLItems.DOLL_DATA.get(), new DollItemData(data.combat));
		if (data.customName != null) stack.set(DataComponents.CUSTOM_NAME, data.customName);
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(data.getColor().getTextColor(), false));
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
	 * The doll's tint from the vanilla {@code DYED_COLOR} component; component-missing stacks
	 * (blank, legacy, or hand-made) fall back to red.
	 */
	public static DyeColor colorOf(ItemStack stack) {
		int rgb = DyedItemColor.getOrDefault(stack, DyeColor.RED.getTextColor());
		for (DyeColor d : DyeColor.values()) {
			if (d.getTextColor() == rgb) return d;
		}
		return DyeColor.RED;
	}

	public static int getColor(ItemStack stack, int tintIndex) {
		if (tintIndex != 1) return -1;
		return DyedItemColor.getOrDefault(stack, DyeColor.RED.getTextColor());
	}

	// ---- health as a durability bar ----
	// The fresh doll (no DOLL_DATA) is full health and shows no bar; a damaged doll shows
	// remaining health scaled over the doll entity's default max health.

	@Override
	public boolean isBarVisible(ItemStack stack) {
		DollItemData data = stack.get(GLItems.DOLL_DATA.get());
		return data != null && data.combat().amount() < BaseDollEntity.DEFAULT_MAX_HEALTH;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		DollItemData data = stack.get(GLItems.DOLL_DATA.get());
		if (data == null) return 0;
		return Math.round(13.0F * data.combat().amount() / BaseDollEntity.DEFAULT_MAX_HEALTH);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		DollItemData data = stack.get(GLItems.DOLL_DATA.get());
		float frac = data == null ? 0.5F : Math.min(1.0F, data.combat().amount() / BaseDollEntity.DEFAULT_MAX_HEALTH);
		return Mth.hsvToRgb(frac / 3.0F, 1.0F, 1.0F);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
		if (player instanceof ServerPlayer sp) {
			Vec3 pos = player.position().add(0, 1, 0).add(player.getLookAngle().scale(2.0));
			DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
			if (att.summon(sp, stack, pos)) {
				if (!sp.getAbilities().instabuild) stack.shrink(1);
				return InteractionResultHolder.consume(stack);
			}
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
			if (att.summon(sp, ctx.getItemInHand(), pos)) {
				if (!sp.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
				return InteractionResult.CONSUME;
			}
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