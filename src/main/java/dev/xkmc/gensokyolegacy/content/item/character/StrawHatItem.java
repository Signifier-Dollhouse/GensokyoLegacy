package dev.xkmc.gensokyolegacy.content.item.character;

import net.minecraft.world.level.Level;
import dev.xkmc.gensokyolegacy.content.client.model.HatModel;
import dev.xkmc.gensokyolegacy.content.client.model.SuwakoHatModel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLCriteriaTriggers;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class StrawHatItem extends TouhouHatItem {

	public StrawHatItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new HatModel(SuwakoHatModel.STRAW));
	}

	@Override
	public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
		return GensokyoLegacy.loc("textures/model/straw_hat.png").toString();
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof Frog frog))
			return InteractionResult.PASS;
		if (!GLMeta.FROG_GOD.type().isProper(frog))
			return InteractionResult.FAIL;
		var cap = GLMeta.FROG_GOD.type().getOrCreate(frog);
		if (cap.hasHat) {
			return InteractionResult.FAIL;
		} else {
			if (player instanceof ServerPlayer sp) {
				cap.hasHat = true;
				cap.syncToClient(frog);
				GLCriteriaTriggers.SUWAKO_WEAR.get().trigger(sp);
			}
			return InteractionResult.SUCCESS;
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.STRAW_HAT_LORE.get());
	}

}
