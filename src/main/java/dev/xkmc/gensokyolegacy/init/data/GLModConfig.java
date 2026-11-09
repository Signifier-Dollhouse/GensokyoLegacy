package dev.xkmc.gensokyolegacy.init.data;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.compat.l2lib.ConfigInit;
import net.minecraftforge.common.ForgeConfigSpec;

public class GLModConfig {

	public static class Common extends ConfigInit {

		public final ForgeConfigSpec.IntValue regionWeight;

		Common(Builder builder) {
			markL2();
			builder.push("worldgen", "World Generation");
			{
				regionWeight = builder.text("TerraBlender region weight for the magical forest region; takes effect on game restart")
						.defineInRange("regionWeight", 2, 0, 100);
			}
			builder.pop();
		}

	}

	public static class Server extends ConfigInit {

		public final ForgeConfigSpec.DoubleValue reputationDecayFloor;

		public final ForgeConfigSpec.DoubleValue visitStayMultiplier;

		public final ForgeConfigSpec.IntValue frogEatCountForHat;
		public final ForgeConfigSpec.IntValue koishiAttackCoolDown;
		public final ForgeConfigSpec.DoubleValue koishiAttackChance;
		public final ForgeConfigSpec.IntValue koishiAttackDamage;
		public final ForgeConfigSpec.IntValue koishiAttackBlockCount;

		public final ForgeConfigSpec.DoubleValue danmakuMinPHPDamage;
		public final ForgeConfigSpec.DoubleValue danmakuPlayerPHPDamage;
		public final ForgeConfigSpec.DoubleValue danmakuHealOnHitTarget;

		public final ForgeConfigSpec.BooleanValue anchorStormDanmaku;

		public final ForgeConfigSpec.DoubleValue morichikaReplaceChance;

		public final ForgeConfigSpec.IntValue curiosDecayInterval;
		public final ForgeConfigSpec.IntValue foodieDecayInterval;
		public final ForgeConfigSpec.IntValue livelyDecayInterval;
		public final ForgeConfigSpec.IntValue curiosSkillCost;
		public final ForgeConfigSpec.IntValue hauntingSkillCost;

		public final ForgeConfigSpec.IntValue sealingPotRadius;
		public final ForgeConfigSpec.DoubleValue jizoXpRatio;

		public final ForgeConfigSpec.IntValue partyScanDistance;
		public final ForgeConfigSpec.IntValue partyGuestRadius;

		public final ForgeConfigSpec.DoubleValue cooldownMaxRate;

		Server(Builder builder) {
			markL2();
			builder.push("reputation", "Reputation");
			{
				reputationDecayFloor = builder.text("Daily reputation decay never reduces reputation below this fraction of the reputation cap")
						.defineInRange("reputationDecayFloor", 0.8, 0, 1);
			}
			builder.pop();

			builder.push("visit", "Character Visit");
			{
				visitStayMultiplier = builder.text("Multiplier on the visit lengths authored in structure_config; 1.0 uses the authored length. Raising it makes guests stay longer, and therefore show up more often")
						.defineInRange("visitStayMultiplier", 1.0, 0.1, 5);
			}
			builder.pop();

			builder.push("blocks", "Blocks");
			{
				sealingPotRadius = builder.text("Radius in chunks of the area around sealing pot that prevents hostile natural mob spawning")
						.defineInRange("sealingPotRadius", 4, 1, 8);
				jizoXpRatio = builder.text("Spiritual power a Jizo restores per experience point when it takes one level and the current progress; 0.1 restores 10%")
						.defineInRange("jizoXpRatio", 2d, 0, 100);
			}
			builder.pop();

			builder.push("party", "Party");
			{
				partyScanDistance = builder.text("How far in blocks from the clicked table a party may reach; the walk covers the connected space, chairs and tables extend it, and nothing beyond this distance counts")
						.defineInRange("partyScanDistance", 32, 4, 64);
				partyGuestRadius = builder.text("How far a party guest may wander from the table she is eating at")
						.defineInRange("partyGuestRadius", 24, 4, 64);
			}
			builder.pop();

			builder.push("cooldown", "Cooldown");
			{
				cooldownMaxRate = builder.text("Upper bound on the cooldown speed, applied after the sources multiply")
						.defineInRange("cooldownMaxRate", 4.0, 1.0, 16.0);
			}
			builder.pop();

			builder.push("suwako_hat", "Suwako Hat");
			{
				frogEatCountForHat = builder.text("Number of raiders with different types frogs need to eat to drop Suwako hat")
						.defineInRange("frogEatCountForHat", 3, 1, 10);
			}
			builder.pop();

			builder.push("koishi_attack", "Koishi Attack");
			{
				koishiAttackCoolDown = builder.text("Time in ticks for minimum time between koishi attacks")
						.defineInRange("koishiAttackCoolDown", 6000, 1, 1000000);
				koishiAttackChance = builder.text("Chance every tick to do koishi attack")
						.defineInRange("koishiAttackChance", 0.001, 0, 1);
				koishiAttackDamage = builder.text("Koishi attack damage")
						.defineInRange("koishiAttackDamage", 100, 0, 100000000);
				koishiAttackBlockCount = builder.text("Number of times player needs to consecutively block Koishi attack to get hat")
						.defineInRange("koishiAttackBlockCount", 3, 0, 100);
			}
			builder.pop();

			builder.push("danmaku_battle", "Danmaku Battle");
			{
				danmakuMinPHPDamage = builder.text("Minimum damage youkai danmaku will deal against non-player")
						.defineInRange("danmakuMinPHPDamage", 0.02, 0, 1);
				danmakuPlayerPHPDamage = builder.text("Minimum damage youkai danmaku will deal against player")
						.defineInRange("danmakuPlayerPHPDamage", 0.1, 0, 1);
				danmakuHealOnHitTarget = builder.text("When danmaku hits target, heal youkai health by percentage of max health")
						.defineInRange("danmakuHealOnHitTarget", 0.2, 0, 1);
			}
			builder.pop();

			builder.push("minamitsu_anchor", "Minamitsu's Anchor");
			{
				anchorStormDanmaku = builder.text("Whether a storm anchor sheds trail danmaku and bursts on impact")
						.define("anchorStormDanmaku", true);
			}
			builder.pop();

			builder.push("morichika", "Morichika");
			{
				morichikaReplaceChance = builder.text("Chance for Morichika to replace a stocked shelf with a new item when no empty shelf is available; otherwise he skips restocking")
						.defineInRange("morichikaReplaceChance", 0.3, 0, 1);
			}
			builder.pop();

			builder.push("ghost", "Ghosts");
			{
				curiosDecayInterval = builder.text("Ticks between power drain pulses for curios ghosts")
						.defineInRange("curiosDecayInterval", 100, 1, 1000000);
				foodieDecayInterval = builder.text("Ticks between power drain pulses for foodie ghosts")
						.defineInRange("foodieDecayInterval", 100, 1, 1000000);
				livelyDecayInterval = builder.text("Ticks between power drain pulses for lively ghosts")
						.defineInRange("livelyDecayInterval", 100, 1, 1000000);
				curiosSkillCost = builder.text("Power a curios ghost spends to cleanse one harmful effect")
						.defineInRange("curiosSkillCost", 10, 0, 1000);
				hauntingSkillCost = builder.text("Power a Ghost Butterfly spends on each butterfly volley")
						.defineInRange("hauntingSkillCost", 10, 0, 1000);
			}
			builder.pop();
		}

	}

	public static final Common COMMON = GensokyoLegacy.REGISTRATE.registerUnsynced(Common::new);

	public static final Server SERVER = GensokyoLegacy.REGISTRATE.registerSynced(Server::new);

	/**
	 * Registers any relevant listeners for config
	 */
	public static void init() {
	}


}
