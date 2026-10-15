package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.YoukaiCombatManager;
import dev.xkmc.gensokyolegacy.content.entity.module.AbstractYoukaiModule;
import dev.xkmc.gensokyolegacy.content.entity.module.FairyFeedModule;
import dev.xkmc.gensokyolegacy.content.entity.module.HomeModule;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.module.TalkModule;
import dev.xkmc.gensokyolegacy.content.entity.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.l2core.base.entity.SyncedData;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The shared fairy base. A fairy has no {@link TalkModule}, unlike
 * {@link YoukaiEntity}'s default: a fairy has no dialog of her own to offer, so a
 * conversation with one only ever opened an empty screen. She is fed instead.
 * <p>
 * Subclasses that override {@link #createModules()} - the plain fairy and Cirno -
 * have to leave the talk module out for the same reason.
 * <p>
 * What a fairy does have to say is said without words: a bubble over her head every ten
 * to twenty seconds with one item in it, out of {@link FairyFeedModule#wants}.
 *
 * @see "doc/design/want_bubble.md"
 */
@SerialClass
public class FairyEntity extends GeneralYoukaiEntity {

	/**
	 * {@link SyncedData} ships a handful of primitives and no stack; the dish rides as a plain
	 * {@link ItemStack} codec over the vanilla serializer.
	 */
	private static final SyncedData.Serializer<ItemStack> ITEM =
			new SyncedData.Simple<>(EntityDataSerializers.ITEM_STACK, ItemStack.CODEC);

	/**
	 * Crumbs, on vanilla's own cadence: {@link #CRUMBS} every {@link #CRUMB_EVERY} ticks.
	 * Vanilla also holds them back until {@code useItem.getUseDuration() * 0.21875}, which
	 * has nothing to bite into here - a fairy's chew is a rig clip, not a use timer.
	 */
	private static final int CRUMB_EVERY = 4;
	private static final int CRUMBS = 5;

	protected static final SyncedData FAIRY_DATA = new SyncedData(FairyEntity::defineId, SPELL_DATA);

	// null names: a chew is a gesture, not a state. NBT-backed values would survive an
	// unload that cuts the task off before its stop(), and she would crumb forever on reload.
	private static final EntityDataAccessor<ItemStack> DATA_EAT_ITEM = FAIRY_DATA.define(ITEM, ItemStack.EMPTY, null);
	private static final EntityDataAccessor<Integer> DATA_EAT_TICKS = FAIRY_DATA.define(SyncedData.INT, 0, null);

	private static <T> EntityDataAccessor<T> defineId(EntityDataSerializer<T> ser) {
		return SynchedEntityData.defineId(FairyEntity.class, ser);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GeneralYoukaiEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.ATTACK_DAMAGE, 6);
	}

	@Override
	protected List<AbstractYoukaiModule> createModules() {
		return List.of(new HomeModule(this), new VisitModule(this), new PartyModule(this));
	}

	@Override
	protected SyncedData data() {
		return FAIRY_DATA;
	}

	public FairyEntity(EntityType<? extends GeneralYoukaiEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	protected YoukaiCombatManager createCombatManager() {
		return new FairyCombatManager(this);
	}

	// ---------- eating ----------

	/**
	 * The dish she is chewing, and how many ticks of the chew are left.
	 * <p>
	 * Both are client-visible because the chew has no vanilla use timer for the renderer to hang
	 * on: the clip is a rig animation played by {@code FairyFeedTask}, so the crumbs are driven
	 * off this countdown rather than off {@code isUsingItem()}. The stack outlives the hand item
	 * on purpose - {@code FairyFeedTask} takes the dish out of her hand halfway through, but the
	 * crumbs keep going to the end.
	 *
	 * @see FairyFeedTask
	 */
	public ItemStack getEatItem() {
		return entityData.get(DATA_EAT_ITEM);
	}

	public int getEatTicks() {
		return entityData.get(DATA_EAT_TICKS);
	}

	public void setEatItem(ItemStack stack) {
		entityData.set(DATA_EAT_ITEM, stack);
	}

	public void setEatTicks(int ticks) {
		entityData.set(DATA_EAT_TICKS, ticks);
	}

	/**
	 * Crumbs through the whole chew, not the half of it the dish is in her hand for. Client-side
	 * only: server-side {@code addParticle} is a broadcast, so ticking this on the server would
	 * send every crumb to every watching client <em>as well</em> as the client drawing its own.
	 */
	@Override
	protected void tickEffects() {
		super.tickEffects();
		int left = getEatTicks();
		if (!level().isClientSide() || left <= 0 || left % CRUMB_EVERY != 0) return;
		var food = getEatItem();
		if (food.isEmpty()) return;
		for (int i = 0; i < CRUMBS; i++) crumb(food);
	}

	/**
	 * One crumb at her mouth, thrown the way vanilla throws one: a small cone out of the eye
	 * position, which on these rigs is close enough to the mouth for the spread to read as coming
	 * from it rather than from her chest.
	 */
	private void crumb(ItemStack food) {
		var out = new Vec3((random.nextFloat() - 0.5F) * 0.2f, -random.nextFloat() * 0.2 - 0.1, 0.6)
				.xRot(-getXRot() * ((float) Math.PI / 180F))
				.yRot(-getYRot() * ((float) Math.PI / 180F))
				.add(getEyePosition());
		var vel = new Vec3((random.nextFloat() - 0.5F) * 0.1, random.nextFloat() * 0.1 + 0.1, 0.0)
				.xRot(-getXRot() * ((float) Math.PI / 180F))
				.yRot(-getYRot() * ((float) Math.PI / 180F));
		level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, food),
				out.x, out.y, out.z, vel.x, vel.y + 0.05, vel.z);
	}

}