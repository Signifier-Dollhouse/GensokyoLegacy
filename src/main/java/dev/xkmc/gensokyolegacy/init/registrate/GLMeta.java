package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkAreaAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.area.LevelAreaAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.BedData;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.CharacterConfig;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.DialogConfig;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureConfig;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.StructureAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.misc.FrogGodCapability;
import dev.xkmc.gensokyolegacy.content.attachment.misc.KoishiAttackCapability;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.MorichikaOfferData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftItemData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftPreference;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestAttachment;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeAttachment;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.capability.player.PlayerCapabilityNetworkHandler;
import dev.xkmc.l2core.init.reg.datapack.DataMapReg;
import dev.xkmc.l2core.init.reg.simple.AttReg;
import dev.xkmc.l2core.init.reg.simple.AttVal;
import dev.xkmc.l2serial.serialization.codec.CodecAdaptor;
import dev.xkmc.l2serial.serialization.custom_handler.CodecHandler;
import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public class GLMeta {

	private static final AttReg ATT = AttReg.of(GensokyoLegacy.REG);

	public static final AttVal.PlayerVal<CharacterAttachment> CHAR = ATT.player("character_data",
			CharacterAttachment.class, CharacterAttachment::new, PlayerCapabilityNetworkHandler::new);
	public static final AttVal.PlayerVal<QuestAttachment> QUEST = ATT.player("quest_data",
			QuestAttachment.class, QuestAttachment::new, PlayerCapabilityNetworkHandler::new);
	public static final AttVal.PlayerVal<TradeAttachment> TRADE = ATT.player("trade_data",
			TradeAttachment.class, TradeAttachment::new, PlayerCapabilityNetworkHandler::new);
	public static final AttVal.CapVal<LevelChunk, StructureAttachment> STRUCTURE = ATT.entity("structure_data",
			StructureAttachment.class, StructureAttachment::new, LevelChunk.class, e -> true);
	public static final AttVal.PlayerVal<KoishiAttackCapability> KOISHI_ATTACK = ATT.player("koishi_attack",
			KoishiAttackCapability.class, KoishiAttackCapability::new, PlayerCapabilityNetworkHandler::new);
	public static final AttVal.PlayerVal<DollAttachment> DOLL = ATT.player("doll_data",
			DollAttachment.class, DollAttachment::new, PlayerCapabilityNetworkHandler::new);
	public static final AttVal.CapVal<Frog, FrogGodCapability> FROG_GOD = ATT.entity("frog_god",
			FrogGodCapability.class, FrogGodCapability::new, Frog.class, e -> true);

	public static final AttVal.CapVal<Level, LevelAreaAttachment> LEVEL_EFFECT =
			ATT.entity("level_area", LevelAreaAttachment.class, LevelAreaAttachment::new, Level.class, e -> true);

	public static final AttVal.CapVal<LevelChunk, ChunkAreaAttachment> CHUNK_EFFECT =
			ATT.entity("chunk_area", ChunkAreaAttachment.class, ChunkAreaAttachment::new, LevelChunk.class, e -> true);

	public static final DataMapReg<Block, BedData> BED_DATA =
			GensokyoLegacy.REG.dataMap("bed_data", Registries.BLOCK, BedData.class);
	public static final DataMapReg<EntityType<?>, CharacterConfig> ENTITY_DATA =
			GensokyoLegacy.REG.dataMap("character_config", Registries.ENTITY_TYPE, CharacterConfig.class);
	public static final DataMapReg<EntityType<?>, DialogConfig> DIALOG_DATA =
			GensokyoLegacy.REG.dataMap("default_dialog", Registries.ENTITY_TYPE, DialogConfig.class);
	public static final DataMapReg<Structure, StructureConfig> STRUCTURE_DATA =
			GensokyoLegacy.REG.dataMap(DataMapType.builder(GensokyoLegacy.loc("structure_config"),
					Registries.STRUCTURE, new CodecAdaptor<>(StructureConfig.class)).build());
	public static final DataMapReg<EntityType<?>, GiftPreference> GIFT_PREFERENCE =
			GensokyoLegacy.REG.dataMap("gift_preference", Registries.ENTITY_TYPE, GiftPreference.class);
	public static final DataMapReg<Item, GiftItemData> GIFT_DATA =
			GensokyoLegacy.REG.dataMap("gift_data", Registries.ITEM, GiftItemData.class);
	public static final DataMapReg<Item, MorichikaOfferData> MORICHIKA_OFFER =
			GensokyoLegacy.REG.dataMap("morichika_offer", Registries.ITEM, MorichikaOfferData.class);

	public static void register() {
		Handlers.enableVanilla(Fluid.class, BuiltInRegistries.FLUID);
		new CodecHandler<>(FluidIngredient.class, FluidIngredient.CODEC, FluidIngredient.STREAM_CODEC);
		new CodecHandler<>(BoundingBox.class, BoundingBox.CODEC, ByteBufCodecs.fromCodecWithRegistries(BoundingBox.CODEC));
		new CodecHandler<>(BlockState.class, BlockState.CODEC, ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC));
	}

}
