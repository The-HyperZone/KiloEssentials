package org.kilocraft.essentials.commands.play;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.EntitySelector;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.kilocraft.essentials.CommandPermission;
import org.kilocraft.essentials.KiloCommands;
import org.kilocraft.essentials.chat.StringText;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.command.ArgumentSuggestions;
import org.kilocraft.essentials.chat.KiloChat;
import org.kilocraft.essentials.commands.CommandUtils;

import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.command.argument.EntityArgumentType.player;

public class HealCommand extends EssentialCommand {
    public HealCommand() {
        super("heal", CommandPermission.HEAL_SELF);
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        RequiredArgumentBuilder<ServerCommandSource, EntitySelector> target = argument("target", player())
                .requires(s -> KiloCommands.hasPermission(s, CommandPermission.HEAL_OTHERS))
                .suggests(ArgumentSuggestions::allPlayers)
                .executes(context -> execute(context.getSource(), getPlayer(context, "target")));
        
        argumentBuilder.executes(context -> execute(context.getSource(), context.getSource().getPlayer()));
        commandNode.addChild(target.build());
    }

    private static int execute(ServerCommandSource source, ServerPlayerEntity player) {
        if (CommandUtils.areTheSame(source, player)) {
            if (player.getHealth() == player.getMaxHealth()) {
                KiloChat.sendMessageTo(player, StringText.of(true, "command.heal.exception.self"));
            } else {
                KiloChat.sendMessageTo(player, StringText.of(true, "command.heal.self"));
            }
        } else {
            if (player.getHealth() == player.getMaxHealth()) {
                KiloChat.sendMessageTo(source, StringText.of(true, "command.heal.exception.others", player.getName().asString()));
            } else {
                KiloChat.sendMessageTo(player, StringText.of(true, "command.heal.announce", source.getName()));
                KiloChat.sendMessageToSource(source, StringText.of(true, "command.heal.other", player.getName().asString()));
            }
        }

        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);

        return SUCCESS;
    }
}
