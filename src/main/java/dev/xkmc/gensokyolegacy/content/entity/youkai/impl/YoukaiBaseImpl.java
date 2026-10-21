package dev.xkmc.gensokyolegacy.content.entity.youkai.impl;

import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Shared base for every youkai facet: the typed self, the flag bitfield and the two lookups
 * every facet reaches for. The entity resolution lives here rather than with any one feature
 * because damage bookkeeping, the relation half and the talk gate all ask the same question.
 * Named {@code asYoukai()} to avoid clashing with {@code DamageClampEntity#self()}.
 *
 * @see "doc/design/youkai/entity.md"
 */
public interface YoukaiBaseImpl {

	default YoukaiEntity asYoukai() {
		return (YoukaiEntity) this;
	}

	/**
	 * The vanilla synced data, redeclared here so defaults can read the accessors.
	 */
	SynchedEntityData getEntityData();

	default boolean getFlag(YoukaiFlags val) {
		return (getEntityData().get(YoukaiEntity.DATA_FLAGS_ID) & (1 << val.ordinal())) != 0;
	}

	/**
	 * The bitfield is an {@code int} and has to stay one: these used to be cast to {@code byte}, silently
	 * dropping any flag past the eighth bit, which made the ninth flag and every one after it unsettable.
	 */
	default void setFlag(YoukaiFlags val, boolean enable) {
		int b0 = getEntityData().get(YoukaiEntity.DATA_FLAGS_ID);
		int flag = 1 << val.ordinal();
		if (enable) {
			b0 |= flag;
		} else {
			b0 &= -1 - flag;
		}
		getEntityData().set(YoukaiEntity.DATA_FLAGS_ID, b0);
	}

	/**
	 * The player behind {@code e}, whether she is one herself or something belongs to her.
	 */
	default Optional<Player> resolvePlayer(@Nullable Entity e) {
		if (e instanceof Player player) {
			return Optional.of(player);
		}
		if (e instanceof OwnableEntity own && asYoukai().level() instanceof ServerLevel sl) {
			var id = own.getOwnerUUID();
			if (id != null && sl.getEntity(id) instanceof Player player) {
				return Optional.of(player);
			}
		}
		return Optional.empty();
	}

	/**
	 * Her standing with whoever {@code e} resolves to, empty if that is nobody.
	 */
	default Optional<CharDataHolder> getData(@Nullable Entity e) {
		return resolvePlayer(e).map(player -> CharDataHolder.get(player, asYoukai()));
	}

}