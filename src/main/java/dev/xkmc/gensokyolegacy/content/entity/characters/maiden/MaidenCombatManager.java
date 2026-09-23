package dev.xkmc.gensokyolegacy.content.entity.characters.maiden;

import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.DefaultCombatManager;
import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.TargetKind;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;

class MaidenCombatManager extends DefaultCombatManager {

	// proactive extermination (FIGHT) while keeping prey heal effects, unlike ENEMY
	private static final TargetKind EXTERMINATE = new TargetKind(true, true, false);

	public MaidenCombatManager(MaidenEntity e) {
		super(e, GeneralYoukaiEntity.SPELL);
	}

	@Override
	public TargetKind targetKind(LivingEntity le) {
		if (le.getType().is(EntityTypeTags.RAIDERS))
			return EXTERMINATE;
		if (le instanceof Mob mob && mob instanceof Enemy && (mob.getTarget() instanceof Villager || mob.getLastHurtMob() instanceof Villager))
			return EXTERMINATE;
		return super.targetKind(le);
	}

}
