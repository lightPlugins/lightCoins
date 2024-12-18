package io.lightstudios.coins.commands.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightTimers;
import io.lightstudios.core.util.interfaces.LightCommand;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class DeleteAccountCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("delete");
    }

    @Override
    public String getDescription() {
        return "Delete a player's account";
    }

    @Override
    public String getSyntax() {
        return "/coins delete <playername>";
    }

    @Override
    public int maxArgs() {
        return 2;
    }

    @Override
    public String getPermission() {
        return LightPermissions.DELETE_ACCOUNT_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {

            if(args.length == 2) {
                return Bukkit.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
            }

            return null;
        };
    }

    @Override
    public boolean performAsPlayer(Player player, String[] strings) {
        if (strings.length != 2) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().wrongSyntax().stream().map(str -> str
                                    .replace("#syntax#", getSyntax())
                            ).collect(Collectors.joining()));
            return false;
        }

        String targetPlayerName = strings[1];
        OfflinePlayer targetPlayer = Bukkit.getServer().getOfflinePlayer(targetPlayerName);
        AccountData playerData = LightCoins.instance.getLightCoinsAPI().getPlayerData(targetPlayer.getUniqueId());
        if (playerData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().playerNotFound().stream().map(str -> str
                                    .replace("#player#", targetPlayerName)
                            ).collect(Collectors.joining()));
            return false;
        }
        LightCoins.instance.getConsolePrinter().printInfo("Deleting account for " + targetPlayerName);
        CompletableFuture<Boolean> deleteResult = LightCoins.instance.getCoinsTable().deleteAccount(targetPlayer.getUniqueId());
        deleteResult.thenAccept(success -> {
            if (success) {
                AccountData test = LightCoins.instance.getLightCoinsAPI().getPlayerData().remove(targetPlayer.getUniqueId());
                if(test == null) {
                    LightCoins.instance.getConsolePrinter().printDebug("Deleted player data for " + targetPlayerName);
                }
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().playerDeleted().stream().map(str -> str
                                        .replace("#player#", targetPlayerName)
                                ).collect(Collectors.joining()));

                if(targetPlayer.isOnline()) {
                    if(targetPlayer.getPlayer() != null) {
                        LightTimers.doSync((task) -> {
                            LightCore.instance.getPlayerPunishment().autoKickPlayer(targetPlayer.getPlayer(),
                                    "<red>Your coins account has been deleted\nPlease rejoin the server");
                        }, 40L);
                    }
                }
            } else {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                        .replace("#info#", "Could not delete account for " + targetPlayerName)
                                ).collect(Collectors.joining()));
            }
        });

        return true;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
