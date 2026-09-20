package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity.CombatData;
import dev.xkmc.l2serial.serialization.marker.OnInject;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;

/**
 * DCStack-style immutable doll item component. Holds only the combat snapshot {@link CombatData}:
 * the doll type is fixed ({@link DollItem#TYPE}, not stored on the item), and the custom name and
 * tint live in the vanilla {@code CUSTOM_NAME} and the mod's {@code DOLL_COLOR} components.
 * More fields are added here in the future.
 * <p>
 * The hash is prebuilt over the final {@link #combat} field and cached like {@code DCStack}; since
 * l2serial reconstructs the instance via the no-arg ctor and writes {@link #combat} afterwards,
 * {@link #onInject()} rebuilds the cache after deserialization (same trick as
 * {@code TalismanPocketData#onInject}).
 */
@SerialClass
public final class DollItemData {

	/**
	 * A freshly made doll (no {@code DOLL_DATA} component) is a pristine doll: full health, no
	 * custom data. Use as the fallback wherever the component is missing.
	 */
	public static DollItemData fresh() {
		return new DollItemData();
	}

	@SerialField
	private CombatData combat = new CombatData(BaseDollEntity.DEFAULT_MAX_HEALTH, 0);

	private int hashCode;

	public DollItemData() {
	}

	public DollItemData(CombatData combat) {
		this.combat = combat;
		hashCode = buildHash(combat);
	}

	public CombatData combat() {
		return combat;
	}

	@OnInject
	public void onInject() {
		hashCode = buildHash(combat);
	}

	private static int buildHash(CombatData combat) {
		int h = Float.floatToIntBits(combat.amount());
		h = 31 * h + Float.floatToIntBits(combat.baseline());
		return h == 0 ? 1 : h;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DollItemData s && hashCode == s.hashCode && combat.equals(s.combat);
	}

}