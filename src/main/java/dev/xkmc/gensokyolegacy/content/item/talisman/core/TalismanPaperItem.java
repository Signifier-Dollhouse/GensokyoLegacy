package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public abstract class TalismanPaperItem extends Item {

	protected final int durability, color;
	protected final GLLang.LangEntry name;

	public TalismanPaperItem(Properties p, int durability, int color, GLLang.LangEntry name) {
		super(p);
		this.durability = durability;
		this.color = color;
		this.name = name;
	}

	public int getDurability() {
		return durability;
	}

	public int getColor() {
		return color;
	}

	public final void tickTalisman(TalismanContext ctx) {
		if (ctx.isOnCooldown())
			return;
		if (!test(ctx.target()))
			return;
		trigger(ctx);
	}

	public boolean test(LivingEntity le) {
		return false;
	}

	public void trigger(TalismanContext ctx) {

	}

	public boolean onAttacked(TalismanContext ctx, DamageData.Attack event) {
		return false;
	}

	public void onDamaged(TalismanContext ctx, DamageData.Defence event) {

	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		appendTalismanDesc(stack, list);
	}

	protected void appendTalismanDesc(ItemStack stack, List<Component> list) {

	}

	protected void applyEffect(TalismanContext ctx, Holder<MobEffect> eff, int amp) {
		LivingEntity le = ctx.target();
		var old = le.getEffect(eff);
		if (old == null || old.getAmplifier() != amp || old.getDuration() <= 20) {
			le.addEffect(new MobEffectInstance(eff, 39, amp, true, false, true));
			ctx.hurtItem();
		}
	}

	public static int color(ItemStack stack, int tintIndex) {
		return tintIndex == 1 ? stack.getItem() instanceof TalismanPaperItem item ? item.color : -1 : -1;
	}

}