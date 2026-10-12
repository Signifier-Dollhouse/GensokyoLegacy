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
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A plain fairy is fed, and what she gives back is a matter of what she was given.
 * <p>
 * Deliberately not the generic {@link FeedModule}: a dish is worth a flat amount
 * here instead of a favor scaled off its nutrition, the ceiling is one feeding
 * alone cannot push past, and a well-fed fairy hands something over on the way in.
 * Anything she does not eat falls through to the next module, which is what leaves
 * {@link TalkModule} reachable with an empty hand.
 * <p>
 * Two tiers, not a list of dishes: a cookie is a snack and a cake of either kind is
 * a meal - {@link #COOKIE_REPUTATION} against {@link #CAKE_REPUTATION}, and a flower
 * against a fairy dust, which is what the fairy cake is made from. So the cheap
 * thing you can carry buys a flower, and the expensive one feeds the cake loop.
 * <p>
 * Food does the same thing for a player she is still hostile towards, minus the
 * gifts - see {@link #feed}.
 */
@SerialClass
public class FairyFeedModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("fairy_feed");

	/** Reputation one cake is worth, and the ceiling feeding alone can reach. */
	public static final int CAKE_REPUTATION = 10;
	public static final int CAKE_REPUTATION_CAP = 100;

	/**
	 * Reputation one cookie is worth, against ten for a cake. Deliberately small: it is
	 * not a slower cake but a different, cheaper thing to hand over, and the flower it
	 * buys back is the cheap half of the trade on purpose. Climbing out of hostility is
	 * what cake is for; a cookie is what you feed her once she is already willing.
	 */
	public static final int COOKIE_REPUTATION = 1;

	/** Ticks before she will take another dish: long enough that she cannot be farmed. */
	private static final int COOL_DOWN = 400;

	/**
	 * Backstop on the feed memory, in case the task never runs and nothing clears it.
	 * Comfortably longer than the eating animation, so it only catches the case where
	 * the behavior itself never started - a memory left in place would otherwise sit
	 * there and hand the next dish to an animation that already ran.
	 */
	private static final int FEED_TTL = 200;

	/**
	 * The handoff into {@link GLBrains#MEM_FEED}: who was fed, and the exact stack she
	 * owes them for it.
	 * <p>
	 * The gift travels with the memory rather than being re-derived from the food at
	 * the far end, because one of the two is a roll: she reaches for <em>that</em>
	 * flower once she has finished chewing, not for whatever comes up next.
	 */
	public record Fed(Player player, ItemStack gift) {}

	/**
	 * What she makes of a given dish: the reputation it is worth and what she hands
	 * back for it. Rolled per feeding rather than held in a table, since the flower is
	 * a die roll.
	 */
	private record Dish(int reputation, Supplier<ItemStack> gift) {}

	@SerialField
	private int feedCoolDown;

	public FairyFeedModule(YoukaiEntity self) {
		super(ID, self);
	}

	public int getCoolDown() {
		return feedCoolDown;
	}

	/**
	 * What she recognises as food: a cookie, or a cake. Both are things she would
	 * call a treat; everything else is not food she recognises.
	 */
	public static boolean isFood(ItemStack stack) {
		return isCake(stack) || stack.is(Items.COOKIE);
	}

	/**
	 * Vanilla cake and the mod's own fairy cake - the two things she calls a meal, and
	 * the two worth ten reputation and a fairy dust rather than a flower.
	 */
	private static boolean isCake(ItemStack stack) {
		return stack.is(Items.CAKE) || stack.is(GLItems.FAIRY_CAKE);
	}

	/**
	 * The dish a given stack is to her, or null for anything she will not eat. The one
	 * place a stack is turned into what feeding it costs and pays, and the only place
	 * that rolls a gift - so the roll belongs to her own random source.
	 */
	private @Nullable Dish dish(ItemStack stack) {
		if (isCake(stack)) return new Dish(CAKE_REPUTATION, () -> new ItemStack(GLItems.FAIRY_DUST.get()));
		if (stack.is(Tags.Items.FOODS_COOKIE)) return new Dish(COOKIE_REPUTATION, () -> new ItemStack(randomFlower()));
		return null;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		// gated on FEAST, not on the blanket rule: she will not be fed mid-panic, and
		// she will still be fed mid-conversation, neither of which the blanket rule can
		// tell apart
		if (!self.mayInteract(player, GLBrains.FEAST.get())) return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		if (!isFood(stack)) return InteractionResult.PASS;
		if (feedCoolDown > 0) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer sp)) {
			InfoUpdateClientManager.clearCache();
			return InteractionResult.SUCCESS;
		}
		var dish = dish(stack);
		if (dish == null) return InteractionResult.PASS;
		boolean welcomed = feed(sp, dish);
		stack.shrink(1);
		// only a welcomed dish starts the timer: a player working their way back
		// out of the hole has to be able to keep feeding, or one cake every twenty
		// seconds is a long walk back from a bad reputation
		if (welcomed) feedCoolDown += COOL_DOWN;
		self.level().broadcastEntityEvent(self, EntityEvent.IN_LOVE_HEARTS);
		self.playSound(stack.getEatingSound());
		self.getModule(TalkModule.class).ifPresent(e -> e.beginTalking(sp));
		return InteractionResult.SUCCESS;
	}

	/**
	 * Whether food counts as a gift for this player or merely as a meal: the question
	 * is whether she is still their enemy, which is the same line at which she starts
	 * accepting their hits. Both this module and {@code FairyFeedTask} ask it, since
	 * the cooldown is decided here and the gift is handed over there.
	 */
	public static boolean welcomed(CharacterData data) {
		return data.reputation > ReputationConstants.COMBAT_SAFE_THRESHOLD;
	}

	/**
	 * A dish is worth the same at any reputation, but it is not <em>received</em> the
	 * same way. While she still counts the player as an enemy the food is only that:
	 * reputation goes up, but there is no gift and no cooldown, so the player can keep
	 * feeding until she is back at zero and the next dish lands as a gift.
	 * Without that, making amends would take a dozen cakes - or several hundred
	 * cookies, at {@link #COOKIE_REPUTATION} - and twenty seconds of standing still
	 * each.
	 * <p>
	 * Eating and handing something back are two beats apart, so nothing is thrown
	 * from here: the player is left in {@link GLBrains#MEM_FEED} and
	 * {@code FairyFeedTask} plays the first beat, then the second.
	 *
	 * @return whether it counted as a peace offering, i.e. she was not hostile
	 */
	private boolean feed(ServerPlayer sp, Dish dish) {
		if (self.getData(sp).isEmpty()) return false;
		var holder = self.getData(sp).get();
		var data = holder.data();
		boolean welcomed = welcomed(data);
		// no soft cap: a dish is worth the same at 90 as at 0, and the
		// relationship is capped flat, not at whatever quests have unlocked
		data.gainReputation(dish.reputation(), 0, 0, 0);
		data.reputation = Math.min(data.reputation, CAKE_REPUTATION_CAP);
		holder.sync();
		// rolled now rather than at the throw: she reaches for the flower she was
		// given, not for whatever comes up two seconds later
		BrainUtils.setForgettableMemory(self, GLBrains.MEM_FEED.get(), new Fed(sp, dish.gift().get()), FEED_TTL);
		return welcomed;
	}

	/**
	 * The thank-you. One flower at random out of the vanilla flower tag, or the fairy
	 * dust a cake earned - tossed at the player so it lands at their feet rather than
	 * appearing there.
	 * <p>
	 * Public because {@code FairyFeedTask} is what decides <em>when</em> she reaches
	 * for it - it is only fair if the gift arrives after she has eaten.
	 */
	public void throwGift(Player player, ItemStack gift) {
		var item = new ItemEntity(self.level(), self.getX(), self.getEyeY() - 0.1, self.getZ(), gift);
		item.setDefaultPickUpDelay();
		aim(item, self.getEyePosition(), player.getBoundingBox().getCenter());
		self.level().addFreshEntity(item);
	}

	/**
	 * Flight time scales with range, so a nearby gift is a lob and a far one is an
	 * arc. The vertical lead is deliberately partial: a thrown item sheds 1% of
	 * its speed every tick, so the throw lands a little short of the target
	 * rather than sailing over its head - the gift drops at their feet and is
	 * picked up from there.
	 */
	private static void aim(ItemEntity gift, Vec3 from, Vec3 to) {
		int ticks = (int) Mth.clamp(Math.sqrt(from.distanceToSqr(to)) * 5.0, 10.0, 40.0);
		var delta = to.subtract(from);
		double time = ticks;
		// 0.015 is half the item's per-tick gravity, i.e. the lead that would carry
		// it level to the target over that flight time.
		gift.setDeltaMovement(delta.x / time, delta.y / time + 0.015 * time, delta.z / time);
		gift.hasImpulse = true;
	}

	/**
	 * One flower out of {@link ItemTags#FLOWERS}, of which a datapack can leave an
	 * empty tag - poppy is the fallback rather than an exception, because the whole
	 * point of this is that something always lands at their feet.
	 * <p>
	 * Walks the item registry every time rather than caching the list: it is one scan
	 * per cookie fed, and a cached list would go stale against a datapack that edits
	 * the tag.
	 */
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