package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class GLSounds {

	public static final SimpleEntry<SoundEvent> KOISHI_RING = reg("koishi_ring");

	public static final SimpleEntry<SoundEvent> DIALOG_BLIP = reg("dialog_blip");

	/**
	 * Full spoken lines, kept for a per-character dialog voice: two timbre variants of
	 * the same length as {@link #DIALOG_BLIP}, so they slot into the typewriter blip
	 * unchanged once a character picks one. Registered but not played yet.
	 */
	public static final SimpleEntry<SoundEvent> DIALOG_CHAT_YOUNG = reg("dialog_chat_young");

	public static final SimpleEntry<SoundEvent> DIALOG_CHAT_MATURE = reg("dialog_chat_mature");

	/**
	 * The generic strike voice, currently unused: {@link #DOLL_LANCE} and
	 * {@link #DOLL_SLAP} cover the two melee shapes a doll has today, and this stays
	 * as the fallback for any weapon that arms one later.
	 */
	public static final SimpleEntry<SoundEvent> DOLL_ATTACK = reg("doll_attack");

	/** The lance charge's impact. */
	public static final SimpleEntry<SoundEvent> DOLL_LANCE = reg("doll_lance");

	/** The bare-handed charge's impact. */
	public static final SimpleEntry<SoundEvent> DOLL_SLAP = reg("doll_slap");

	private static SimpleEntry<SoundEvent> reg(String id) {
		ResourceLocation rl = GensokyoLegacy.loc(id);
		return new SimpleEntry<>(GensokyoLegacy.REGISTRATE.simple(id, Registries.SOUND_EVENT, () -> SoundEvent.createVariableRangeEvent(rl)));
	}

	public static void register() {
	}

}
