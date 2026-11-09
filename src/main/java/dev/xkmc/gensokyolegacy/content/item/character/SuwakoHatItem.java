package dev.xkmc.gensokyolegacy.content.item.character;

import dev.xkmc.gensokyolegacy.content.client.model.HatModel;
import dev.xkmc.gensokyolegacy.content.client.model.SuwakoHatModel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.l2library.base.effects.EffectUtil;
import dev.xkmc.l2library.util.math.MathHelper;
import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class SuwakoHatItem extends TouhouHatItem {

	public SuwakoHatItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new HatModel(SuwakoHatModel.SUWAKO));
	}

	@Override
	protected void addModifiers(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder) {
		builder.put(L2DamageTracker.MAGIC_FACTOR.get(), new AttributeModifier(
				MathHelper.getUUIDFromString("gensokyolegacy:suwako_hat"), "suwako_hat", 0.25,
				AttributeModifier.Operation.ADDITION));
	}

	@Override
	public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
		return GensokyoLegacy.loc("textures/model/suwako_hat.png").toString();
	}

	@Override
	protected void tick(ItemStack stack, Level level, Player player) {
		EffectUtil.refreshEffect(player, new MobEffectInstance(GLEffects.NATIVE.get(), 40, 0,
				true, true), EffectUtil.AddReason.SELF, player);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.SUWAKO_HAT_LORE.get());
	}

	@Override
	public boolean support(DyeColor color) {
		return color == DyeColor.CYAN || color == DyeColor.LIME;
	}


}
