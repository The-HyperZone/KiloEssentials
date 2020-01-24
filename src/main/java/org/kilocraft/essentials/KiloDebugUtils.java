package org.kilocraft.essentials;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.ModConstants;
import org.kilocraft.essentials.api.chat.TextFormat;
import org.kilocraft.essentials.api.server.Server;
import org.kilocraft.essentials.api.world.MonitorableWorld;
import org.kilocraft.essentials.util.TPSTracker;

public class KiloDebugUtils {
    private KiloEssentials ess;
    private Server server;
    private MinecraftServer minecraftServer;
    private CommandBossBar bossBar;

    public KiloDebugUtils(KiloEssentials ess) {
        this.ess = ess;
        this.server = KiloServer.getServer();
        this.minecraftServer = server.getVanillaServer();

        setupBossBar();
    }

    public void onScheduledUpdate() {
        updateBossbar();
    }

    private void setupBossBar() {
        BossBarManager manager = minecraftServer.getBossBarManager();
        bossBar = manager.add(new Identifier("kiloessentials", "debug_bar"), new LiteralText("DebugBar"));
        bossBar.setMaxValue(20);
        bossBar.setOverlay(BossBar.Style.NOTCHED_20);
    }

    public void updateBossbar() {
        if (minecraftServer.getBossBarManager().get(new Identifier("kiloessentials", "debug_bar")) == null)
            setupBossBar();

        int loadedChunks = 0;
        int entities = 0;
        for (ServerWorld world : minecraftServer.getWorlds()) {
            MonitorableWorld moWorld = ((MonitorableWorld) world);
            loadedChunks = loadedChunks + moWorld.totalLoadedChunks();
            entities = entities + moWorld.loadedEntities();
        }

        int tps = (int) TPSTracker.tps1.getAverage();
        bossBar.setValue(tps);

        String debugText = String.format(ModConstants.getProperties().getProperty("debug_bar_text"),
                TextFormat.getFormattedTPS(TPSTracker.tps1.getAverage()), entities, loadedChunks, ModConstants.getVersionInt().split(" ")[0]);

        Text text = new LiteralText("[").formatted(Formatting.BOLD)
                .append(new LiteralText("Debug").formatted(Formatting.YELLOW, Formatting.BOLD)
                .append(new LiteralText("]").formatted(Formatting.BOLD).append(TextFormat.translateToLiteralText('&', debugText))));

        if (tps > 15) {
            bossBar.setColor(BossBar.Color.GREEN);
            text.formatted(Formatting.GREEN);
        } else if (tps > 10) {
            bossBar.setColor(BossBar.Color.YELLOW);
            text.formatted(Formatting.YELLOW);
        } else {
            bossBar.setColor(BossBar.Color.RED);
            text.formatted(Formatting.RED);
        }

        bossBar.setName(text);
        if (minecraftServer.getPlayerManager().getPlayerList() != null)
            bossBar.addPlayers(minecraftServer.getPlayerManager().getPlayerList());
    }

}
