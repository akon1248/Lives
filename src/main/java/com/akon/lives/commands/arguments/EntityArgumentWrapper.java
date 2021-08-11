package com.akon.lives.commands.arguments;

import com.akon.lives.commands.ComponentMessageWrapper;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;

@UtilityClass
public class EntityArgumentWrapper {

	public final SimpleCommandExceptionType NO_ENTITIES_FOUND = new SimpleCommandExceptionType(new ComponentMessageWrapper(new TranslatableComponent("argument.entity.notfound.entity")));
	public final SimpleCommandExceptionType NO_PLAYERS_FOUND = new SimpleCommandExceptionType(new ComponentMessageWrapper(new TranslatableComponent("argument.entity.notfound.player")));
	private final Class<?> ENTITY_SELECTOR_CLASS = MinecraftReflection.getMinecraftClass("commands.arguments.selector.EntitySelector", "EntitySelector");
	private final ConstructorAccessor ARGUMENT_ENTITY_CONSTRUCTOR = Accessors.getConstructorAccessor(MinecraftReflection.getMinecraftClass("commands.arguments.ArgumentEntity", "ArgumentEntity"), boolean.class, boolean.class);

	@SuppressWarnings("unchecked")
	private ArgumentType<Object> createEntityArgument(boolean single, boolean playersOnly) {
		return (ArgumentType<Object>)ARGUMENT_ENTITY_CONSTRUCTOR.invoke(single, playersOnly);
	}

	private EntitySelectorWrapper getEntitySelector(CommandContext<CommandSender> context, String name) {
		return EntitySelectorWrapper.fromHandle(context.getArgument(name, ENTITY_SELECTOR_CLASS));
	}

	public ArgumentType<Object> entity() {
		return createEntityArgument(true, false);
	}

	public Entity getEntity(CommandContext<CommandSender> context, String name) throws CommandSyntaxException {
		return getEntitySelector(context, name).findSingleEntity(context.getSource());
	}

	public ArgumentType<Object> entities() {
		return createEntityArgument(false, false);
	}

	public Collection<? extends Entity> getEntities(CommandContext<CommandSender> context, String name) throws CommandSyntaxException {
		Collection<? extends Entity> entities = getOptionalEntities(context, name);
		if (entities.isEmpty()) {
			throw NO_ENTITIES_FOUND.create();
		}
		return entities;
	}

	public Collection<? extends Entity> getOptionalEntities(CommandContext<CommandSender> context, String name) throws CommandSyntaxException {
		return getEntitySelector(context, name).findEntities(context.getSource());
	}

	public Collection<Player> getOptionalPlayers(CommandContext<CommandSender> context, String name)  throws CommandSyntaxException {
		return getEntitySelector(context, name).findPlayers(context.getSource());
	}

	public ArgumentType<Object> player() {
		return createEntityArgument(true, true);
	}

	public Player getPlayer(CommandContext<CommandSender> context, String name) throws CommandSyntaxException {
		return getEntitySelector(context, name).findSinglePlayer(context.getSource());
	}

	public ArgumentType<Object> players() {
		return createEntityArgument(false, true);
	}

	public Collection<Player> getPlayers(CommandContext<CommandSender> context, String name) throws CommandSyntaxException {
		Collection<Player> entities = getOptionalPlayers(context, name);
		if (entities.isEmpty()) {
			throw NO_PLAYERS_FOUND.create();
		}
		return entities;
	}

}
