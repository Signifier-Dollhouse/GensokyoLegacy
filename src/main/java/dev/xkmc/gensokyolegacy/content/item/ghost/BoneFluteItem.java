package dev.xkmc.gensokyolegacy.content.item.ghost;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPower;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.StrayGhostEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Binds a wild ghost to the player, or releases an owned ghost back into the wild. The two states
 * are different entity types, so the item only ever discards one and spawns the other.
 *
 * @see "doc/design/doll/stray.md"
 */
public class BoneFluteItem extends Item {

	private static final int COOLDOWN = 40;

	public BoneFluteItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		boolean stray = target instanceof StrayGhostEntity;
		boolean owned = target instanceof GhostEntity;
		if (!stray && !owned) return InteractionResult.PASS;
		if (player.level().isClientSide()) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
		if (sp.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
		DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
		if (stray) {
			StrayGhostEntity ghost = (StrayGhostEntity) target;
			if (ghost.getPower() < GhostPower.LEAVE_THRESHOLD) {
				sp.displayClientMessage(GLLang.Ghost.TOO_WEAK.get(GhostPower.LEAVE_THRESHOLD), true);
				return InteractionResult.FAIL;
			}
			if (att.summonCapped()) {
				sp.displayClientMessage(GLLang.Doll.TOO_MANY.get(DollAttachment.MAX_SUMMONED), true);
				return InteractionResult.FAIL;
			}
			if (!att.adopt(sp, ghost)) {
				sp.displayClientMessage(GLLang.Ghost.BIND_FAIL.get(), true);
				return InteractionResult.FAIL;
			}
			play(sp, 1.2F);
		} else {
			GhostEntity ghost = (GhostEntity) target;
			if (!ghost.isOwner(sp)) {
				sp.displayClientMessage(GLLang.Ghost.NOT_YOURS.get(), true);
				return InteractionResult.FAIL;
			}
			if (!att.release(sp, ghost)) {
				sp.displayClientMessage(GLLang.Ghost.RELEASE_FAIL.get(), true);
				return InteractionResult.FAIL;
			}
			play(sp, 0.8F);
		}
		sp.getCooldowns().addCooldown(this, COOLDOWN);
		sp.swing(hand, true);
		return InteractionResult.SUCCESS;
	}

	private static void play(ServerPlayer player, float pitch) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, pitch);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemBoneFlute.BIND.get());
		list.add(GLLang.ItemBoneFlute.RELEASE.get());
	}

}
