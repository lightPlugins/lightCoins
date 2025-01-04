package io.lightstudios.coins.commands.virtual.defaults;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VirtualShowCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("show");
    }

    @Override
    public String getDescription() {
        return "Show your virtual currency balance";
    }

    @Override
    public String getSyntax() {
        return "/virtual show <currency> <playername>";
    }

    @Override
    public int maxArgs() {
        return 3;
    }

    @Override
    public String getPermission() {
        return LightPermissions.VIRTUAL_SHOW_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {

            if(args.length == 2) {
                return LightCoins.instance.getVirtualCurrencyFiles().getYamlFiles().stream()
                        .map(file -> file.getName().replace(".yml", "")).toList();
            }

            if(args.length == 3) {
                if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                        LightCore.instance.getSettings().multiServerEnabled()) {
                    return Arrays.stream(Bukkit.getServer().getOfflinePlayers()).map(OfflinePlayer::getName).toList();
                } else {
                    return LightCoins.instance.getLightCoinsAPI().getAccountDataPlayerNames();
                }
            }

            return null;
        };
    }

    @Override
    public boolean performAsPlayer(Player player, String[] args) {

        if(args.length == 3) {

            String targetCurrency = args[1];
            String targetName = args[2];

            if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                    LightCore.instance.getSettings().multiServerEnabled()) {

                OfflinePlayer target = Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                        .filter(offlinePlayer -> offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(targetName))
                        .findFirst()
                        .orElse(null);

                if(target == null) {
                    LightCore.instance.getMessageSender().sendPlayerMessage(
                            player,
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().playerNotFound().stream().map(str -> str
                                            .replace("#player#", targetName)
                                    ).collect(Collectors.joining()));
                    return false;
                }

                List<VirtualData> virtualDataList = LightCoins.instance.getVirtualDataTable().readVirtualData().join();
                VirtualData virtualData = virtualDataList.stream()
                        .filter(data -> data.getPlayerUUID().equals(target.getUniqueId()) && data.getCurrencyName().equalsIgnoreCase(targetCurrency))
                        .findFirst()
                        .orElse(null);

                if(virtualData == null) {
                    LightCore.instance.getMessageSender().sendPlayerMessage(
                            player,
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().virtualCurrencyNotFound().stream().map(str -> str
                                            .replace("#currency#", targetCurrency)
                                    ).collect(Collectors.joining()));
                    return false;
                }

                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().virtualShow().stream().map(str -> str
                                        .replace("#currency#", virtualData.getFormattedCurrencySymbol())
                                        .replace("#amount#", virtualData.getFormattedBalance())
                                ).collect(Collectors.joining()));

                return true;

            }

            AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(player);

            if(accountData == null) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                        .replace("#info#", "Could not find your player data")
                                ).collect(Collectors.joining()));
                return false;
            }

            VirtualData virtualData = accountData.getVirtualCurrencyByName(args[1]);

            if(virtualData == null) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().virtualCurrencyNotFound().stream().map(str -> str
                                        .replace("#currency#", args[1])
                                        .replace("#syntax#", getSyntax())
                                ).collect(Collectors.joining()));
                return false;
            }

            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().virtualShow().stream().map(str -> str
                                    .replace("#currency#", virtualData.getFormattedCurrencySymbol())
                                    .replace("#amount#", virtualData.getFormattedBalance())
                                    .replace("#player#", virtualData.getPlayerName())
                            ).collect(Collectors.joining()));

            return true;
        }

        LightCore.instance.getMessageSender().sendPlayerMessage(
                player,
                LightCoins.instance.getMessageConfig().prefix() +
                        LightCoins.instance.getMessageConfig().wrongSyntax().stream().map(str -> str
                                .replace("#syntax#", getSyntax())
                        ).collect(Collectors.joining()));

        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
