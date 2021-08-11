package com.akon.lives;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class LivesListener extends PacketAdapter implements Listener {

	public LivesListener() {
		super(LivesMain.getInstance(), PacketType.Play.Server.LOGIN);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		LivesAPI.getLives(e.getPlayer()); //残機が設定されていない場合、初期化する
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();
		int lives = LivesAPI.getLives(player);
		if (lives <= 0) {
			player.setGameMode(GameMode.SPECTATOR);
			return;
		}
		LivesAPI.setLives(player, lives-1);
		Bukkit.getScheduler().runTask(LivesMain.getInstance(), player.spigot()::respawn);
	}

	@Override
	public void onPacketSending(PacketEvent e) {
		e.getPacket().getBooleans().write(0, true); //ハードコアモードに見せかける
	}

}
