package io.lightstudios.coins.commands.overall.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.interfaces.LightCommand;
import io.papermc.paper.util.ServerWorkerThread;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;

public class DebugCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("debug");
    }

    @Override
    public String getDescription() {
        return "Shows debug information about the plugin.";
    }

    @Override
    public String getSyntax() {
        return "/lightcoins debug";
    }

    @Override
    public int maxArgs() {
        return 1;
    }

    @Override
    public String getPermission() {
        return LightPermissions.DEBUG_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {;
            if (args.length == 1) {
                return getSubcommand();
            }
            return null;
        };
    }

    @Override
    public boolean performAsPlayer(Player player, String[] args) {
        String os = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String ram = String.valueOf(Runtime.getRuntime().totalMemory() / (1024 * 1024));
        String maxRam = String.valueOf(Runtime.getRuntime().maxMemory() / (1024 * 1024));
        String cores = String.valueOf(Runtime.getRuntime().availableProcessors());
        String cpu = System.getenv("PROCESSOR_IDENTIFIER");
        String cpuCores = System.getenv("NUMBER_OF_PROCESSORS");
        String databaseType = LightCore.instance.getSettings().syncType();
        String isMultiSync = LightCore.instance.getSettings().multiServerEnabled() ? "<green>enabled" : "<red>disabled";
        String databaseConnected = LightCore.instance.getSqlDatabase().checkConnection() ? "<green>connected" : "<red>disconnected";
        String registeredAccounts = String.valueOf(LightCoins.instance.getLightCoinsAPI().getAccountData().size());
        String redisTest = LightCore.instance.getRedisManager() != null ? LightCore.instance.getRedisManager().testConnection() ? "<green>active" : "<red>inactive" : "<red>off";
        String redisStatus = LightCore.instance.getSettings().redisEnabled() ? "<green>enabled" : "<red>disabled";
        String serverVersion = Bukkit.getVersion();
        String serverType = Bukkit.getServer().getName();

        String lightCoinsVersion = LightCoins.instance.getDescription().getVersion();
        String lightCoreVersion = LightCore.instance.getDescription().getVersion();

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String currentTPS1 = decimalFormat.format(Bukkit.getServer().getTPS()[0]);
        String currentTPS5 = decimalFormat.format(Bukkit.getServer().getTPS()[1]);
        String currentTPS15 = decimalFormat.format(Bukkit.getServer().getTPS()[2]);
        String currentMspt = decimalFormat.format(Bukkit.getServer().getAverageTickTime());

        List<String> debugMessages = LightCoins.instance.getMessageConfig().debug()
                .stream()
                .map(msg -> msg.replace("#os#", os)
                        .replace("#os-version#", osVersion)
                        .replace("#os-arch#", osArch)
                        .replace("#java-version#", javaVersion)
                        .replace("#java-vendor#", javaVendor)
                        .replace("#ram-usage#", ram)
                        .replace("#ram#", maxRam)
                        .replace("#cpu#", cpu != null ? cpu : "Unknown")
                        .replace("#cpu-cores#", cpuCores != null ? cpuCores : cores)
                        .replace("#database-type#", databaseType)
                        .replace("#database-status#", databaseConnected)
                        .replace("#multisync#", isMultiSync)
                        .replace("#accounts#", registeredAccounts)
                        .replace("#redis-status#", redisStatus)
                        .replace("#redis-test#", redisTest)
                        .replace("#server-version#", serverVersion)
                        .replace("#server-type#", serverType)
                        .replace("#tps-1#", currentTPS1)
                        .replace("#tps-5#", currentTPS5)
                        .replace("#tps-15#", currentTPS15)
                        .replace("#coins-version#", lightCoinsVersion)
                        .replace("#core-version#", lightCoreVersion)
                        .replace("#current-mspt#", currentMspt))
                .toList();

        LightCore.instance.getMessageSender().sendPlayerMessage(player, debugMessages);
        return true;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
