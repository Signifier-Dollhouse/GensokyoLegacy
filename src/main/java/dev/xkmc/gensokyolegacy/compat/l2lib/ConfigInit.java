package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import dev.xkmc.l2library.base.L2Registrate;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A config body that carries its own language keys. Backport of the 1.21 helper.
 */
public class ConfigInit {

	private static boolean titleAdded = false;

	private String folder = null;
	private String path = "";

	public ModConfig.Type getType() {
		return type;
	}

	private ModConfig.Type type;
	private ForgeConfigSpec spec;

	public String getPath() {
		return path;
	}

	protected void folder(String str) {
		if (folder != null) throw new IllegalStateException("folder already set to " + folder);
		this.folder = str;
	}

	protected void markL2() {
		folder("l2configs/");
	}

	protected void markPlain() {
		folder("");
	}

	public static <T extends ConfigInit> T register(L2Registrate reg, ModConfig.Type type, Function<Builder, T> factory) {
		var builder = new Builder(reg);
		var ans = factory.apply(builder);
		var spec = builder.build();
		register(reg, type, spec, ans);
		return ans;
	}

	private static void register(L2Registrate reg, ModConfig.Type type, ForgeConfigSpec spec, ConfigInit val) {
		if (val.folder == null) throw new IllegalStateException("Config must specify folder");
		String path = val.folder + reg.getModid() + "-" + type.extension() + ".toml";
		ModLoadingContext.get().registerConfig(type, spec, path);
		val.path = path;
		val.type = type;
		val.spec = spec;
		if (!titleAdded) {
			titleAdded = true;
			reg.addRawLang(reg.getModid() + ".configuration.title", "Configuration");
		}
	}

	public static class Builder extends ForgeConfigSpec.Builder {

		private final L2Registrate reg;

		@Nullable
		private String text = null;

		Builder(L2Registrate reg) {
			this.reg = reg;
		}

		public Builder push(String path, String name) {
			reg.addRawLang(reg.getModid() + ".configuration." + path, name);
			super.push(path);
			return this;
		}

		public Builder text(String text) {
			this.text = text;
			return this;
		}

		@Override
		public <T> ForgeConfigSpec.ConfigValue<T> define(List<String> path, ForgeConfigSpec.ValueSpec value,
														 Supplier<T> defaultSupplier) {
			if (text == null) throw new IllegalStateException("text not specified");
			reg.addRawLang(reg.getModid() + ".configuration." + path.get(path.size() - 1), text);
			String comment = value.getComment();
			reg.addRawLang(reg.getModid() + ".configuration." + path.get(path.size() - 1) + ".tooltip",
					comment == null ? "" : comment);
			return super.define(path, value, defaultSupplier);
		}

		@Override
		public Builder comment(String comment) {
			super.comment(comment);
			return this;
		}

	}

}
