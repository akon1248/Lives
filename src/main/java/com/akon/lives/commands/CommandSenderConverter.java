package com.akon.lives.commands;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import lombok.experimental.UtilityClass;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@UtilityClass
public class CommandSenderConverter {

	private final MethodAccessor GET_LISTENER = Accessors.getMethodAccessor(MinecraftReflection.getCraftBukkitClass("command.VanillaCommandWrapper"), "getListener", CommandSender.class);
	private final Class<?> COMMAND_LISTENER_WRAPPER_CLASS = MinecraftReflection.getMinecraftClass("commands.CommandListenerWrapper", "CommandListenerWrapper");
	private final MethodAccessor GET_BUKKIT_SENDER = Accessors.getMethodAccessor(COMMAND_LISTENER_WRAPPER_CLASS, "getBukkitSender");
	private final FieldAccessor COMMAND_CONTEXT_ARGUMENTS = Accessors.getFieldAccessor(CommandContext.class, "arguments", true);

	public Object toNMS(CommandSender sender) {
		return sender == null ? null : GET_LISTENER.invoke(null, sender);
	}

	public CommandSender toBukkit(Object source) {
		return source == null ? null : (CommandSender)GET_BUKKIT_SENDER.invoke(source);
	}

	@SuppressWarnings("unchecked")
	private <T, R> CommandContext<R> convertContext(CommandContext<T> context, Function<T, R> tToR, Function<CommandContext<T>, CommandContext<R>> tContextToRContext, Function<CommandContext<R>, CommandContext<T>> rContextToTContext, Function<CommandNode<T>, CommandNode<R>> tNodeToRNode) {
		if (context == null) {
			return null;
		}
		R source = tToR.apply(context.getSource());
		String input = context.getInput();
		Map<String, ParsedArgument<R, ?>> arguments = (Map<String, ParsedArgument<R, ?>>)COMMAND_CONTEXT_ARGUMENTS.get(context);
		Command<R> command = context.getCommand() == null ? null : c -> context.getCommand().run(rContextToTContext.apply(c));
		CommandNode<R> rootNode = tNodeToRNode.apply(context.getRootNode());
		List<ParsedCommandNode<R>> nodes = context.getNodes().stream()
			.map(node -> new ParsedCommandNode<>(tNodeToRNode.apply(node.getNode()), node.getRange()))
			.collect(Collectors.toList());
		StringRange range = context.getRange();
		CommandContext<R> child = tContextToRContext.apply(context.getChild());
		RedirectModifier<R> modifier = context.getRedirectModifier() == null ? null : c -> context.getRedirectModifier().apply(rContextToTContext.apply(c)).stream()
			.map(tToR)
			.collect(Collectors.toList());
		boolean forks = context.isForked();
		return new CommandContext<>(source, input, arguments, command, rootNode, nodes, range, child, modifier, forks);
	}

	public CommandContext<Object> toNMS(CommandContext<CommandSender> context) {
		return convertContext(context, CommandSenderConverter::toNMS, CommandSenderConverter::toNMS, CommandSenderConverter::toBukkit, CommandSenderConverter::toNMS);
	}

	public CommandContext<CommandSender> toBukkit(CommandContext<Object> context) {
		return convertContext(context, CommandSenderConverter::toBukkit, CommandSenderConverter::toBukkit, CommandSenderConverter::toNMS, CommandSenderConverter::toBukkit);
	}

	private <T, R> CommandNode<R> convertNode(CommandNode<T> node, Function<T, R> tToR, Function<R, T> rToT, Function<CommandContext<R>, CommandContext<T>> rContextToTContext, Function<CommandNode<T>, CommandNode<R>> tNodeToRNode) {
		if (node == null) {
			return null;
		}
		Collection<CommandNode<R>> children = node.getChildren().stream()
			.map(tNodeToRNode)
			.collect(Collectors.toList());
		Predicate<R> requirement = node.getRequirement() == null ? null : source -> node.getRequirement().test(rToT.apply(source));
		CommandNode<R> redirect = tNodeToRNode.apply(node.getRedirect());
		RedirectModifier<R> modifier = node.getRedirectModifier() == null ? null : context -> node.getRedirectModifier().apply(rContextToTContext.apply(context)).stream()
			.map(tToR)
			.collect(Collectors.toList());
		boolean fork = node.isFork();
		Command<R> command = node.getCommand() == null ? null : context -> node.getCommand().run(rContextToTContext.apply(context));
		CommandNode<R> converted;
		if (node instanceof ArgumentCommandNode<T, ?> argNode) {
			String name = argNode.getName();
			ArgumentType<?> type = argNode.getType();
			SuggestionProvider<R> customSuggestions = argNode.getCustomSuggestions() == null ? null : (context, builder) -> argNode.getCustomSuggestions().getSuggestions(rContextToTContext.apply(context), builder);
			converted = new ArgumentCommandNode<>(name, type, command, requirement, redirect, modifier, fork, customSuggestions);
		} else if (node instanceof LiteralCommandNode<T> literalNode) {
			String literal = literalNode.getLiteral();
			converted = new LiteralCommandNode<>(literal, command, requirement, redirect, modifier, fork);
		} else {
			converted = new RootCommandNode<>();
		}
		children.forEach(converted::addChild);
		return converted;
	}

	public CommandNode<Object> toNMS(CommandNode<CommandSender> node) {
		return convertNode(node, CommandSenderConverter::toNMS, CommandSenderConverter::toBukkit, CommandSenderConverter::toBukkit, CommandSenderConverter::toNMS);
	}

	public CommandNode<CommandSender> toBukkit(CommandNode<Object> node) {
		return convertNode(node, CommandSenderConverter::toBukkit, CommandSenderConverter::toNMS, CommandSenderConverter::toNMS, CommandSenderConverter::toBukkit);
	}

}
