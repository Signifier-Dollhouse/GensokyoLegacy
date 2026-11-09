package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class CentiPickaxe extends PickaxeItem {

	public CentiPickaxe(Properties p) {
		super(Tiers.GOLD, 1, -2.8F, p);
	}

	@Override
	public ItemStack getDefaultInstance() {
		ItemStack ans = super.getDefaultInstance();
		ans.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
		ans.enchant(Enchantments.BLOCK_FORTUNE, 3);
		ans.enchant(Enchantments.MENDING, 1);
		return ans;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		if (entity instanceof LivingEntity le) {
			if (le.getMainHandItem() == stack || le.getOffhandItem() == stack) {
				if (le.tickCount % 20 == 4) {
					le.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, true, false, true), le);
				}
			}
		}
	}

	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<T> onBroken) {
		int dmg = stack.getDamageValue();
		int max = stack.getMaxDamage();
		int rem = max - dmg;
		if (rem <= amount && entity != null) {
			var hp = entity.getHealth();
			if (hp > amount) {
				entity.setHealth(hp - amount);
				if (entity.getHealth() == hp - amount) {
					return 0;
				}
				return amount - (int) (hp - entity.getHealth());
			}
		}
		return super.damageItem(stack, amount, entity, onBroken);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.CENTIPICKAXE_LORE.get());
	}

}
