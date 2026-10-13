package dev.xkmc.gensokyolegacy.content.entity.characters.maiden;

import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.YoukaiCombatManager;
import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

@SerialClass
public class MaidenEntity extends BossYoukaiEntity {

	public MaidenEntity(EntityType<? extends MaidenEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
		navCtrl.markHuman();
	}

	/**
	 * She is the one character tasked with holding her own home, so the house is where
	 * she does her fighting rather than only somewhere she waits for a target to be
	 * handed to her: the target-acquire task searches here as well as on her idle and
	 * play time.
	 */
	@Override
	public boolean shouldSearchTarget(Activity activity) {
		return super.shouldSearchTarget(activity) || activity == GLBrains.AT_HOME.get();
	}

	@Override
	protected YoukaiCombatManager createCombatManager() {
		return new MaidenCombatManager(this);
	}

}
