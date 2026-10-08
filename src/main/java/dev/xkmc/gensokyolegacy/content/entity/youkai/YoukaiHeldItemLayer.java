package dev.xkmc.gensokyolegacy.content.entity.youkai;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Renders whatever a youkai holds at her wrist locator bones. A geo renderer draws no
 * items by itself — vanilla {@code MobRenderer} did, so without this a character would
 * lose the held item the moment she became geo-rendered. Unlike the dolls, their hands
 * are ordinary vanilla equipment, so the stacks come straight off the entity.
 * <p>
 * Every character rig in this mod names the two wrist bones the same way (Blockbench
 * exports them as {@code RightHandLocator}/{@code LeftHandLocator}), so one layer serves
 * all of them.
 */
public class YoukaiHeldItemLayer<T extends GeneralYoukaiEntity & GeoYoukaiAnim> extends BlockAndItemGeoLayer<T> {

	public static final String RIGHT_HAND_BONE = "RightHandLocator";
	public static final String LEFT_HAND_BONE = "LeftHandLocator";

	public YoukaiHeldItemLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Override
	@Nullable
	protected ItemStack getStackForBone(GeoBone bone, T entity) {
		ItemStack stack = switch (bone.getName()) {
			case RIGHT_HAND_BONE -> entity.getItemBySlot(EquipmentSlot.MAINHAND);
			case LEFT_HAND_BONE -> entity.getItemBySlot(EquipmentSlot.OFFHAND);
			default -> ItemStack.EMPTY;
		};
		return stack.isEmpty() ? null : stack;
	}

}