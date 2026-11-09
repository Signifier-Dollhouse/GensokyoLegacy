package dev.xkmc.gensokyolegacy.content.entity.behavior.module;

import dev.xkmc.gensokyolegacy.content.client.debug.InfoUpdateClientManager;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

@SerialClass
public class FeedModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("feed");

	@SerialField
	private int feedCoolDown;

	public FeedModule(YoukaiEntity self) {
		super(ID, self);
	}

	public int getCoolDown() {
		return feedCoolDown;
	}

	public int getFavor(ItemStack food, FoodProperties prop) {
		int ans = 3 + prop.getNutrition() / 4;
		if (prop.getNutrition() == 0)
			ans = 1;
		boolean good = false;
		for (var e : prop.getEffects()) {
			var effect = e.getFirst().getEffect();
			if (effect == MobEffects.POISON)
				return -1;
			if (effect == MobEffects.WITHER)
				return -1;
			if (effect.isBeneficial())
				good = true;
		}
		if (good)
			ans += ans / 2;
		return Math.min(ans, 10);
	}

	public void onFeed(ItemStack food, FoodProperties prop, Player player, int favor) {
		var data = self.getData(player);
		if (data.isEmpty()) return;
		int heart = data.get().feed(food, favor);
		if (heart > 0) {
			self.level().broadcastEntityEvent(self, EntityEvent.IN_LOVE_HEARTS);
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!self.mayInteract(player)) return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		var food = stack.getFoodProperties(self);
		if (food == null) return InteractionResult.PASS;
		if (feedCoolDown > 0) return InteractionResult.PASS;
		int favor = getFavor(stack, food);
		if (favor < 0) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer)) {
			InfoUpdateClientManager.clearCache();
			return InteractionResult.SUCCESS;
		}
		onFeed(stack, food, player, favor);
		ItemStack remain = stack.getCraftingRemainingItem();
		stack.shrink(1);
		feedCoolDown += food.getNutrition() * 100;
		if (stack.getUseAnimation() == UseAnim.DRINK)
			self.playSound(stack.getDrinkingSound());
		else self.playSound(stack.getEatingSound());
		if (!remain.isEmpty())
			player.getInventory().placeItemBackInInventory(remain);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void tickServer() {
		if (feedCoolDown > 0) feedCoolDown--;
		self.setFlag(YoukaiFlags.FED, feedCoolDown > 0);
	}

	@Override
	public void tickClient() {
		if (!self.getFlag(YoukaiFlags.FED)) return;
		int chance = self.isInvisible() ? 15 : 2;
		if (self.getRandom().nextInt(chance) != 0) return;
		// 1.20.1 has no colored ENTITY_EFFECT: the colour rides in the particle's velocity args
		int color = MobEffects.SATURATION.getColor();
		self.level().addParticle(ParticleTypes.ENTITY_EFFECT,
				self.getRandomX(0.5D), self.getRandomY(), self.getRandomZ(0.5D),
				(color >> 16 & 255) / 255.0, (color >> 8 & 255) / 255.0, (color & 255) / 255.0
		);
	}

	@Override
	public boolean handleEntityEvent(byte pId) {
		if (pId == EntityEvent.IN_LOVE_HEARTS) {
			for (int i = 0; i < 7; ++i) {
				double d0 = self.getRandom().nextGaussian() * 0.02D;
				double d1 = self.getRandom().nextGaussian() * 0.02D;
				double d2 = self.getRandom().nextGaussian() * 0.02D;
				self.level().addParticle(ParticleTypes.HEART,
						self.getRandomX(1.0D), self.getRandomY() + 0.5D, self.getRandomZ(1.0D), d0, d1, d2);
			}
			return true;
		}
		return false;
	}

}
