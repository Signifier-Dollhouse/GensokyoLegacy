package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import net.minecraft.core.Holder;

import java.util.List;
import java.util.Optional;

public interface OptionResult {

	List<DialogAction<?>> actions();

	Optional<Holder<Dialog>> next();

}
