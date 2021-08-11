package com.akon.lives.commands;

import com.mojang.brigadier.Message;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public record ComponentMessageWrapper(BaseComponent... components) implements Message {

	@Override
	public String getString() {
		return TextComponent.toLegacyText(this.components);
	}

}
