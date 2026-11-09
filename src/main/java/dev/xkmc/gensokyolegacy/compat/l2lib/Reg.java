package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.serialization.Codec;
import dev.xkmc.l2serial.serialization.codec.CodecAdaptor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Registration entry point: holds the mod event bus and creates the deferred and datapack
 * registrations the mod uses.
 */
public final class Reg {

	private final String modid;
	private final IEventBus bus;

	public Reg(String modid) {
		this.modid = modid;
		this.bus = FMLJavaModLoadingContext.get().getModEventBus();
	}

	public String modid() {
		return modid;
	}

	public IEventBus bus() {
		return bus;
	}

	public <T> DeferredRegister<T> make(Registry<T> reg) {
		return register(DeferredRegister.create(reg.key(), modid));
	}

	public <T> DeferredRegister<T> make(IForgeRegistry<T> reg) {
		return register(DeferredRegister.create(reg, modid));
	}

	public <T> DeferredRegister<T> make(ResourceKey<? extends Registry<T>> reg) {
		return register(DeferredRegister.create(reg, modid));
	}

	private <T> DeferredRegister<T> register(DeferredRegister<T> ans) {
		ans.register(bus);
		return ans;
	}

	public <T> DatapackReg<T> dataReg(String id, Codec<T> codec) {
		var ans = new DatapackReg<>(ResourceKey.createRegistryKey(loc(id)), codec);
		bus.addListener(ans::onRegister);
		return ans;
	}

	public <T> DatapackReg<T> dataReg(String id, Class<T> cls) {
		return dataReg(id, new CodecAdaptor<>(cls));
	}

	public <K, V> DataMapReg<K, V> dataMap(String id, ResourceKey<? extends Registry<K>> key, Class<V> cls) {
		return new DataMapReg<>(loc(id), key, new CodecAdaptor<>(cls));
	}

	public <K, V> DataMapReg<K, V> dataMap(String id, ResourceKey<? extends Registry<K>> key, Codec<V> codec) {
		return new DataMapReg<>(loc(id), key, codec);
	}

	public ResourceLocation loc(String id) {
		return new ResourceLocation(modid, id);
	}

}
