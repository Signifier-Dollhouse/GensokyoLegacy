package dev.xkmc.gensokyolegacy.content.entity.module;

import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterData;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles giving gift items to a character. Runs before
 * {@link FeedModule} so gift items take priority over being fed as food.
 */
@SerialClass
public class GiftModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("gift");

	@SerialField
	private int giftCoolDown;

	public GiftModule(YoukaiEntity self) {
		super(ID, self);
	}

	public int getCoolDown() {
		return giftCoolDown;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!self.mayInteract(player)) return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		var giftData = GLMeta.GIFT_DATA.get(player.registryAccess(), stack.getItemHolder());
		if (giftData == null) return InteractionResult.PASS;
		if (giftCoolDown > 0) return InteractionResult.PASS;
		int favor = giftData.getFavor(stack, self);
		if (favor <= 0) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS;
		}
		var data = self.getData(player);
		if (data.isEmpty()) return InteractionResult.PASS;
		data.get().gain(favor, CharacterData.MAX);
		stack.shrink(1);
		giftCoolDown += giftData.cooldown();
		self.setFlag(YoukaiFlags.GIFTED, true);
		self.level().broadcastEntityEvent(self, EntityEvent.IN_LOVE_HEARTS);
		self.playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.2F);
		self.setTalkTo(sp, -1);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void tickServer() {
		if (giftCoolDown > 0) {
			giftCoolDown--;
			if (giftCoolDown == 0)
				self.setFlag(YoukaiFlags.GIFTED, false);
		}
	}

	@Override
	public void tickClient() {
		if (!self.getFlag(YoukaiFlags.GIFTED)) return;
		int chance = self.isInvisible() ? 15 : 2;
		if (self.getRandom().nextInt(chance) != 0) return;
		self.level().addParticle(ParticleTypes.END_ROD,
				self.getRandomX(0.5D), self.getRandomY() + 0.5D, self.getRandomZ(0.5D), 0, 0, 0);
	}

	@Override
	public boolean handleEntityEvent(byte pId) {
		if (pId == EntityEvent.IN_LOVE_HEARTS) {
			for (int i = 0; i < 7; ++i) {
				double d0 = self.getRandom().nextGaussian() * 0.02D;
				double d1 = self.getRandom().nextGaussian() * 0.02D;
				double d2 = self.getRandom().nextGaussian() * 0.02D;
				self.level().addParticle(ParticleTypes.HEART,
						self.getRandomX(1.0D), self.getRandomY() + 0.5D, self.getRandomZ(1.0D), d0, d1, d2);
			}
			return true;
		}
		return false;
	}

}
