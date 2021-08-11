package com.akon.lives;

import com.akon.lives.commands.CommandRegistry;
import com.akon.lives.commands.SetLivesCommand;
import com.comphenix.protocol.ProtocolLibrary;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

//メインクラスにこの処理を書くと、ProtocolLibが最初から導入されていなかった場合エラーが発生するので、別のクラスに記述する
@UtilityClass
public class Registration {

	void register(LivesMain plugin) {
		LivesListener listener = new LivesListener();
		Bukkit.getPluginManager().registerEvents(listener, plugin);
		ProtocolLibrary.getProtocolManager().addPacketListener(listener);
		ProtocolLibrary.getProtocolManager().addPacketListener(new CommandRegistry.CommandPacketListener(plugin));
		SetLivesCommand.register(plugin);
	}

	void unregister(LivesMain plugin) {
		ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
	}

}
