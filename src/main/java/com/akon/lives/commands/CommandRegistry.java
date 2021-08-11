package com.akon.lives.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@UtilityClass
public class CommandRegistry {

	private final ConstructorAccessor PLUGIN_COMMAND_CONSTRUCTOR = Accessors.getConstructorAccessor(PluginCommand.class, String.class, Plugin.class);
	private final Class<?> CHAT_BASE_COMPONENT_CLASS = MinecraftReflection.getIChatBaseComponentClass();
	private final CommandMap COMMAND_MAP = (CommandMap)Accessors.getMethodAccessor(Bukkit.getServer().getClass(), "getCommandMap").invoke(Bukkit.getServer());
	private final CommandDispatcher<CommandSender> DISPATCHER = new CommandDispatcher<>();
	private final CommandDispatcher<Object> NMS_DISPATCHER = new CommandDispatcher<>();
	private final CommandExecutor EXECUTOR = (sender, command, label, args) -> {
		String fullCommand = label + " " + String.join(" ", args);
		try {
			DISPATCHER.execute(fullCommand, sender);
		} catch (CommandSyntaxException ex) {
			sendMessage(sender, new ComponentBuilder()
				.append(toBaseComponents(ex.getRawMessage()))
				.color(ChatColor.RED)
				.create()
			);
			if (ex.getInput() != null && ex.getCursor() >= 0) {
				int i = Math.min(ex.getInput().length(), ex.getCursor());
				ComponentBuilder componentBuilder = new ComponentBuilder("")
					.color(ChatColor.GRAY)
					.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, fullCommand));
				if (i > 10) {
					componentBuilder.append("...");
				}
				componentBuilder.append(ex.getInput().substring(Math.max(0, i - 10), i));
				if (i < ex.getInput().length()) {
					componentBuilder
						.append(ex.getInput().substring(i))
						.color(ChatColor.RED)
						.underlined(true);
				}
				componentBuilder
					.append(new TranslatableComponent("command.context.here"))
					.color(ChatColor.RED)
					.italic(true)
					.underlined(false);
				sendMessage(sender, componentBuilder.create());
			}
		} catch (Exception ex) {
			sendMessage(sender, new ComponentBuilder(new TranslatableComponent("command.failed"))
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage())))
				.create()
			);
		}
		return true;
	};
	private final TabCompleter COMPLETER = (sender, command, alias, args) -> {
		if (sender instanceof Player) {
			return null;
		}
		ArrayList<String> result = Lists.newArrayList();
		suggest(new StringReader(alias + " " + String.join(" ", args)), sender, suggestions -> suggestions.getList().stream()
			.map(Suggestion::getText)
			.forEach(result::add)
		);
		return result;
	};

	public void register(LiteralArgumentBuilder<CommandSender> command, Plugin plugin, @Nullable String permission, @Nullable String description, String... aliases) {
		LiteralCommandNode<CommandSender> literalNode = DISPATCHER.register(command);
		NMS_DISPATCHER.getRoot().addChild(CommandSenderConverter.toNMS(literalNode));
		String prefix = plugin.getName().toLowerCase(Locale.ENGLISH).trim();
		Stream.concat(
			Stream.of(createAlias(prefix + ":" + command.getLiteral(), literalNode)),
			Arrays.stream(aliases)
				.flatMap(alias -> Stream.of(
					createAlias(alias, literalNode),
					createAlias(prefix + ":" + alias, literalNode)
				))
		).forEach(builder -> NMS_DISPATCHER.getRoot().addChild(CommandSenderConverter.toNMS(DISPATCHER.register(builder))));
		PluginCommand pluginCmd = (PluginCommand)PLUGIN_COMMAND_CONSTRUCTOR.invoke(command.getLiteral(), plugin);
		pluginCmd.setPermission(permission);
		pluginCmd.setDescription(description == null ? "" : description);
		pluginCmd.setAliases(Arrays.asList(aliases));
		pluginCmd.setExecutor(EXECUTOR);
		pluginCmd.setTabCompleter(COMPLETER);
		COMMAND_MAP.register(plugin.getName(), pluginCmd);
	}

	private LiteralArgumentBuilder<CommandSender> createAlias(String alias, LiteralCommandNode<CommandSender> literalNode) {
		LiteralArgumentBuilder<CommandSender> builder = LiteralArgumentBuilder.<CommandSender>literal(alias)
			.executes(literalNode.getCommand())
			.requires(literalNode.getRequirement())
			.forward(literalNode.getRedirect(), literalNode.getRedirectModifier(), literalNode.isFork());
		literalNode.getChildren().forEach(builder::then);
		return builder;
	}

	private BaseComponent[] toBaseComponents(Message message) {
		if (message instanceof ComponentMessageWrapper wrapper) {
			return wrapper.components();
		}
		if (CHAT_BASE_COMPONENT_CLASS.isInstance(message)) {
			return ComponentSerializer.parse(WrappedChatComponent.fromHandle(message).getJson());
		}
		return new BaseComponent[]{new TextComponent(message.getString())};
	}

	private void sendMessage(CommandSender sender, BaseComponent[] components) {
		if (sender instanceof ConsoleCommandSender) {
			sender.sendMessage(TextComponent.toPlainText(components));
			return;
		}
		sender.spigot().sendMessage(components);
	}

	private void suggest(StringReader command, CommandSender sender, Consumer<? super Suggestions> action) {
		NMS_DISPATCHER.getCompletionSuggestions(NMS_DISPATCHER.parse(command, CommandSenderConverter.toNMS(sender))).thenAccept(action);
	}

	public static class CommandPacketListener extends PacketAdapter {

		private static final Class<?> COMMANDS_CLASS = MinecraftReflection.getMinecraftClass("commands.CommandDispatcher", "CommandDispatcher");
		private static final MethodAccessor FILL_USABLE_COMMANDS;
		private static final Object NMS_SERVER;
		private static final FieldAccessor VANILLA_COMMANDS;
		private static final FieldAccessor RESOURCES;
		private static final FieldAccessor RESOURCES_COMMANDS;
		private static final MethodAccessor GET_DISPATCHER;

		static {
			FuzzyReflection commandsFuzzy = FuzzyReflection.fromClass(COMMANDS_CLASS, true);
			FILL_USABLE_COMMANDS = Accessors.getMethodAccessor(commandsFuzzy.getMethod(FuzzyMethodContract.newBuilder()
				.parameterExactArray(CommandNode.class, CommandNode.class, MinecraftReflection.getMinecraftClass("commands.CommandListenerWrapper", "CommandListenerWrapper"), Map.class)
				.returnTypeVoid()
				.build()
			));
			NMS_SERVER = Accessors.getMethodAccessor(Bukkit.getServer().getClass(), "getServer").invoke(Bukkit.getServer());
			FuzzyReflection nmsServerFuzzy = FuzzyReflection.fromClass(MinecraftReflection.getMinecraftServerClass());
			VANILLA_COMMANDS = Accessors.getFieldAccessor(nmsServerFuzzy.getFieldByType("vanillaCommandDispatcher", COMMANDS_CLASS));
			Class<?> dataPackResourcesClass = MinecraftReflection.getMinecraftClass("server.DataPackResources", "DataPackResources");
			RESOURCES = Accessors.getFieldAccessor(nmsServerFuzzy.getFieldByType("resources", dataPackResourcesClass));
			RESOURCES_COMMANDS = Accessors.getFieldAccessor(FuzzyReflection.fromClass(dataPackResourcesClass).getFieldByType("commands", COMMANDS_CLASS));
			GET_DISPATCHER = Accessors.getMethodAccessor(commandsFuzzy.getMethod(FuzzyMethodContract.newBuilder()
				.returnTypeExact(CommandDispatcher.class)
				.build()
			));
		}

		public CommandPacketListener(Plugin plugin) {
			super(plugin, PacketType.Play.Server.COMMANDS, PacketType.Play.Client.TAB_COMPLETE);
		}

		@Override
		public void onPacketReceiving(PacketEvent e) {
			StringReader command = new StringReader(e.getPacket().getStrings().read(0));
			if (command.canRead() && command.peek() == '/') {
				command.skip();
			}
			String str = command.getString();
			if (NMS_DISPATCHER.getRoot().getChild(str.substring(command.getCursor(), str.indexOf(' '))) == null) {
				return;
			}
			e.setCancelled(true);
			Runnable runnable = () -> suggest(command, e.getPlayer(), suggestions -> {
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.TAB_COMPLETE);
				packet.getIntegers().write(0, e.getPacket().getIntegers().read(0));
				packet.getSpecificModifier(Suggestions.class).write(0, suggestions);
				try {
					ProtocolLibrary.getProtocolManager().sendServerPacket(e.getPlayer(), packet);
				} catch (InvocationTargetException ex) {
					ex.printStackTrace();
				}
			});
			if (Bukkit.isPrimaryThread()) {
				runnable.run();
				return;
			}
			Bukkit.getScheduler().runTask(this.getPlugin(), runnable);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onPacketSending(PacketEvent e) {
			HashMap<CommandNode<?>, CommandNode<?>> map = Maps.newHashMap();
			RootCommandNode<?> rootNode = new RootCommandNode<>();
			RootCommandNode<Object> commandSourceNode = new RootCommandNode<>();
			Object resourceCommands = RESOURCES_COMMANDS.get(RESOURCES.get(NMS_SERVER));
			Stream.concat(
				((CommandDispatcher<Object>)GET_DISPATCHER.invoke(resourceCommands)).getRoot().getChildren().stream()
					.filter(node -> NMS_DISPATCHER.getRoot().getChild(node.getName()) == null),
				Stream.of(NMS_DISPATCHER, (CommandDispatcher<Object>)GET_DISPATCHER.invoke(VANILLA_COMMANDS.get(NMS_SERVER)))
					.map(CommandDispatcher::getRoot)
					.map(CommandNode::getChildren)
					.flatMap(Collection::stream)
			).forEach(commandSourceNode::addChild);
			map.put(commandSourceNode, rootNode);
			FILL_USABLE_COMMANDS.invoke(resourceCommands, commandSourceNode, rootNode, CommandSenderConverter.toNMS(e.getPlayer()), map);
			e.getPacket().getSpecificModifier(RootCommandNode.class).write(0, rootNode);
		}

	}

}
