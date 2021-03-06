package org.kilocraft.essentials.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.ModConstants;
import org.kilocraft.essentials.api.text.ComponentText;
import org.kilocraft.essentials.api.text.TextFormat;
import org.kilocraft.essentials.commands.CommandUtils;
import org.kilocraft.essentials.config.ConfigVariableFactory;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.config.main.sections.chat.ChatConfigSection;
import org.kilocraft.essentials.config.messages.Messages;
import org.kilocraft.essentials.user.ServerUser;

import static org.kilocraft.essentials.api.KiloServer.getServer;

public class KiloChat {
	private static final ChatConfigSection config = KiloConfig.main().chat();
	private static final Messages messages = KiloConfig.messages();

	public static String getFormattedLang(String key) {
		return getFormattedString(ModConstants.getStrings().getProperty(key), (Object) null);
	}

	public static String getFormattedLang(String key, Object... objects) {
		return getFormattedString(ModConstants.getStrings().getProperty(key), objects);
	}

	public static String getFormattedString(String string, Object... objects) {
		return (objects[0] != null) ? String.format(string, objects) : string;
	}

	public static void sendMessageTo(ServerPlayerEntity player, MutableTextMessage mutableTextMessage) {
		sendMessageTo(player, mutableTextMessage.toText());
	}

	public static void sendMessageTo(ServerCommandSource source, MutableTextMessage mutableTextMessage) throws CommandSyntaxException {
		sendMessageTo(source.getPlayer(), mutableTextMessage.toText());
	}

	public static void sendMessageTo(ServerPlayerEntity player, Text text) {
		player.sendMessage(text, false);
	}

	public static void sendMessageTo(ServerCommandSource source, Text text) {
		source.sendFeedback(text, false);
	}

	public static void sendMessageToSource(ServerCommandSource source, MutableTextMessage message) {
		if (CommandUtils.isConsole(source))
			KiloEssentials.getServer().sendMessage(message.getOriginal());
		else
			source.sendFeedback(message.toText(), false);
	}

	public static void sendMessageToSource(ServerCommandSource source, Text text) {
		if (CommandUtils.isConsole(source))
			getServer().sendMessage(text.asString());
		else
			source.sendFeedback(text, false);
	}

	public static void sendLangMessageTo(ServerCommandSource source, String key) {
		if (CommandUtils.isConsole(source))
			getServer().sendMessage(getFormattedLang(key));
		else
			source.sendFeedback(StringText.of(key), false);
	}

	public static void sendLangCommandFeedback(ServerCommandSource source, String key, boolean sendToOPs, Object... objects) {
		if (CommandUtils.isConsole(source))
			getServer().sendMessage(getFormattedLang(key, objects));
		else
			source.sendFeedback(StringText.of(true, key, objects), sendToOPs);
	}

	public static void sendLangMessageTo(ServerPlayerEntity player, String key) {
		sendMessageTo(player, StringText.of(true, key));
	}

	public static void sendLangMessageTo(ServerPlayerEntity player, String key, Object... objects) {
		sendMessageTo(player, StringText.of(true, key, objects));
	}

	public static void sendLangMessageTo(ServerCommandSource source, String key, Object... objects) {
		if (CommandUtils.isConsole(source))
			KiloEssentials.getServer().sendMessage(getFormattedLang(key, objects));
		else
			source.sendFeedback(StringText.of(true, key, objects), false);
	}

	public static void broadCastExceptConsole(MutableTextMessage mutableTextMessage) {
		for (PlayerEntity player : getServer().getPlayerList()) {
			player.sendMessage(mutableTextMessage.toText(), false);
		}
	}

	public static void broadCastLangExceptConsole(String key, Object... objects) {
		broadCastExceptConsole(new MutableTextMessage(getFormattedLang(key, objects), false));
	}

	public static void broadCastLangToConsole(String key, Object... objects) {
		broadCastToConsole(new MutableTextMessage(getFormattedLang(key, objects), false));
	}

	public static void broadCastToConsole(MutableTextMessage mutableTextMessage) {
		mutableTextMessage.setMessage(mutableTextMessage.getOriginal(), false);
		getServer().sendMessage(mutableTextMessage.toText());
	}

	public static void broadCast(MutableTextMessage mutableTextMessage) {
		for (PlayerEntity player : getServer().getPlayerList()) {
			player.sendMessage(mutableTextMessage.toText(), false);
		}

		getServer().sendMessage(mutableTextMessage.toText());
	}

	public static void broadCast(Text text) {
		for (PlayerEntity entity : getServer().getPlayerList()) {
			entity.sendMessage(text, false);
		}
	}

	public static void broadCast(String message) {
        for (PlayerEntity entity : getServer().getPlayerList()) {
            entity.sendMessage(ComponentText.toText(message), false);
        }
    }

	public static void broadCastLang(String key, Object... objects) {
		broadCast(getFormattedLang(key, objects));
	}

	public static void onUserJoin(ServerUser user) {
		broadCast(ConfigVariableFactory.replaceUserVariables(messages.events().userJoin, user));
	}

    public static void onUserLeave(ServerUser user) {
        broadCast(ConfigVariableFactory.replaceUserVariables(messages.events().userLeave, user));
    }
}