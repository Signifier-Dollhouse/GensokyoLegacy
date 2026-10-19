package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity.CombatData;
import dev.xkmc.l2serial.serialization.marker.OnInject;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;

/**
 * The doll item's combat component: the doll type comes from the item ({@link DollItem#dollType()})
 * and name and tint have their own components. The cached hash is rebuilt by {@link #onInject()},
 * since l2serial reconstructs through the no-arg constructor and writes the field afterwards.
 *
 * @see "doc/design/doll/item.md"
 */
@SerialClass
public final class DollItemData {

	/**
	 * A freshly made doll: full health, no custom data. Use as the fallback wherever the
	 * component is missing.
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