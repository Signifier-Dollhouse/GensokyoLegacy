package dev.xkmc.gensokyolegacy.compat.l2lib;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Runs queued server-thread tasks once per tick.
 */
@Mod.EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class SchedulerHandler {

	private static List<BooleanSupplier> TASKS = new ArrayList<>();

	public static synchronized void schedule(Runnable runnable) {
		TASKS.add(() -> {
			runnable.run();
			return true;
		});
	}

	public static synchronized void schedulePersistent(BooleanSupplier runnable) {
		TASKS.add(runnable);
	}

	@SubscribeEvent
	public static void serverTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		execute();
	}

	private static synchronized void execute() {
		if (TASKS.isEmpty()) return;
		var temp = TASKS;
		TASKS = new ArrayList<>();
		temp.removeIf(BooleanSupplier::getAsBoolean);
		temp.addAll(TASKS);
		TASKS = temp;
	}

}
