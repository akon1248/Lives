package com.akon.lives;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.dependency.SoftDependency;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.author.Author;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Map;

@org.bukkit.plugin.java.annotation.plugin.Plugin(name = "Lives", version = "1.0")
@Description("残機の要素を追加します")
@Author("akon")
@SoftDependency("ProtocolLib")
@Permissions({@Permission(name = "lives.command.setlives")})
public class LivesMain extends JavaPlugin {

	@Getter
	private static LivesMain instance;
	private boolean disable;

	@SuppressWarnings("unchecked")
	@Override
	public void onLoad() {
		instance = this;
		PluginManager pluginManager = Bukkit.getPluginManager();
		if (pluginManager.getPlugin("ProtocolLib") != null) {
			return;
		}
		//ProtocolLibが導入されていなかった場合、ダウンロードしてきて読み込む
		File jar = new File(this.getDataFolder().getParent(), "ProtocolLib.jar");
		try (BufferedInputStream in = new BufferedInputStream(new URL("https://repo.dmulloy2.net/nexus/repository/public/com/comphenix/protocol/ProtocolLib/4.7.0/ProtocolLib-4.7.0.jar").openStream());
		     BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(jar))) {
			byte[] buf = new byte[4096];
			int len;
			while ((len = in.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
			out.flush();
			Plugin protocolLib = this.getPluginLoader().loadPlugin(jar);
			Field pluginsField = SimplePluginManager.class.getDeclaredField("plugins");
			Field lookupNamesField = SimplePluginManager.class.getDeclaredField("lookupNames");
			pluginsField.setAccessible(true);
			lookupNamesField.setAccessible(true);
			((List<Plugin>)pluginsField.get(pluginManager)).add(protocolLib);
			Map<String, Plugin> lookupNames = (Map<String, Plugin>)lookupNamesField.get(pluginManager);
			lookupNames.put(protocolLib.getName(), protocolLib);
			protocolLib.getDescription().getProvides().forEach(provided -> lookupNames.putIfAbsent(provided, protocolLib));
			protocolLib.getLogger().info("Loading " + protocolLib);
			protocolLib.onLoad();
			pluginManager.enablePlugin(protocolLib);
		} catch (IOException | InvalidPluginException | ReflectiveOperationException ex) {
			this.getLogger().severe("ProtocolLibの" + (ex instanceof IOException ? "ダウンロード" : "読み込み") + "に失敗したためプラグインを無効化します");
			disable = true;
			ex.printStackTrace();
		}
	}

	@Override
	public void onEnable() {
		if (disable) {
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		this.saveDefaultConfig();
		Registration.register(this);
	}

	@Override
	public void onDisable() {
		Registration.unregister(this);
	}

}
