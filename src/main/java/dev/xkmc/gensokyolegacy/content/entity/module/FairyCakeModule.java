package dev.xkmc.gensokyolegacy.content.entity.module;

import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterData;
import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.content.client.debug.InfoUpdateClientManager;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A plain fairy is fed cake and nothing else.
 * <p>
 * Deliberately not the generic {@link FeedModule}: a cake is worth a flat amount
 * here instead of a favor scaled off its nutrition, the ceiling is one feeding
 * alone cannot push past, and a well-fed fairy throws a flower at you on the way
 * in. Anything that is not cake falls through to the next module, which is what
 * leaves {@link TalkModule} reachable with an empty hand.
 * <p>
 * Cake does the same thing for a player she is still hostile towards, minus the
 * gifts - see {@link #feed}.
 */
@SerialClass
public class FairyCakeModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("cake_feed");

	/** Reputation one cake is worth, and the ceiling feeding alone can reach. */
	public static final int CAKE_REPUTATION = 10;
	public static final int CAKE_REPUTATION_CAP = 100;

	/** Ticks before she will take another cake: long enough that she cannot be farmed. */
	private static final int COOL_DOWN = 400;

	/**
	 * Backstop on the feed memory, in case the task never runs and nothing clears it.
	 * Comfortably longer than the eating animation, so it only catches the case where
	 * the behavior itself never started - a memory left in place would otherwise sit
	 * there and hand the next cake to an animation that already ran.
	 */
	private static final int FEED_TTL = 200;

	@SerialField
	private int feedCoolDown;

	public FairyCakeModule(YoukaiEntity self) {
		super(ID, self);
	}

	public int getCoolDown() {
		return feedCoolDown;
	}

	/**
	 * Vanilla cake and the mod's own fairy cake - the two things a fairy would
	 * call cake. Everything else is not food she recognises.
	 */
	public static boolean isCake(ItemStack stack) {
		return stack.is(Items.CAKE) || stack.is(GLItems.FAIRY_CAKE);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		// gated on FEAST, not on the blanket rule: she will not be fed mid-panic, and
		// she will still be fed mid-conversation, neither of which the blanket rule can
		// tell apart
		if (!self.mayInteract(player, GLBrains.FEAST.get())) return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		if (!isCake(stack)) return InteractionResult.PASS;
		if (feedCoolDown > 0) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer sp)) {
			InfoUpdateClientManager.clearCache();
			return InteractionResult.SUCCESS;
		}
		boolean welcomed = feed(sp);
		stack.shrink(1);
		// only a welcomed cake starts the timer: a player working their way back
		// out of the hole has to be able to keep feeding, or one cake every twenty
		// seconds is a long walk back from a bad reputation
		if (welcomed) feedCoolDown += COOL_DOWN;
		self.level().broadcastEntityEvent(self, EntityEvent.IN_LOVE_HEARTS);
		self.playSound(stack.getEatingSound());
		self.getModule(TalkModule.class).ifPresent(e -> e.beginTalking(sp));
		return InteractionResult.SUCCESS;
	}

	/**
	 * Whether cake counts as a gift for this player or merely as food: the question
	 * is whether she is still their enemy, which is the same line at which she starts
	 * accepting their hits. Both this module and {@code FairyCakeTask} ask it, since
	 * the cooldown is decided here and the flower is handed over there.
	 */
	public static boolean welcomed(CharacterData data) {
		return data.reputation > ReputationConstants.COMBAT_SAFE_THRESHOLD;
	}

	/**
	 * Cake is worth the same at any reputation, but it is not <em>received</em> the
	 * same way. While she still counts the player as an enemy the cake is only food:
	 * reputation goes up, but there is no flower and no cooldown, so the player can
	 * keep feeding until she is back at zero and the next cake lands as a gift.
	 * Without that, making amends would take a dozen cakes and twenty seconds of
	 * standing still each.
	 * <p>
	 * Eating and handing something back are two beats apart, so nothing is thrown
	 * from here: the player is left in {@link GLBrains#MEM_FEED} and
	 * {@code FairyCakeTask} plays the first beat, then the second.
	 *
	 * @return whether the cake counted as a peace offering, i.e. she was not hostile
	 */
	private boolean feed(ServerPlayer sp) {
		if (self.getData(sp).isEmpty()) return false;
		var holder = self.getData(sp).get();
		var data = holder.data();
		boolean welcomed = welcomed(data);
		// no soft cap: a cake is worth the same at 90 as at 0, and the
		// relationship is capped flat, not at whatever quests have unlocked
		data.gainReputation(CAKE_REPUTATION, 0, 0, 0);
		data.reputation = Math.min(data.reputation, CAKE_REPUTATION_CAP);
		holder.sync();
		BrainUtils.setForgettableMemory(self, GLBrains.MEM_FEED.get(), sp, FEED_TTL);
		return welcomed;
	}

	/**
 * The thank-you. One flower at random out of the vanilla flower tag, tossed
 * at the player so it lands at their feet rather than appearing there.
 * <p>
 * Public because {@code FairyCakeTask} is what decides <em>when</em> she reaches
 * for it - it is only fair if the gift arrives after she has eaten.
 */
public void throwFlower(Player sp) {
		var flower = new ItemEntity(self.level(), self.getX(), self.getEyeY() - 0.1, self.getZ(),
				new ItemStack(randomFlower()));
		flower.setDefaultPickUpDelay();
		aim(flower, self.getEyePosition(), sp.getBoundingBox().getCenter());
		self.level().addFreshEntity(flower);
	}

	/**
	 * Flight time scales with range, so a nearby gift is a lob and a far one is an
	 * arc. The vertical lead is deliberately partial: a thrown item sheds 1% of
	 * its speed every tick, so the throw lands a little short of the target
	 * rather than sailing over its head - the flower drops at their feet and is
	 * picked up from there.
	 */
	private static void aim(ItemEntity flower, Vec3 from, Vec3 to) {
		int ticks = (int) Mth.clamp(Math.sqrt(from.distanceToSqr(to)) * 5.0, 10.0, 40.0);
		var delta = to.subtract(from);
		double time = ticks;
		// 0.015 is half the item's per-tick gravity, i.e. the lead that would carry
		// it level to the target over that flight time.
		flower.setDeltaMovement(delta.x / time, delta.y / time + 0.015 * time, delta.z / time);
		flower.hasImpulse = true;
	}

	private Item randomFlower() {
		List<Item> flowers = new ArrayList<>();
		for (var item : BuiltInRegistries.ITEM) {
			if (item.builtInRegistryHolder().is(ItemTags.FLOWERS)) flowers.add(item);
		}
		return flowers.isEmpty() ? Items.POPPY : flowers.get(self.getRandom().nextInt(flowers.size()));
	}

	@Override
	public void tickServer() {
		if (feedCoolDown > 0) feedCoolDown--;
	}

	@Override
	public boolean handleEntityEvent(byte pId) {
		if (pId != EntityEvent.IN_LOVE_HEARTS) return false;
		for (int i = 0; i < 7; ++i) {
			double d0 = self.getRandom().nextGaussian() * 0.02D;
			double d1 = self.getRandom().nextGaussian() * 0.02D;
			double d2 = self.getRandom().nextGaussian() * 0.02D;
			self.level().addParticle(ParticleTypes.HEART,
					self.getRandomX(1.0D), self.getRandomY() + 0.5D, self.getRandomZ(1.0D), d0, d1, d2);
		}
		return true;
	}

}