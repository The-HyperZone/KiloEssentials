package org.kilocraft.essentials.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kilocraft.essentials.EssentialPermission;
import org.kilocraft.essentials.KiloCommands;
import org.kilocraft.essentials.KiloDebugUtils;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.ModConstants;
import org.kilocraft.essentials.api.event.player.PlayerOnChatMessageEvent;
import org.kilocraft.essentials.api.event.player.PlayerOnDirectMessageEvent;
import org.kilocraft.essentials.api.text.ComponentText;
import org.kilocraft.essentials.api.text.TextFormat;
import org.kilocraft.essentials.api.user.CommandSourceUser;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.commands.CommandUtils;
import org.kilocraft.essentials.config.ConfigVariableFactory;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.config.main.sections.chat.ChatConfigSection;
import org.kilocraft.essentials.config.main.sections.chat.ChatPingSoundConfigSection;
import org.kilocraft.essentials.events.player.PlayerOnChatMessageEventImpl;
import org.kilocraft.essentials.events.player.PlayerOnDirectMessageEventImpl;
import org.kilocraft.essentials.user.OnlineServerUser;
import org.kilocraft.essentials.user.ServerUser;
import org.kilocraft.essentials.user.ServerUserManager;
import org.kilocraft.essentials.user.preference.Preferences;
import org.kilocraft.essentials.util.RegexLib;
import org.kilocraft.essentials.util.messages.nodes.ExceptionMessageNode;
import org.kilocraft.essentials.util.text.Texter;

import java.rmi.UnexpectedException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerChat {
    private static final String DEBUG_EXCEPTION = "--texc";
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private static final Pattern LINK_PATTERN = Pattern.compile(RegexLib.URL.get());
    private static final int LINK_MAX_LENGTH = 20;
    private static final int COMMAND_MAX_LENGTH = 45;
    private static final SimpleCommandExceptionType CANT_MESSAGE_EXCEPTION = new SimpleCommandExceptionType(StringText.of(true, "command.message.error"));
    private static ChatConfigSection config;
    private static String pingEveryoneTemplate;
    private static String senderFormat;
    private static String displayFormat;
    private static String pingFailedDisplayFormat;
    private static String everyoneDisplayFormat;
    private static String itemFormat;
    private static String hoverStyle = ModConstants.translation("channel.message.hover");
    private static String hoverStyleNicked = ModConstants.translation("channel.message.hover.nicked");
    private static String hoverDateStyle = ModConstants.translation("channel.message.hover.time");
    private static String commandSpyHoverStyle = ModConstants.translation("channel.commandspy.hover");
    private static boolean pingSoundEnabled;
    private static boolean pingEnabled;

    public static void load() {
        config = KiloConfig.main().chat();
        pingEveryoneTemplate = ServerChat.config.ping().everyoneTypedFormat;
        senderFormat = ServerChat.config.ping().typedFormat;
        displayFormat = ServerChat.config.ping().pingedFormat;
        pingFailedDisplayFormat = ServerChat.config.ping().pingedNotPingedFormat;
        everyoneDisplayFormat = ServerChat.config.ping().everyonePingedFormat;
        itemFormat = ServerChat.config.itemFormat;
        pingSoundEnabled = ServerChat.config.ping().pingSound().enabled;
        pingEnabled = ServerChat.config.ping().enabled;
    }

    public static void sendSafely(final OnlineUser sender, final MutableTextMessage message, final Channel channel) {
        try {
            send(sender, message, channel);
        } catch (Exception e) {
            MutableText text = Texter.newTranslatable("command.failed");
            if (SharedConstants.isDevelopment) {
                text.append("\n").append(Util.getInnermostMessage(e));
                KiloDebugUtils.getLogger().error("Processing a chat message throw an exception", e);
            }

            sender.getCommandSource().sendError(text);
        }
    }

    public static void sendChatMessage(final OnlineUser sender, final String raw, final Channel channel) {
        Objects.requireNonNull(channel, () -> {
            sender.getPreferences().set(Preferences.CHAT_CHANNEL, Channel.PUBLIC);
            return "Channel must not be null! Channel set to Public";
        });

        boolean processWords;
        if (channel == Channel.PUBLIC) {
            processWords = KiloConfig.messages().censorList().censor;
        } else {
            processWords = KiloConfig.messages().censorList().censorPrivateChannels;
        }

        try {
            final Component messageComponent = ComponentText.of(processWords ? processWords(sender, raw) : raw);

            final Text text = ComponentText.toText(
                    ComponentText.of(ConfigVariableFactory.replaceUserVariables(channel.getFormat(), sender))
                            .append(ComponentText.removeEvents(messageComponent))
            );

            if (sender.getPreference(Preferences.CHAT_VISIBILITY) == VisibilityPreference.MENTIONS) {
                sender.sendMessage(text);
            }

            channel.send(text, true);
            KiloServer.getServer().sendMessage(text);
        } catch (Exception e) {
            sender.sendError(e.getMessage());
        }
    }

    public static void send(final OnlineUser sender, final MutableTextMessage message, final Channel channel) throws Exception {
//        if (message.getOriginal().startsWith(DEBUG_EXCEPTION) && SharedConstants.isDevelopment) {
//            throw new UnexpectedException("Debug exception thrown by " + sender.getUsername() + message.getOriginal().replaceFirst(DEBUG_EXCEPTION, ""));
//        }
//
//        if (channel == null) {
//            sender.getPreferences().set(Preferences.CHAT_CHANNEL, Channel.PUBLIC);
//            throw new NullPointerException("Channel must not be Null! channel set to Public");
//        }
//
//        boolean processWords;
//        if (channel == Channel.PUBLIC) {
//            processWords = true;
//        } else {
//            processWords = KiloConfig.messages().censorList().censorPrivateChannels;
//        }
//
//        try {
//            message.setMessage(
//                    processWords ? processWords(sender, message.getOriginal()) : message.getOriginal(),
//                    KiloEssentials.hasPermissionNode(sender.getCommandSource(), EssentialPermission.CHAT_COLOR)
//            );
//        } catch (Exception e) {
//            return;
//        }
//
//        MutableTextMessage prefix = new MutableTextMessage(ConfigVariableFactory.replaceUserVariables(channel.getFormat(), sender)
//                .replace("%USER_RANKED_DISPLAYNAME%", sender.getRankedDisplayName().getString()));
//
//        boolean mentions = processPings(sender, message, channel);
//
//        Text component = ServerChat.stringToMessageComponent(
//                message.getFormattedMessage(),
//                sender,
//                sender.hasPermission(EssentialPermission.CHAT_URL),
//                sender.hasPermission(EssentialPermission.CHAT_SHOW_ITEM)
//        );
//
//        MutableText text = new LiteralText("");
//        text.append(
//                prefix.toComponent()
//                        .styled((style) -> style.withHoverEvent(hoverEvent(sender, channel)).withClickEvent(clickEvent(sender)))
//        ).append(" ").append(component);
//
//        PlayerOnChatMessageEvent event = KiloServer.getServer().triggerEvent(new PlayerOnChatMessageEventImpl(sender.asPlayer(), component.getString(), channel));
//        if (event.isCancelled()) {
//            if (event.getCancelReason() != null) {
//                sender.sendError(event.getCancelReason());
//            }
//
//            return;
//        }
//
//        if (sender.getPreference(Preferences.CHAT_VISIBILITY) == VisibilityPreference.MENTIONS) {
//            sender.sendMessage(text);
//        }
//
//        KiloServer.getServer().sendMessage(TextFormat.clearColorCodes(text.getString()));
//        channel.send(text, mentions);
    }

    private static boolean processPings(final OnlineUser sender, final MutableTextMessage message, final Channel channel) {
        if (!pingEnabled && !KiloEssentials.hasPermissionNode(sender.getCommandSource(), EssentialPermission.CHAT_PING_OTHER)) {
            return false;
        }

        boolean contains = false;
        if (message.getOriginal().contains(pingEveryoneTemplate) && KiloEssentials.hasPermissionNode(sender.getCommandSource(), EssentialPermission.CHAT_PING_EVERYONE)) {
            message.setMessage(message.getFormattedMessage().replaceAll(pingEveryoneTemplate, everyoneDisplayFormat + "&r"));

            for (OnlineUser user : KiloServer.getServer().getUserManager().getOnlineUsersAsList()) {
                if (user.getPreference(Preferences.CHAT_CHANNEL) == channel) {
                    ServerChat.pingUser(user.asPlayer(), config.ping().pingSound());
                }
            }
        }

        for (OnlineUser target : KiloServer.getServer().getUserManager().getOnlineUsersAsList()) {
            String nameFormat = senderFormat.replace("%PLAYER_NAME%", target.getUsername());
            String nickFormat = senderFormat.replace("%PLAYER_NAME%", target.getDisplayName());
            if ((!message.getOriginal().contains(nameFormat) && !message.getOriginal().contains(nickFormat)) || !KiloEssentials.hasPermissionNode(target.getCommandSource(), EssentialPermission.CHAT_GET_PINGED)) {
                continue;
            }

            boolean canPing = target.getPreference(Preferences.CHAT_CHANNEL) == channel;
            String formattedPing = canPing ? displayFormat : pingFailedDisplayFormat;
            String format = message.getOriginal().contains(nameFormat) ? nameFormat : nickFormat;

            message.setMessage(
                    message.getFormattedMessage().replaceAll(
                            format,
                            formattedPing.replaceAll("%PLAYER_DISPLAYNAME%", target.getFormattedDisplayName() + "&r")
                    ).replaceAll("%", "")
            );

            if (pingSoundEnabled && canPing) {
                contains = true;
                pingUser(target, MentionTypes.PUBLIC);
            }
        }

        return contains;
    }

    private static HoverEvent hoverEvent(final OnlineUser user, Channel channel) {
        String date = String.format(hoverDateStyle, dateFormat.format(new Date()));

        assert channel.getFormat() != null;
        if (user.hasNickname() && channel.getFormat().contains("%USER_RANKED_DISPLAYNAME%")) {
            return Texter.Events.onHover(String.format(hoverStyleNicked, user.getUsername(), date));
        } else {
            return Texter.Events.onHover(String.format(hoverStyle, date));
        }
    }

    private static ClickEvent clickEvent(final OnlineUser user) {
        return Texter.Events.onClickSuggest("/msg " + user.getUsername() + " ");
    }

    public static void pingUser(final OnlineUser target, final MentionTypes type) {
        if (target.getPreference(Preferences.DON_NOT_DISTURB) && type != MentionTypes.EVERYONE) {
            return;
        }

        ChatPingSoundConfigSection cfg = null;
        switch (type) {
            case PUBLIC:
            case EVERYONE:
                cfg = config.ping().pingSound();
                break;
            case PRIVATE:
                cfg = config.privateChat().pingSound();
                break;
        }

        pingUser(target.asPlayer(), cfg);
    }

    private static void pingUser(final ServerPlayerEntity target, final ChatPingSoundConfigSection cfg) {
        Vec3d vec3d = target.getCommandSource().getPosition();
        if (target.networkHandler != null) {
            target.networkHandler.sendPacket(
                    new PlaySoundIdS2CPacket(
                            new Identifier(cfg.id),
                            SoundCategory.MASTER,
                            vec3d, (float) cfg.volume, (float) cfg.pitch)
            );
        }
    }

    public static int sendDirectMessage(final ServerCommandSource source, final OnlineUser target, final String message) throws CommandSyntaxException {
        CommandSourceUser src = KiloServer.getServer().getCommandSourceUser(source);

        if (!((ServerUser) target).shouldMessage() && src.getUser() != null) {
            if (!src.isConsole() && src.isOnline() && !((ServerUser) src.getUser()).isStaff()) {
                throw ServerChat.CANT_MESSAGE_EXCEPTION.create();
            }
        }

        if (!CommandUtils.isConsole(source)) {
            OnlineUser online = KiloServer.getServer().getOnlineUser(source.getPlayer());
            online.setLastMessageReceptionist(target);
            target.setLastMessageReceptionist(online);
        }

        if (CommandUtils.areTheSame(source, target)) {
            throw KiloCommands.getException(ExceptionMessageNode.SOURCE_IS_TARGET).create();
        }

        String msg = message;
        if (KiloConfig.messages().censorList().censorDirectMessages) {
            try {
                msg = processWords(src, msg);
            } catch (Exception e) {
                return -1;
            }
        }

        ServerChat.messagePrivately(source, target, msg);
        return 1;
    }

    public static void messagePrivately(final ServerCommandSource source, final OnlineUser target, final String raw) throws CommandSyntaxException {
        String format = ServerChat.config.privateChat().privateChat;
        String me_format = ServerChat.config.privateChat().privateChatMeFormat;
        String sourceName = source.getName();

        if (CommandUtils.isPlayer(source)) {
            OnlineUser user = KiloServer.getServer().getOnlineUser(source.getPlayer());
            if (KiloServer.getServer().getUserManager().getPunishmentManager().isMuted(user)) {
                KiloChat.sendMessageTo(source, new MutableTextMessage(ServerUserManager.getMuteMessage(user)));
                return;
            }

            if (target.ignored(source.getPlayer().getUuid())) {
                throw KiloCommands.getException(ExceptionMessageNode.IGNORED, target.getFormattedDisplayName()).create();
            }
        }

        PlayerOnDirectMessageEvent event = KiloServer.getServer().triggerEvent(new PlayerOnDirectMessageEventImpl(source, target, raw));
        if (event.isCancelled()) {
            if (event.getCancelReason() != null) {
                source.sendError(Texter.newText(event.getCancelReason()));
            }
            return;
        }
        final String message = event.getMessage();

        String toSource = format.replace("%SOURCE%", me_format)
                .replace("%TARGET%", "&r" + target.getUsername() + "&r")
                .replace("%MESSAGE%", message);
        String toTarget = format.replace("%SOURCE%", sourceName)
                .replace("%TARGET%", me_format)
                .replace("%MESSAGE%", message);

        String toSpy = ServerChat.config.socialSpyFormat.replace("%SOURCE%", sourceName)
                .replace("%TARGET%", target.getUsername() + "&r")
                .replace("%MESSAGE%", message);

        if (target.getPreference(Preferences.SOUNDS)) {
            pingUser(target, MentionTypes.PRIVATE);
        }

        ComponentText.toText(ComponentText.removeEvents(ComponentText.of(toSource)));

        KiloChat.sendMessageToSource(source, new MutableTextMessage(toSource, true).toText().formatted(Formatting.WHITE));
        KiloChat.sendMessageTo(target.asPlayer(), new MutableTextMessage(toTarget, true).toText().formatted(Formatting.WHITE));

        for (final OnlineServerUser user : KiloServer.getServer().getUserManager().getOnlineUsers().values()) {
            if (user.getPreference(Preferences.SOCIAL_SPY) && !CommandUtils.areTheSame(source, user) && !CommandUtils.areTheSame(target, user)) {
                user.sendMessage(new MutableTextMessage(toSpy, true).toText().formatted(Formatting.GRAY));
            }

            KiloServer.getServer().triggerEvent(new PlayerOnDirectMessageEventImpl(source, target, raw));
        }

        KiloServer.getServer().sendMessage(toSpy);
    }

    public static void sendCommandSpy(final ServerCommandSource source, final String command) {
        String format = ServerChat.config.commandSpyFormat;
        String shortenedCommand = command.substring(0, Math.min(command.length(), COMMAND_MAX_LENGTH));
        String toSpy = format.replace("%SOURCE%", source.getName()).replace("%COMMAND%", shortenedCommand);
        MutableText text = Texter.newText(toSpy).formatted(Formatting.GRAY);

        if (command.length() > COMMAND_MAX_LENGTH) {
            text.append("...");
        }

        text.styled((style) -> style.withHoverEvent(Texter.Events.onHover(commandSpyHoverStyle)).withClickEvent(Texter.Events.onClickSuggest("/" + command)));

        for (OnlineServerUser user : KiloServer.getServer().getUserManager().getOnlineUsers().values()) {
            if (user.getPreference(Preferences.COMMAND_SPY) && !CommandUtils.areTheSame(source, user)) {
                user.sendMessage(text);
            }
        }
    }

    public static void send(Text message, EssentialPermission permission) {
        for (OnlineUser user : KiloServer.getServer().getUserManager().getOnlineUsersAsList()) {
            if (user.hasPermission(permission)) {
                user.sendMessage(message);
            }
        }
    }

    public static void sendLangMessage(EssentialPermission permission, String key, Object... objects) {
        for (OnlineUser user : KiloServer.getServer().getUserManager().getOnlineUsersAsList()) {
            if (user.hasPermission(permission)) {
                user.sendLangMessage(key, objects);
            }
        }
    }

    private static String processWords(@NotNull final OnlineUser sender, @NotNull final String message) throws Exception {
        String msg = message;
        String lowerCased = msg.toLowerCase(Locale.ROOT);
        int index = 0;
        boolean censor = KiloConfig.messages().censorList().censor;

        for (String value : KiloConfig.messages().censorList().words) {
            String s = value.toLowerCase(Locale.ROOT);
            if (lowerCased.contains(s)) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < s.length(); i++) {
                    builder.append(KiloConfig.messages().censorList().alternateChar);
                }

                msg = msg.replaceAll(("(?i)" + s), Matcher.quoteReplacement(builder.toString()));
                index++;

                if (!censor) {
                    break;
                }
            }
        }

        if (index >= 1 && !censor) {
            throw new Exception(String.format(KiloConfig.messages().censorList().blockMessage, sender.getFormattedDisplayName()));
        }

        return msg;
    }

    public static Text stringToMessageComponent(@NotNull String string, OnlineUser sender, boolean appendLinks, boolean appendItems) {
        return ComponentText.toText(ComponentText.of(string));
    }

    public enum Channel {
        PUBLIC("public"),
        STAFF("staff"),
        BUILDER("builder");

        private String id;

        Channel(String id) {
            this.id = id;
        }

        @Nullable
        public static Channel getById(String id) {
            for (Channel value : values()) {
                if (value.id.equalsIgnoreCase(id)) {
                    return value;
                }
            }

            return null;
        }

        public String getId() {
            return this.id;
        }

        public void send(Text message) {
            send(message, true);
        }

        public void send(Text message, boolean mentions) {
            switch (this) {
                case PUBLIC:
                    for (OnlineUser user : KiloServer.getServer().getUserManager().getOnlineUsersAsList()) {
                        VisibilityPreference preference = user.getPreference(Preferences.CHAT_VISIBILITY);
                        if (preference == VisibilityPreference.ALL || (preference == VisibilityPreference.MENTIONS && mentions)) {
                            user.sendMessage(message);
                        }
                    }
                    break;
                case STAFF:
                    ServerChat.send(message, EssentialPermission.STAFF);
                    break;
                case BUILDER:
                    ServerChat.send(message, EssentialPermission.BUILDER);
                    break;
            }
        }

        public void sendLangMessage(String key, Object... objects) {
            switch (this) {
                case PUBLIC:
                    KiloChat.broadCastLang(key, objects);
                    break;
                case STAFF:
                    ServerChat.sendLangMessage(EssentialPermission.STAFF, key, objects);
                    break;
                case BUILDER:
                    ServerChat.sendLangMessage(EssentialPermission.BUILDER, key, objects);
                    break;
            }
        }

        public String getFormat() {
            switch (this) {
                case PUBLIC:
                    return KiloConfig.main().chat().prefixes().publicChat;
                case STAFF:
                    return KiloConfig.main().chat().prefixes().staffChat;
                case BUILDER:
                    return KiloConfig.main().chat().prefixes().builderChat;
            }

            return null;
        }
    }

    public enum MentionTypes {
        PUBLIC,
        PRIVATE,
        EVERYONE;
    }

    public enum VisibilityPreference {
        ALL,
        MENTIONS;

        @Nullable
        public static VisibilityPreference getByName(final String name) {
            for (VisibilityPreference value : values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return value;
                }
            }

            return null;
        }

        public static String[] names() {
            String[] strings = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                strings[i] = values()[i].toString();
            }
            return strings;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
