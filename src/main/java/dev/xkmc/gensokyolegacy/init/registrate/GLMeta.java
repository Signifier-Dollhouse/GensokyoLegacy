package dev.xkmc.gensokyolegacy.init.registrate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkAreaAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.area.LevelAreaAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterConfig;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.glove.GloveTargetAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.StructureAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.BedData;
import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.StructureConfig;
import dev.xkmc.gensokyolegacy.content.attachment.misc.FrogGodCapability;
import dev.xkmc.gensokyolegacy.content.attachment.misc.KoishiAttackCapability;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFood;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTaste;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.MorichikaOfferData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftItemData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftPreference;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.core.DialogConfig;
import dev.xkmc.gensokyolegacy.content.rpg.core.GiftDialogData;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.Dialog;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
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
	// the shared glove target cache (glove.md §2): one ray-trace hint per glove, written by
	// the client's trace and read by whichever glove is used
	public static final AttVal.PlayerVal<GloveTargetAttachment> GLOVE_TARGET = ATT.player("glove_target",
			GloveTargetAttachment.class, GloveTargetAttachment::new, PlayerCapabilityNetworkHandler::new);
	public static final AttVal.CapVal<Frog, FrogGodCapability> FROG_GOD = ATT.entity("frog_god",
			FrogGodCapability.class, FrogGodCapability::new, Frog.class, e -> true);

	public static final AttVal.CapVal<Level, LevelAreaAttachment> LEVEL_EFFECT =
			ATT.entity("level_area", LevelAreaAttachment.class, LevelAreaAttachment::new, Level.class, e -> true);

	public static final AttVal.CapVal<LevelChunk, ChunkAreaAttachment> CHUNK_EFFECT =
			ATT.entity("chunk_area", ChunkAreaAttachment.class, ChunkAreaAttachment::new, LevelChunk.class, e -> true);

	public static final DataMapReg<Block, BedData> BED_DATA =
			GensokyoLegacy.REG.dataMap("bed_data", Registries.BLOCK, BedData.class);

	public static final AttVal.CapVal<LevelChunk, PartyAttachment> PARTY =
			ATT.entity("party_data", PartyAttachment.class, PartyAttachment::new, LevelChunk.class, e -> true);
	public static final DataMapReg<Block, PartyFood> PARTY_FOOD =
			GensokyoLegacy.REG.dataMap("party_food", Registries.BLOCK, PartyFood.class);
	public static final DataMapReg<EntityType<?>, PartyTaste> PARTY_TASTE =
			GensokyoLegacy.REG.dataMap("party_taste", Registries.ENTITY_TYPE, PartyTaste.class);
	public static final DataMapReg<EntityType<?>, CharacterConfig> ENTITY_DATA =
			GensokyoLegacy.REG.dataMap("character_config", Registries.ENTITY_TYPE, CharacterConfig.class);
	public static final DataMapReg<EntityType<?>, DialogConfig> DIALOG_DATA =
			GensokyoLegacy.REG.dataMap("default_dialog", Registries.ENTITY_TYPE, DialogConfig.class);
	public static final DataMapReg<Structure, StructureConfig> STRUCTURE_DATA =
			GensokyoLegacy.REG.dataMap(DataMapType.builder(GensokyoLegacy.loc("structure_config"),
					Registries.STRUCTURE, new CodecAdaptor<>(StructureConfig.class)).build());
	public static final DataMapReg<EntityType<?>, GiftPreference> GIFT_PREFERENCE =
			GensokyoLegacy.REG.dataMap("gift_preference", Registries.ENTITY_TYPE, GiftPreference.class);
	public static final DataMapReg<EntityType<?>, GiftDialogData> GIFT_DIALOG_DATA =
			GensokyoLegacy.REG.dataMap("gift_dialog", Registries.ENTITY_TYPE, GiftDialogData.class);
	public static final DataMapReg<Item, GiftItemData> GIFT_DATA =
			GensokyoLegacy.REG.dataMap("gift_data", Registries.ITEM, GiftItemData.class);
	public static final DataMapReg<Item, MorichikaOfferData> MORICHIKA_OFFER =
			GensokyoLegacy.REG.dataMap("morichika_offer", Registries.ITEM, MorichikaOfferData.class);

	public static void register() {
		Handlers.enableVanilla(Fluid.class, BuiltInRegistries.FLUID);
		// so GiftDialogData can name its two dialogs as Holder<Dialog>: l2serial resolves a Holder
		// through the reg its element type is registered under, and Dialog is a datapack registry
		// rather than a built-in one, so the reg has to be declared here
		Handlers.registerReg(Dialog.class, CodecRegistry.DIALOG.key());
		new CodecHandler<>(FluidIngredient.class, FluidIngredient.CODEC, FluidIngredient.STREAM_CODEC);
		new CodecHandler<>(BoundingBox.class, BoundingBox.CODEC, ByteBufCodecs.fromCodecWithRegistries(BoundingBox.CODEC));
		new CodecHandler<>(BlockPos.class, BlockPos.CODEC, ByteBufCodecs.fromCodecWithRegistries(BlockPos.CODEC));
		new CodecHandler<>(BlockState.class, BlockState.CODEC, ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC));
		new CodecHandler<>(ChunkPos.class, RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("x").forGetter(e -> e.x),
				Codec.INT.fieldOf("z").forGetter(e -> e.z)
		).apply(i, ChunkPos::new)), ByteBufCodecs.fromCodecWithRegistries(Codec.LONG).map(ChunkPos::new, ChunkPos::toLong));
	}

}
