package dev.xkmc.gensokyolegacy.content.entity.module;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The interaction pages, one per character that answers a hand-out, keyed by that character's id
 * rather than by the {@link EntityType} itself: a Registrate entry is still unbound while
 * {@code GLEntities} is declaring it, so the type only exists once the registry has caught up.
 * Registered in declaration order, and built on first use, since a page may name items that are
 * still registering.
 *
 * @see dev.xkmc.gensokyolegacy.compat.jei.EntityInteractionCategory
 */
public final class EntityInteractions {

	private static final Map<ResourceLocation, Supplier<List<EntityInteraction>>> PAGES = new LinkedHashMap<>();

	public static void register(ResourceLocation character, Supplier<List<EntityInteraction>> page) {
		PAGES.put(character, page);
	}

	/** The characters with a page, in declaration order. Asked once JEI is up, by which point the
	 * registry holds them all; an id that never arrived is skipped rather than drawn blank. */
	public static List<EntityType<?>> characters() {
		List<EntityType<?>> out = new ArrayList<>();
		for (var id : PAGES.keySet()) {
			var type = BuiltInRegistries.ENTITY_TYPE.get(id);
			if (type != null) out.add(type);
		}
		return out;
	}

	/** Never empty for a registered character; empty rather than null for anything else, so a
	 * character that has not answered to a hand-out cannot make a blank page. */
	public static List<EntityInteraction> pageOf(EntityType<?> character) {
		var id = BuiltInRegistries.ENTITY_TYPE.getKey(character);
		var page = id == null ? null : PAGES.get(id);
		return page == null ? List.of() : page.get();
	}

	private EntityInteractions() {
	}
}