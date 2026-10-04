package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Renders whatever Cirno holds at her wrist locator bones. A geo renderer draws no
 * items by itself — vanilla {@code MobRenderer} did, so without this she would lose
 * the held item the moment she became geo-rendered. Unlike the dolls, her hands are
 * ordinary vanilla equipment, so the stacks come straight off the entity.
 */
public class CirnoHeldItemLayer extends BlockAndItemGeoLayer<CirnoEntity> {

	public static final String RIGHT_HAND_BONE = "RightHandLocator";
	public static final String LEFT_HAND_BONE = "LeftHandLocator";

	public CirnoHeldItemLayer(GeoRenderer<CirnoEntity> renderer) {
		super(renderer);
	}

	@Override
	@Nullable
	protected ItemStack getStackForBone(GeoBone bone, CirnoEntity cirno) {
		ItemStack stack = switch (bone.getName()) {
			case RIGHT_HAND_BONE -> cirno.getItemBySlot(EquipmentSlot.MAINHAND);
			case LEFT_HAND_BONE -> cirno.getItemBySlot(EquipmentSlot.OFFHAND);
			default -> ItemStack.EMPTY;
		};
		return stack.isEmpty() ? null : stack;
	}

}
