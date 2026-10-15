package dev.xkmc.gensokyolegacy.content.entity.behavior.module;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The want bubble's clock and its state, for any module that is also an {@link IBubbleModule}.
 * <p>
 * It runs on the module tick {@code YoukaiEntity.tickEffects} already drives, so a character picks the
 * bubble up by having one of these rather than by being wired into the entity. The id is the subclass's: a
 * bubble module is one module, not two, so there is nothing extra to register or save.
 *
 * @see "doc/design/want_bubble.md"
 */
public abstract class AbstractBubbleModule extends AbstractYoukaiModule implements IBubbleModule {

	/**
	 * Where the bubble is put, as the player sees it. Rolled per want, so she does not always think out of
	 * the same corner - {@code YoukaiBubbleLayer} is the side that has to know what the three mean.
	 */
	public static final int WANT_LEFT = 0;
	public static final int WANT_CENTER = 1;
	public static final int WANT_RIGHT = 2;
	private static final int WANT_PLACEMENTS = 3;

	/**
	 * Ten to twenty seconds between two wants, both ends inclusive.
	 */
	private static final int MIN_DELAY = 200;
	private static final int MAX_DELAY = 400;

	/**
	 * Ticks left until the next want. Rolled as the module is built rather than on its first tick, so a
	 * player who starts tracking her does not get a bubble the instant she appears.
	 */
	private int delay = rollDelay();

	/**
	 * What the bubble shows, which corner it went up in, and the tick it did. Client side only: written by
	 * {@link #tickClient()} and read by the layer, never saved and never sent.
	 */
	private ItemStack bubble = ItemStack.EMPTY;
	private int placement;
	private int bubbleAt;

	protected AbstractBubbleModule(ResourceLocation id, YoukaiEntity self) {
		super(id, self);
	}

	/**
	 * The clock. {@code YoukaiEntity#tickEffects} is the gate; this repeats it because everything
	 * written here is display state the renderer alone reads, so a server-side run is pure waste.
	 */
	@Override
	public void tickClient() {
		if (!self.level().isClientSide()) return;
		if (--delay > 0) return;
		delay = rollDelay();
		if (!mayShowWant()) {
			bubbleAt = -1000;
			bubble = ItemStack.EMPTY;
			return;
		}
		var wants = wants();
		if (wants.isEmpty()) return;
		bubble = wants.get(self.getRandom().nextInt(wants.size())).get();
		placement = self.getRandom().nextInt(WANT_PLACEMENTS);
		bubbleAt = self.tickCount;
	}

	@Override
	public ItemStack getWantBubble() {
		return bubble;
	}

	@Override
	public int getWantPlacement() {
		return placement;
	}

	@Override
	public float getWantBubbleAge(float partialTick) {
		return self.tickCount + partialTick - bubbleAt;
	}

	private int rollDelay() {
		return self.getRandom().nextInt(MIN_DELAY, MAX_DELAY + 1);
	}

}