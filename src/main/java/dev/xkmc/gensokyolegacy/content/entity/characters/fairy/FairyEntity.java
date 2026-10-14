package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.YoukaiCombatManager;
import dev.xkmc.gensokyolegacy.content.entity.module.AbstractYoukaiModule;
import dev.xkmc.gensokyolegacy.content.entity.module.HomeModule;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.module.TalkModule;
import dev.xkmc.gensokyolegacy.content.entity.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * The shared fairy base. A fairy has no {@link TalkModule}, unlike
 * {@link YoukaiEntity}'s default: a fairy has no dialog of her own to offer, so a
 * conversation with one only ever opened an empty screen. She is fed instead.
 * <p>
 * Subclasses that override {@link #createModules()} - the plain fairy and Cirno -
 * have to leave the talk module out for the same reason.
 * <p>
 * What a fairy does have to say is said without words: {@link FairyWantTask} puts a
 * bubble up beside her head every ten to twenty seconds with one item in it, out of
 * {@link #wantItems()}. That is where the whole fairy family - the plain fairy, Cirno
 * and the three named ones on the placeholder rig - gets its one bit of speech, and it
 * is why the list of dishes is here rather than on the task: what she wants is a
 * property of the fairy, and what the task owns is only the timing.
 */
@SerialClass
public class FairyEntity extends GeneralYoukaiEntity {

	public static AttributeSupplier.Builder createAttributes() {
		return GeneralYoukaiEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.ATTACK_DAMAGE, 6);
	}

	@Override
	protected List<AbstractYoukaiModule> createModules() {
		return List.of(new HomeModule(this), new VisitModule(this), new PartyModule(this));
	}

	public FairyEntity(EntityType<? extends GeneralYoukaiEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	protected YoukaiCombatManager createCombatManager() {
		return new FairyCombatManager(this);
	}

	@Override
	protected void constructTaskBoard(TaskBoard board) {
		super.constructTaskBoard(board);
		// always rather than random: the want keeps its own ten-to-twenty-second clock,
		// so it must not be waiting for the one random slot to come round to it
		board.addAlways(new FairyWantTask(), Activity.IDLE, Activity.PLAY, GLBrains.AT_HOME.get());
	}

	/*
	 * ============================== the want bubble ==============================
	 *
	 * A fairy's one spontaneous utterance: an item she is hoping for, shown over her
	 * head. It travels as an entity event carrying two numbers - which item, and where the
	 * bubble is put - the same way the chew and the flinch travel, see WANT_EVENT below.
	 *
	 * Both halves of the list live here rather than on the task because both sides of the
	 * wire need it: the server rolls an index out of it, and the client turns that index
	 * back into a stack. One list, two readers, no item on the wire.
	 */

	/**
	 * What she hopes to be handed, in the order the entity event indexes them.
	 * <p>
	 * The two dishes are exactly what {@code FairyFeedModule#isFood} recognises - one
	 * cookie, and either cake - so the bubble asks for things feeding her actually
	 * answers. The fairy dust is the odd one out and is there on purpose: it is what a
	 * cake buys the player back, so the cheap half of the list is also what the cheap
	 * half of the trade is made of, and a fairy who wants it is a fairy the player can
	 * afford to satisfy.
	 * <p>
	 * Built per call rather than held in a static field, and overridable, because this
	 * is where a character's own taste would go - a Cirno who wants ice says so by
	 * returning a different list, on both sides at once.
	 */
	protected List<Supplier<ItemStack>> wantItems() {
		return List.of(
				GLItems.FAIRY_DUST::asStack,
				GLItems.FAIRY_CAKE::asStack,
				() -> new ItemStack(Items.COOKIE),
				() -> new ItemStack(Items.CAKE));
	}

	/**
	 * Where the bubble is put, as the player sees it: off to her left, straight over her
	 * head, or off to her right. Rolled per want, so she does not always think out of the
	 * same corner - see {@code FairyWantLayer}, which is the side that has to know what the
	 * three mean.
	 */
	public static final int WANT_LEFT = 0;
	public static final int WANT_CENTER = 1;
	public static final int WANT_RIGHT = 2;
	private static final int WANT_PLACEMENTS = 3;

	/**
	 * The want bubble's event id; the payload is {@code index * WANT_PLACEMENTS + placement},
	 * the index into {@link #wantItems()} and where the bubble is put.
	 * <p>
	 * 87 upward is the first free block: the dolls own 66-70, the dialog animations
	 * 71-83, and the chew, the flinch and the gather 84-86 (see
	 * {@code GeoYoukaiAnim#ANIM_EVENT_BASE}). A byte is the whole payload, so the two
	 * numbers together have room for {@code (255 - 87) / WANT_PLACEMENTS} items, which is
	 * the only cap on {@link #wantItems} worth knowing about.
	 * <p>
	 * The placement is decided here rather than rolled where it is drawn, so that every
	 * player watching the same fairy sees the same bubble in the same spot: it is a
	 * decision about the fairy, not about the frame.
	 */
	private static final byte WANT_EVENT = 87;

	/**
	 * Show, to whoever is watching, one of the things she wants. Server side; the
	 * choosing is hers and the drawing is the client's.
	 */
	public void broadcastWant(RandomSource random) {
		if (level().isClientSide()) return;
		var wants = wantItems();
		if (wants.isEmpty()) return;
		int payload = random.nextInt(wants.size()) * WANT_PLACEMENTS + random.nextInt(WANT_PLACEMENTS);
		level().broadcastEntityEvent(this, (byte) (WANT_EVENT + payload));
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide() && handleWantEvent(id)) return;
		super.handleEntityEvent(id);
	}

	/**
	 * Client side: turn the id back into the item it names and where it is put. Answers
	 * {@code false} for everything that is not a want, so the id continues down the chain
	 * to the animation handler and to vanilla rather than being swallowed here.
	 */
	private boolean handleWantEvent(byte id) {
		int payload = id - WANT_EVENT;
		var wants = wantItems();
		if (payload < 0 || payload >= wants.size() * WANT_PLACEMENTS) return false;
		wantBubble = wants.get(payload / WANT_PLACEMENTS).get();
		wantPlacement = payload % WANT_PLACEMENTS;
		wantBubbleAt = tickCount;
		return true;
	}

	/**
	 * What the bubble beside her head is showing, where it was put, and the tick it
	 * arrived on.
	 * <p>
	 * Client side only: written by {@link #handleWantEvent} and read back by
	 * {@code FairyWantLayer}, never saved and never sent in its own right. The stack is
	 * the display of a moment rather than a state of hers - a player who arrives after
	 * the moment has passed has nothing to see, and the server's copy of all three fields
	 * stays empty for ever. How long it stays up for is the layer's number, not hers; the
	 * server is done with the want the moment it has sent it.
	 */
	private ItemStack wantBubble = ItemStack.EMPTY;
	private int wantPlacement;
	private int wantBubbleAt;

	public ItemStack getWantBubble() {
		return wantBubble;
	}

	/**
	 * Where the current want was put: {@link #WANT_LEFT}, {@link #WANT_CENTER} or
	 * {@link #WANT_RIGHT}, as the player sees it.
	 */
	public int getWantPlacement() {
		return wantPlacement;
	}

	/**
	 * Ticks since the current want arrived, partial tick included: what the renderer
	 * ages the bubble by. Meaningless before the first want, which is fine - an empty
	 * stack is skipped before the age is ever read.
	 */
	public float getWantBubbleAge(float pTick) {
		return tickCount + pTick - wantBubbleAt;
	}

}
