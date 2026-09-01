package dev.xkmc.gensokyolegacy.content.spell.item;

import dev.xkmc.danmakuapi.content.spell.item.ItemSpell;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.spell.part.ReimuPart;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

@SerialClass
public class ReimuItemSpell extends ItemSpell {

	@Override
	public void start(LivingEntity player, @Nullable LivingEntity target) {
		super.start(player, target);
		addTicker(new ReimuPart<ReimuItemSpell>()
				.setRad(8, 6, 20, 20, 1)
				.setVals(20, 0, 40, 60)
				.setProp(DanmakuItems.Bullet.CIRCLE, DyeColor.RED));
	}
}
