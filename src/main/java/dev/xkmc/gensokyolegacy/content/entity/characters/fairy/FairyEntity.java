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
 * bubble over her head every ten to twenty seconds with one item in it, out of
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
	 * head. It travels as an entity event whose payload is the index into wantItems(),
	 * the same way the chew and the flinch travel - see WANT_EVENT below.
	 *
	 * Both halves live here rather than on the task because both sides of the wire
	 * need the list: the server rolls an index out of it, and the client turns that
	 * index back into a stack. One list, two readers, no item on the wire.
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
	 * The want bubble's event id; the index into {@link #wantItems()} is added to it.
	 * <p>
	 * 87 upward is the first free block: the dolls own 66-70, the dialog animations
	 * 71-83, and the chew, the flinch and the gather 84-86 (see
	 * {@code GeoYoukaiAnim#ANIM_EVENT_BASE}). A byte is the whole payload, so the list
	 * has room for {@code 255 - 87} entries, which is the only cap on {@link #wantItems}
	 * worth knowing about.
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
		level().broadcastEntityEvent(this, (byte) (WANT_EVENT + random.nextInt(wants.size())));
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide() && handleWantEvent(id)) return;
		super.handleEntityEvent(id);
	}

	/**
	 * Client side: turn the id back into the item it names. Answers {@code false} for
	 * everything that is not a want, so the id continues down the chain to the
	 * animation handler and to vanilla rather than being swallowed here.
	 */
	private boolean handleWantEvent(byte id) {
		int index = id - WANT_EVENT;
		var wants = wantItems();
		if (index < 0 || index >= wants.size()) return false;
		wantBubble = wants.get(index).get();
		wantBubbleAt = tickCount;
		return true;
	}

	/**
	 * What the bubble over her head is showing, and the tick it arrived on.
	 * <p>
	 * Client side only: written by {@link #handleWantEvent} and read back by
	 * {@code FairyWantLayer}, never saved and never sent in its own right. The stack
	 * is the display of a moment rather than a state of hers - a player who arrives
	 * after the moment has passed has nothing to see, and the server's copy of both
	 * fields stays empty for ever. How long it stays up for is the layer's number,
	 * not hers; the server is done with the want the moment it has sent it.
	 */
	private ItemStack wantBubble = ItemStack.EMPTY;
	private int wantBubbleAt;

	public ItemStack getWantBubble() {
		return wantBubble;
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
