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
import dev.xkmc.gensokyolegacy.content.attachment.misc.ChunkMiscDataAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.misc.FrogGodCapability;
import dev.xkmc.gensokyolegacy.content.attachment.misc.KoishiAttackCapability;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFood;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTaste;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.MorichikaOfferData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftItemData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftPreference;
import dev.xkmc.gensokyolegacy.content.rpg.core.DialogConfig;
import dev.xkmc.gensokyolegacy.content.rpg.core.GiftDialogData;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestAttachment;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeAttachment;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2library.capability.player.PlayerCapabilityNetworkHandler;
import dev.xkmc.gensokyolegacy.compat.l2lib.DataMapReg;
import dev.xkmc.gensokyolegacy.compat.l2lib.AttReg;
import dev.xkmc.gensokyolegacy.compat.l2lib.AttVal;
import dev.xkmc.gensokyolegacy.compat.l2lib.FluidIngredient;
import dev.xkmc.l2serial.serialization.custom_handler.CodecHandler;
import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.BiConsumer;
import java.util.function.Function;

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

	public static final AttVal.CapVal<LevelChunk, ChunkMiscDataAttachment> CHUNK_MISC =
			ATT.entity("chunk_misc", ChunkMiscDataAttachment.class, ChunkMiscDataAttachment::new, LevelChunk.class, e -> true);

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
			GensokyoLegacy.REG.dataMap("structure_config", Registries.STRUCTURE, StructureConfig.class);
	public static final DataMapReg<EntityType<?>, GiftPreference> GIFT_PREFERENCE =
			GensokyoLegacy.REG.dataMap("gift_preference", Registries.ENTITY_TYPE, GiftPreference.class);
	public static final DataMapReg<EntityType<?>, GiftDialogData> GIFT_DIALOG_DATA =
			GensokyoLegacy.REG.dataMap("gift_dialog", Registries.ENTITY_TYPE, GiftDialogData.class);
	public static final DataMapReg<Item, GiftItemData> GIFT_DATA =
			GensokyoLegacy.REG.dataMap("gift_data", Registries.ITEM, GiftItemData.class);
	public static final DataMapReg<Item, MorichikaOfferData> MORICHIKA_OFFER =
			GensokyoLegacy.REG.dataMap("morichika_offer", Registries.ITEM, MorichikaOfferData.class);

	public static void register() {
		Handlers.enableVanilla(Fluid.class, () -> ForgeRegistries.FLUIDS);
		register(BoundingBox.class, BoundingBox.CODEC);
		register(BlockPos.class, BlockPos.CODEC);
		register(BlockState.class, BlockState.CODEC);
		register(ChunkPos.class, RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("x").forGetter(e -> e.x),
				Codec.INT.fieldOf("z").forGetter(e -> e.z)
		).apply(i, ChunkPos::new)));
		register(FluidIngredient.class, FluidIngredient.CODEC);
	}

	private static <T> void register(Class<T> cls, Codec<T> codec) {
		new CodecHandler<>(cls, codec, readNbt(codec), writeNbt(codec));
	}

	private static <T> Function<FriendlyByteBuf, T> readNbt(Codec<T> codec) {
		return buf -> {
			CompoundTag tag = buf.readNbt();
			return codec.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
		};
	}

	private static <T> BiConsumer<FriendlyByteBuf, T> writeNbt(Codec<T> codec) {
		return (buf, val) -> buf.writeNbt((CompoundTag) codec.encodeStart(NbtOps.INSTANCE, val).result().orElseThrow());
	}

}
