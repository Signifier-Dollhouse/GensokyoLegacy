package dev.xkmc.gensokyolegacy.content.block.deco.donation;

import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;
import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.BedData;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.content.block.base.IDebugInfoBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.base.LocatedBlockEntity;
import dev.xkmc.gensokyolegacy.content.client.debug.BlockInfoToClient;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.type.BlockMethod;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Donation box bound to a structure. Coins accumulate credit there, and every eight coins buy one
 * effect from the box's data map.
 *
 * @see "doc/design/structure/donation.md"
 */
@SerialClass
public class DonationBoxBlockEntity extends LocatedBlockEntity implements IDebugInfoBlockEntity {

	public static final int CREDIT_COST = 8;

	public static final BlockMethod TE = new BlockEntityBlockMethodImpl<DonationBoxBlockEntity>(GLFurniture.DONATION_BOX_BE, DonationBoxBlockEntity.class) {
		@Override
		public DonationBoxBlockEntity createTileEntity(BlockPos pos, BlockState state) {
			if (state.getValue(DonationBoxBlock.PART) != DonationBoxBlock.Part.MIDDLE) return null;
			return super.createTileEntity(pos, state);
		}
	};

	public DonationBoxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * Coins in one item: a plain coin is one, a coin stack is {@link GLItems#COIN_STACK_VALUE}.
	 */
	public static int coinsOf(ItemStack stack) {
		if (stack.is(GLItems.COIN)) return 1;
		if (stack.is(GLItems.COIN_STACK)) return GLItems.COIN_STACK_VALUE;
		return 0;
	}

	@Override
	public void tick() {
		if (level instanceof ServerLevel sl && key == null && sl.getGameTime() % 20 == 0) {
			tryLink(sl);
		}
	}

	/**
	 * Links to the home here when the block's data map names its structure.
	 */
	public void tryLink(ServerLevel sl) {
		var data = DonationBoxData.of(getBlockState().getBlock());
		if (data == null) return;
		var found = locate(sl, data);
		if (found != null && !found.equals(key)) setLink(found);
	}

	/**
	 * Adds coins to this box's credit and pays out one effect per {@link #CREDIT_COST} reached.
	 * Returns false when the box is not inside the structure its data map names.
	 */
	public boolean insert(ServerPlayer player, int coins) {
		if (coins <= 0 || !(level instanceof ServerLevel sl)) return false;
		var data = DonationBoxData.of(getBlockState().getBlock());
		if (data == null) return false;
		var found = locate(sl, data);
		if (found == null) return false;
		if (!found.equals(key)) setLink(found);

		var attachment = GLMeta.CHAR.type().getOrCreate(player);
		int credit = attachment.addDonation(data.structure(), coins);
		int granted = 0;
		while (credit >= CREDIT_COST) {
			var effect = data.pick(player.getRandom(), e -> !alreadyHas(player, e));
			if (effect == null) break;
			credit -= CREDIT_COST;
			grant(player, effect);
			granted++;
		}
		attachment.setDonation(data.structure(), credit);
		sl.playSound(null, getBlockPos(), granted > 0 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ITEM_PICKUP,
				SoundSource.BLOCKS, 0.5F, granted > 0 ? 1.2F : 0.8F);
		return true;
	}

	@Nullable
	private StructureKey locate(ServerLevel sl, DonationBoxData data) {
		var reg = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);
		for (var structure : sl.structureManager().getAllStructuresAt(getBlockPos()).keySet()) {
			if (!data.structure().equals(reg.getKey(structure))) continue;
			var start = sl.structureManager().getStructureWithPieceAt(getBlockPos(), structure);
			if (!start.isValid() || start.getPieces().isEmpty()) continue;
			var key = ResourceKey.create(Registries.STRUCTURE, data.structure());
			return new StructureKey(key, sl.dimension(), start.getPieces().getFirst().getLocatorPosition());
		}
		return null;
	}

	private static boolean alreadyHas(ServerPlayer player, DonationBoxData.DonationEffect effect) {
		var instance = player.getEffect(effect.effect());
		return instance != null && instance.getAmplifier() >= effect.level() - 1;
	}

	private static void grant(ServerPlayer player, DonationBoxData.DonationEffect effect) {
		for (var instance : List.copyOf(player.getActiveEffects())) {
			if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				player.removeEffect(instance.getEffect());
			}
		}
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
				GLModConfig.SERVER.donationRegenDuration.getAsInt(), 2));
		player.addEffect(new MobEffectInstance(effect.effect(),
				GLModConfig.SERVER.donationEffectDuration.getAsInt(), effect.level() - 1));
	}

	/**
	 * Per-item value in reputation, counted in nuggets: nugget 1, emerald 3, ingot 9, block 81; anything else is worth
	 * 0.
	 */
	public void take(@Nullable Player player, ItemStack stack) {
		if (key == null) return;
		if (player == null) return;
		var bed = BedData.of(getBlockState().getBlock());
		if (bed == null) return;
		var holder = CharDataHolder.getUnbounded(player, bed.type());
		int value = 0;
		if (stack.is(Items.EMERALD)) {
			value = 3;
		} else if (stack.is(Items.GOLD_INGOT)) {
			value = 9;
		} else if (stack.is(Items.GOLD_NUGGET)) {
			value = 1;
		} else if (stack.is(Items.GOLD_BLOCK)) {
			value = 81;
		}
		if (value > 0) {
			int current = holder.data().reputation;
			int max = holder.data().reputationCap;
			if (current < max) {
				// Never take more items than still move the reputation, so the stack does not shrink for nothing.
				int count = Math.min(stack.getCount(), (max - current - 1) / value + 1);
				stack.shrink(count);
				holder.gain(value * count, ReputationConstants.FEED_SOFT_CAP, 0, 0);
			}
		}
	}

	@Override
	public BlockInfoToClient getDebugPacket(ServerPlayer player) {
		var data = DonationBoxData.of(getBlockState().getBlock());
		if (data == null) {
			return BlockInfoToClient.of(GLLang.Info.BED_UNBOUND.get().withStyle(ChatFormatting.RED));
		}
		if (!data.matches(key)) {
			return BlockInfoToClient.of(GLLang.Info.DONATION_WRONG_STRUCTURE.get().withStyle(ChatFormatting.RED));
		}
		int credit = GLMeta.CHAR.type().getOrCreate(player).getDonation(data.structure());
		return BlockInfoToClient.of(GLLang.Info.DONATION_CREDIT.get(credit, CREDIT_COST));
	}

}
