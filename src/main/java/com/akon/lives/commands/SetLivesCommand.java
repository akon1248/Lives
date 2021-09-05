package com.akon.lives.commands;

import com.akon.lives.LivesAPI;
import com.akon.lives.LivesMain;
import com.akon.lives.commands.arguments.EntityArgumentWrapper;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;

@UtilityClass
public class SetLivesCommand {

	private final SimpleCommandExceptionType REQUIRES_PLAYER = new SimpleCommandExceptionType(new ComponentMessageWrapper(new TranslatableComponent("permissions.requires.player")));

	public void register(LivesMain plugin) {
		CommandRegistry.register(LiteralArgumentBuilder.<CommandSender>literal("setlives")
			.then(RequiredArgumentBuilder.<CommandSender, Integer>argument("lives", IntegerArgumentType.integer(0))
				.then(RequiredArgumentBuilder.<CommandSender, Object>argument("player", EntityArgumentWrapper.players())
					.executes(context -> setLives(context, EntityArgumentWrapper.getPlayers(context, "player"), IntegerArgumentType.getInteger(context, "lives")))
				)
				.executes(context -> {
					if (context.getSource() instanceof Player player) {
						return setLives(context, Collections.singleton(player), IntegerArgumentType.getInteger(context, "lives"));
					}
					throw REQUIRES_PLAYER.create();
				})
			), plugin, "lives.command.setlives", "プレイヤーの残機を設定します");
	}

	private int setLives(CommandContext<CommandSender> sender, Collection<Player> players, int lives) {
		players.forEach(player -> {
			LivesAPI.setLives(player, lives);
			sender.getSource().sendMessage(player.getName() + "の残機を" + lives + "に設定しました");
		});
		return 0;
	}

}
