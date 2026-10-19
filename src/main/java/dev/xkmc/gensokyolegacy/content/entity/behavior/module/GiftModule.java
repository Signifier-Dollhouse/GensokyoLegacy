package dev.xkmc.gensokyolegacy.content.entity.behavior.module;

import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterData;
import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiFlags;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftItemData;
import dev.xkmc.gensokyolegacy.content.rpg.core.GiftDialogData;
import dev.xkmc.gensokyolegacy.content.rpg.handle.DirectDialogHandle;
import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogSession;
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
 * Giving gift items to a character, for the favor it gives {@code CharacterData} and a per-item cooldown
 * rather than the flat amount a dish is worth. Ordering against a feed module is the caller's.
 * <p>
 * A favor that comes out at 0 or below is a refusal rather than a free gift: the item is not consumed
 * and no cooldown starts, so the interaction falls through. How much she thinks a category is worth is
 * her {@code gift_preference} datamap entry; see {@link GiftItemData#getFactor}.
 * <p>
 * A refusal is the one thing that gets a reaction of its own — she turns the item down in {@code 讨厌},
 * while anything she takes plays {@code 喜欢}.
 * <p>
 * Every accepted gift also banks its favor toward {@link CharacterData#giftReturn}, which is what she
 * eventually pays back; see {@link GiftDialogData}.
 *
 * @see "doc/design/gift.md"
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
		double factor = giftData.getFactor(stack, self);
		int favor = (int) (factor * giftData.favor());
		boolean accepted = favor > 0;
		// a factor too small to be worth anything truncates the favor to 0 but is not a refusal:
		// she takes it at face value and says nothing about it
		if (!accepted && factor > 0) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer sp)) {
			return accepted ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		if (accepted) {
			var relationship = CharDataHolder.get(sp, self);
			relationship.gain(favor, ReputationConstants.GIFT_SOFT_CAP, 0, 0);
			// the same favor, banked toward what she hands back; a character with no
			// gift_dialog entry has no threshold to reach, so it just sits there
			relationship.data().giftReturn += favor;
			relationship.sync();
			stack.shrink(1);
			giftCoolDown += giftData.cooldown();
			self.setFlag(YoukaiFlags.GIFTED, true);
			self.level().broadcastEntityEvent(self, EntityEvent.IN_LOVE_HEARTS);
			self.playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.2F);
		}
		react(sp, accepted);
		return accepted ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	/**
	 * What she says about the gift, which is a dialog for the seven characters who have one written
	 * and the bare {@code 喜欢} / {@code 讨厌} clip for everyone else.
	 * <p>
	 * A refusal returns {@code PASS}, so vanilla offers the other hand as well and lands here a second
	 * time; whoever arrives first owns the reaction and the clip is idempotent, so the second is a no-op.
	 */
	private void react(ServerPlayer sp, boolean accepted) {
		var config = GiftDialogData.of(self.getType());
		if (config == null) {
			(accepted ? YoukaiAnim.LIKE : YoukaiAnim.DISLIKE).play(self);
			return;
		}
		var dialog = accepted ? config.like() : config.dislike();
		var talk = self.getModule(TalkModule.class);
		if (talk.isEmpty() || talk.get().isTalking()) {
			(accepted ? YoukaiAnim.LIKE : YoukaiAnim.DISLIKE).play(self);
			return;
		}
		talk.get().beginTalking(sp);
		SimpleDialogSession.open(sp, self, new DirectDialogHandle(dialog), dialog);
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
