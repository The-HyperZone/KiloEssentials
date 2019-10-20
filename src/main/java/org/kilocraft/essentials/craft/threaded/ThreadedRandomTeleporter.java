package org.kilocraft.essentials.craft.threaded;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.kilocraft.essentials.craft.KiloEssentials;
import org.kilocraft.essentials.craft.commands.essentials.RandomTeleportCommand;

public class ThreadedRandomTeleporter implements Runnable, KiloThread {
    private ServerPlayerEntity playerEntity;
    private ServerCommandSource commandSource;

    public ThreadedRandomTeleporter(ServerPlayerEntity player, ServerCommandSource source) {
        this.playerEntity = player;
        this.commandSource = source;
        KiloEssentials.getLogger().info("Thread started by " + source.getName());
    }
    @Override
    public String getName() {
        return "RandomTeleporter";
    }

    @Override
    public void run() {
        RandomTeleportCommand.teleportRandomly(this.playerEntity, this.commandSource);
    }
}