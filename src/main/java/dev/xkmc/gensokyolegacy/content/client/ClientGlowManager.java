package dev.xkmc.gensokyolegacy.content.client;

import dev.xkmc.gensokyolegacy.content.item.character.StrangeGlassesItem;
import dev.xkmc.gensokyolegacy.content.item.glove.client.GloveDollHover;
import dev.xkmc.gensokyolegacy.content.item.glove.client.GloveTargetCache;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Central registry for client-side entity glow effects (read by the single
 * entity glow mixin; {@code LevelRenderer} uses {@code getTeamColor} for the
 * outline). Rules are checked in registration order: the first non-null
 * color wins, so specific highlights (glove targets) must register before
 * blanket ones (strange glasses white-out).
 */
public final class ClientGlowManager {

	public record GlowRule(Predicate<Entity> glow, Function<Entity, Integer> color) {
	}

	private static final List<GlowRule> RULES = new ArrayList<>();

	private static boolean ready;

	private ClientGlowManager() {
	}

	public static void register(Predicate<Entity> glow, Function<Entity, Integer> color) {
		RULES.add(new GlowRule(glow, color));
	}

	public static boolean shouldGlow(Entity entity) {
		ensureInit();
		for (var rule : RULES) {
			if (rule.glow().test(entity)) return true;
		}
		return false;
	}

	@Nullable
	public static Integer glowColor(Entity entity) {
		ensureInit();
		for (var rule : RULES) {
			Integer color = rule.color().apply(entity);
			if (color != null) return color;
		}
		return null;
	}

	private static void ensureInit() {
		if (ready) return;
		ready = true;
		// glove target highlight (glove.md §2–2b): the doll under the vanilla
		// crosshair glows gold via GloveDollHover, while the client-cached
		// ray-trace target glows in the held mode's color.
		register(GloveDollHover::isHovered, GloveDollHover::hoverColor);
		register(GloveTargetCache::isMarked, GloveTargetCache::hoverColor);
		// strange glasses: everything glows white while the viewer wears them.
		register(ClientGlowManager::glassesGlow, ClientGlowManager::glassesColor);
	}

	private static boolean glassesGlow(Entity entity) {
		if (!entity.level().isClientSide()) return false;
		var player = Minecraft.getInstance().player;
		return player != null && StrangeGlassesItem.isWearing(player);
	}

	@Nullable
	private static Integer glassesColor(Entity entity) {
		return glassesGlow(entity) ? 0xFFFFFF : null;
	}

}
