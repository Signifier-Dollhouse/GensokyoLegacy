package dev.xkmc.gensokyolegacy.content.entity.behavior.module;

import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterData;
import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.content.client.debug.InfoUpdateClientManager;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.init.registrate.GLCriteriaTriggers;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A fairy is fed, and what she gives back depends on what she was given. Not the generic
 * {@link FeedModule}: a dish is worth a flat amount, and a well-fed fairy hands something over on the way in.
 * <p>
 * It is also what she asks for: the bubble over her head is this module's own, since what a fairy is thinking
 * about is what she eats and nothing else on her would know the answer.
 *
 * @see "doc/design/want_bubble.md"
 */
@SerialClass
public class FairyFeedModule extends AbstractBubbleModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("fairy_feed");

	/**
	 * Vanilla cake and the mod's own fairy cake - the two things she calls a meal, and the two
	 * worth ten reputation and a fairy dust rather than a flower.
	 */
	public static final Ingredient CAKE_FOOD = Ingredient.of(new ItemStack(Items.CAKE), GLItems.FAIRY_CAKE.asStack());

	/**
	 * Anything in the cookie tag. Not a slower cake but a cheaper, different trade: the flower it
	 * buys back is the cheap half on purpose. Climbing out of hostility is cake's job.
	 */
	public static final Ingredient COOKIE_FOOD = Ingredient.of(Tags.Items.FOODS_COOKIE);

	/**
	 * Reputation one cake is worth, and the ceiling feeding alone can reach.
	 */
	public static final int CAKE_REPUTATION = 10;
	public static final int CAKE_REPUTATION_CAP = 100;

	/**
	 * Reputation one cookie is worth, against ten for a cake.
	 */
	public static final int COOKIE_REPUTATION = 1;

	/**
	 * Ticks before she will take another dish: long enough that she cannot be farmed.
	 */
	private static final int COOL_DOWN = 400;

	/**
	 * Backstop on the feed memory, in case the task never runs and nothing clears it. Comfortably
	 * longer than the eating animation, so it only catches a behavior that never started.
	 */
	private static final int FEED_TTL = 200;

	/**
	 * The handoff into {@link GLBrains#MEM_FEED}: who was fed, the single dish she holds while
	 * she chews it, and the exact gift she owes. The dish and the gift both ride with the
	 * memory rather than being re-derived, since the flower is a roll: she reaches for
	 * <em>that</em> one, and the stack she puts in her hand is the one the player handed over.
	 */
	public record Fed(Player player, ItemStack food, ItemStack gift) {
	}

	/**
	 * What she makes of a given dish: the reputation it is worth and what she hands back for it. The
	 * gift is rolled per feeding rather than held in a table, since the flower is a die roll.
	 */
	private record Dish(int reputation, Supplier<ItemStack> gift) {
	}

	@SerialField
	private int feedCoolDown;

	public FairyFeedModule(YoukaiEntity self) {
		super(ID, self);
	}

	public int getCoolDown() {
		return feedCoolDown;
	}

	/**
	 * Whether she is still busy with the last dish she was given. Off the synced flag rather than
	 * {@link #getCoolDown()}, which is server-side state the client has no copy of, and the bubble is drawn
	 * on the client.
	 */
	public boolean isFed() {
		return self.getFlag(YoukaiFlags.FED);
	}

	/**
	 * What she hopes to be handed: the two dishes {@link #isFood} recognises, so the bubble only ever asks for
	 * things feeding her answers, plus the dust a cake pays back - the cheap half of the list is made of the
	 * cheap half of the trade, and a fairy who wants it is one the player can afford to satisfy.
	 */
	@Override
	public List<Supplier<ItemStack>> wants() {
		return List.of(
				GLItems.FAIRY_CAKE::asStack,
				() -> new ItemStack(Items.COOKIE),
				() -> new ItemStack(Items.CAKE));
	}

	/**
	 * No bubble while she is chewing or asleep: she is not asking for anything then, and a thought drawn over
	 * either reads as a glitch rather than as her thinking.
	 */
	@Override
	public boolean mayShowWant() {
		if (self.isAggressive() || self.getFlag(YoukaiFlags.FLEEING)) return false;
		if (self.isSleeping() || self.isPartying()) return false;
		return !isFed();
	}

	/**
	 * What she recognises as food: a cookie, or a cake. Both are things she would
	 * call a treat; everything else is not food she recognises.
	 */
	public static boolean isFood(ItemStack stack) {
		return CAKE_FOOD.test(stack) || COOKIE_FOOD.test(stack);
	}

	/**
	 * The thank-you for a cake: the dust is what the cake is made of, so meal and payment are the same
	 * ingredient. Overridden where the answer is not dust — {@code CirnoFeedModule} hands back a crystal.
	 */
	protected ItemStack cakeGift() {
		return new ItemStack(GLItems.FAIRY_DUST.get());
	}

	/**
	 * The thank-you for a cookie: one flower out of {@link ItemTags#FLOWERS}, rolled per feeding.
	 */
	protected ItemStack cookieGift() {
		var flowers = flowerGifts();
		return flowers.get(self.getRandom().nextInt(flowers.size()));
	}

	/**
	 * The dish a given stack is to her, or null for anything she will not eat — the one place a stack
	 * becomes what feeding it costs and pays. The gift stays a method so Cirno's crystal stays overridable.
	 */
	private @Nullable Dish dish(ItemStack stack) {
		if (CAKE_FOOD.test(stack)) return new Dish(CAKE_REPUTATION, this::cakeGift);
		if (COOKIE_FOOD.test(stack)) return new Dish(COOKIE_REPUTATION, this::cookieGift);
		return null;
	}

	/**
	 * The plain fairy's page: a cake is worth {@link #CAKE_REPUTATION} and buys the dust the cake is
	 * made of, a cookie {@link #COOKIE_REPUTATION} and any flower she can reach. The two sets are the
	 * ones {@link #cakeGift()} and {@link #cookieGift()} actually hand out of, so the page cannot
	 * promise something she would not give.
	 */
	public static List<EntityInteraction> interactions() {
		return page(List.of(new ItemStack(GLItems.FAIRY_DUST.get())), flowerGifts());
	}

	/**
	 * Assembles a fairy's page out of the two gifts her module hands back, so a subclass states only
	 * what is different from a plain fairy and inherits the two dishes and what they earn.
	 */
	protected static List<EntityInteraction> page(List<ItemStack> cake, List<ItemStack> cookie) {
		return List.of(new EntityInteraction(GLLang.JeiExtra.KIND_FEED.get(),
				GLLang.JeiExtra.FEED_NOTE.get(CAKE_REPUTATION_CAP),
				List.of(
						new EntityInteraction.Line(GLLang.JeiExtra.FEED_WORTH.get(CAKE_REPUTATION), CAKE_FOOD, cake),
						new EntityInteraction.Line(GLLang.JeiExtra.FEED_WORTH.get(COOKIE_REPUTATION), COOKIE_FOOD, cookie))));
	}

	/**
	 * Every flower out of {@link ItemTags#FLOWERS} she could reach for, which is what a cookie
	 * actually buys. Walks the registry each time so the list cannot go stale against a datapack;
	 * poppy is the fallback, not an exception.
	 */
	public static List<ItemStack> flowerGifts() {
		List<ItemStack> flowers = new ArrayList<>();
		for (var item : BuiltInRegistries.ITEM) {
			if (item.builtInRegistryHolder().is(ItemTags.FLOWERS)) flowers.add(new ItemStack(item));
		}
		if (flowers.isEmpty()) return List.of(new ItemStack(Items.POPPY));
		return flowers;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		// gated on FEAST, not on the blanket rule: she will not be fed mid-panic, but
		// will still be fed mid-conversation, which the blanket rule cannot tell apart
		ItemStack stack = player.getItemInHand(hand);
		if (!isFood(stack)) return InteractionResult.PASS;
		if (!self.mayInteract(player, GLBrains.FEAST.get())) return InteractionResult.FAIL;
		if (feedCoolDown > 0) return InteractionResult.FAIL;
		if (!(player instanceof ServerPlayer sp)) {
			InfoUpdateClientManager.clearCache();
			return InteractionResult.SUCCESS;
		}
		var dish = dish(stack);
		if (dish == null) return InteractionResult.FAIL;
		boolean welcomed = feed(sp, stack, dish);
		stack.shrink(1);
		// only a welcomed dish starts the timer: a player working their way back out
		// of the hole must keep feeding, or one cake every twenty seconds is a long walk
		if (welcomed) feedCoolDown += COOL_DOWN;
		self.level().broadcastEntityEvent(self, EntityEvent.IN_LOVE_HEARTS);
		self.playSound(stack.getEatingSound());
		return InteractionResult.SUCCESS;
	}

	/**
	 * Whether food counts as a gift or merely as a meal: whether she is still their enemy, the same
	 * line at which she starts accepting their hits. Both this module and {@code FairyFeedTask} ask it.
	 */
	public static boolean welcomed(CharacterData data) {
		return data.reputation > ReputationConstants.COMBAT_SAFE_THRESHOLD;
	}

	/**
	 * Worth the same at any reputation, but not <em>received</em> the same way: while she counts the
	 * player an enemy there is no gift and no cooldown, so they can keep feeding until one lands as a gift.
	 * The throw itself is {@code FairyFeedTask}'s, two beats later.
	 *
	 * @param stack the dish as handed over, passed to the criterion trigger before the stack is
	 *              shrunk: an empty stack would match no item predicate
	 * @return whether it counted as a peace offering, i.e. she was not hostile
	 */
	private boolean feed(ServerPlayer sp, ItemStack stack, Dish dish) {
		if (self.getData(sp).isEmpty()) return false;
		var holder = self.getData(sp).get();
		var data = holder.data();
		boolean welcomed = welcomed(data);
		// no soft cap: a dish is worth the same at 90 as at 0, and the
		// relationship is capped flat, not at whatever quests have unlocked
		data.gainReputation(dish.reputation(), 0, 0, 0);
		data.reputation = Math.min(data.reputation, CAKE_REPUTATION_CAP);
		holder.sync();
		// past the empty-data bail above, so it fires on the same dishes that actually
		// moved the reputation and not on ones fed to a relationship that does not exist
		GLCriteriaTriggers.FEED_FAIRY.get().trigger(sp, self, stack);
		// rolled now rather than at the throw: she reaches for the flower she was
		// given, not for whatever comes up two seconds later. One of the pair, too -
		// a stack of nine cakes hands over a cake, not all of them
		BrainUtils.setForgettableMemory(self, GLBrains.MEM_FEED.get(),
				new Fed(sp, stack.copyWithCount(1), dish.gift().get()), FEED_TTL);
		return welcomed;
	}

	/**
	 * The thank-you, tossed so it lands at the player's feet rather than appearing there. Public because
	 * {@code FairyFeedTask} decides <em>when</em> she reaches for it — the gift is only fair after she eats.
	 */
	public void throwGift(Player player, ItemStack gift) {
		var item = new ItemEntity(self.level(), self.getX(), self.getEyeY() - 0.1, self.getZ(), gift);
		item.setDefaultPickUpDelay();
		aim(item, self.getEyePosition(), player.getBoundingBox().getCenter());
		self.level().addFreshEntity(item);
	}

	/**
	 * Flight time scales with range, so a nearby gift is a lob and a far one an arc. The vertical lead is
	 * deliberately partial: a thrown item sheds 1% of its speed per tick, so the gift lands short.
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
	 * The client is told, so the want bubble knows to keep quiet while she is chewing - the same bit the
	 * generic {@link FeedModule} puts up for its own client effects.
	 */
	@Override
	public void tickServer() {
		if (feedCoolDown > 0) feedCoolDown--;
		self.setFlag(YoukaiFlags.FED, feedCoolDown > 0);
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