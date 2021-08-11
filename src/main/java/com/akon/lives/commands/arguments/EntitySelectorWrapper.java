package com.akon.lives.commands.arguments;

import com.akon.lives.commands.CommandSenderConverter;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EntitySelectorWrapper {

	private static final Class<?> ENTITY_SELECTOR_CLASS = MinecraftReflection.getMinecraftClass("commands.arguments.selector.EntitySelector", "EntitySelector");
	private static final Class<?> COMMAND_LISTENER_WRAPPER_CLASS = MinecraftReflection.getMinecraftClass("commands.CommandListenerWrapper", "CommandListenerWrapper");
	private static final Class<?> NMS_ENTITY_CLASS = MinecraftReflection.getEntityClass();
	private static final Class<?> NMS_PLAYER_CLASS = MinecraftReflection.getEntityPlayerClass();
	private static final MethodAccessor GET_MAX_RESULTS;
	private static final MethodAccessor INCLUDES_ENTITIES;
	private static final MethodAccessor IS_SELF_SELECTOR;
	private static final MethodAccessor IS_WORLD_LIMITED;
	private static final MethodAccessor USES_SELECTOR;
	private static final MethodAccessor FIND_SINGLE_ENTITY;
	private static final MethodAccessor FIND_ENTITIES;
	private static final MethodAccessor FIND_SINGLE_PLAYER;
	private static final MethodAccessor FIND_PLAYERS;

	static {
		FuzzyReflection fuzzy = FuzzyReflection.fromClass(ENTITY_SELECTOR_CLASS);
		GET_MAX_RESULTS = Accessors.getMethodAccessor(fuzzy.getMethod(FuzzyMethodContract.newBuilder()
			.parameterCount(0)
			.returnTypeExact(int.class)
			.build()
		));
		List<Method> methodList1 = fuzzy.getMethodList(FuzzyMethodContract.newBuilder()
			.parameterCount(0)
			.returnTypeExact(boolean.class)
			.build()
		);
		INCLUDES_ENTITIES = Accessors.getMethodAccessor(methodList1.get(0));
		IS_SELF_SELECTOR = Accessors.getMethodAccessor(methodList1.get(1));
		IS_WORLD_LIMITED = Accessors.getMethodAccessor(methodList1.get(2));
		USES_SELECTOR = Accessors.getMethodAccessor(methodList1.get(3));
		FIND_SINGLE_ENTITY = Accessors.getMethodAccessor(fuzzy.getMethod(FuzzyMethodContract.newBuilder()
			.parameterExactArray(COMMAND_LISTENER_WRAPPER_CLASS)
			.returnTypeExact(NMS_ENTITY_CLASS)
			.build()
		));
		List<Method> methodList2 = FuzzyReflection.fromClass(ENTITY_SELECTOR_CLASS).getMethodList(FuzzyMethodContract.newBuilder()
			.parameterExactArray(COMMAND_LISTENER_WRAPPER_CLASS)
			.returnDerivedOf(List.class)
			.build()
		);
		FIND_ENTITIES = Accessors.getMethodAccessor(methodList2.get(0));
		FIND_SINGLE_PLAYER = Accessors.getMethodAccessor(FuzzyReflection.fromClass(ENTITY_SELECTOR_CLASS).getMethod(FuzzyMethodContract.newBuilder()
			.parameterExactArray(COMMAND_LISTENER_WRAPPER_CLASS)
			.returnTypeExact(NMS_PLAYER_CLASS)
			.build()
		));
		FIND_PLAYERS = Accessors.getMethodAccessor(methodList2.get(1));
	}

	private final Object handle;

	public static EntitySelectorWrapper fromHandle(Object handle) {
		return handle == null ? null : new EntitySelectorWrapper(handle);
	}

	public int getMaxResults() {
		return (Integer)GET_MAX_RESULTS.invoke(this.handle);
	}

	public boolean includesEntities() {
		return (Boolean)INCLUDES_ENTITIES.invoke(this.handle);
	}

	public boolean isSelfSelector() {
		return (Boolean)IS_SELF_SELECTOR.invoke(this.handle);
	}

	public boolean isWorldLimited() {
		return (Boolean)IS_WORLD_LIMITED.invoke(this.handle);
	}

	public boolean usesSelector() {
		return (Boolean)USES_SELECTOR.invoke(this.handle);
	}

	public Entity findSingleEntity(CommandSender sender) throws CommandSyntaxException {
		try {
			return (Entity)MinecraftReflection.getBukkitEntity(FIND_SINGLE_ENTITY.invoke(this.handle, CommandSenderConverter.toNMS(sender)));
		} catch (RuntimeException ex) {
			if (ex.getCause() instanceof CommandSyntaxException commandSyntaxException) {
				throw commandSyntaxException;
			}
			throw ex;
		}
	}

	public List<? extends Entity> findEntities(CommandSender sender) throws CommandSyntaxException {
		try {
			return ((List<?>)FIND_ENTITIES.invoke(this.handle, CommandSenderConverter.toNMS(sender))).stream()
				.map(nmsEntity -> (Entity)MinecraftReflection.getBukkitEntity(nmsEntity))
				.collect(Collectors.toList());
		} catch (RuntimeException ex) {
			if (ex.getCause() instanceof CommandSyntaxException commandSyntaxException) {
				throw commandSyntaxException;
			}
			throw ex;
		}
	}

	public Player findSinglePlayer(CommandSender sender) throws CommandSyntaxException {
		try {
			return (Player)MinecraftReflection.getBukkitEntity(FIND_SINGLE_PLAYER.invoke(this.handle, CommandSenderConverter.toNMS(sender)));
		} catch (RuntimeException ex) {
			if (ex.getCause() instanceof CommandSyntaxException commandSyntaxException) {
				throw commandSyntaxException;
			}
			throw ex;
		}
	}

	public List<Player> findPlayers(CommandSender sender) throws CommandSyntaxException {
		try {
			return ((List<?>)FIND_PLAYERS.invoke(this.handle, CommandSenderConverter.toNMS(sender))).stream()
				.map(nmsEntity -> (Player)MinecraftReflection.getBukkitEntity(nmsEntity))
				.collect(Collectors.toList());
		} catch (RuntimeException ex) {
			if (ex.getCause() instanceof CommandSyntaxException commandSyntaxException) {
				throw commandSyntaxException;
			}
			throw ex;
		}
	}

}
