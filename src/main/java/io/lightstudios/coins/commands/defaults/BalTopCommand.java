package io.lightstudios.coins.commands.defaults;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BalTopCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of();
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getSyntax() {
        return "";
    }

    @Override
    public int maxArgs() {
        return -1;
    }

    @Override
    public String getPermission() {
        return LightPermissions.BALTOP_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return null;
    }

    @Override
    public boolean performAsPlayer(Player player, String[] strings) {
        int x = 10;

        List<PlayerData> allPlayers = new ArrayList<>(LightCoins.instance.getLightCoinsAPI().getPlayerData().values());
        System.out.println("Total players: " + allPlayers.size());

        List<PlayerData> sortedPlayers = allPlayers.stream()
                .sorted((p1, p2) -> p2.getCoinsPlayer().getCoins().compareTo(p1.getCoinsPlayer().getCoins()))
                .limit(x)
                .toList();

        System.out.println("Filtered and sorted players: " + sortedPlayers.size());

        List<String> messages = new ArrayList<>(LightCoins.instance.getMessageConfig().baltopHeader());
        for (int i = 0; i < sortedPlayers.size(); i++) {
            PlayerData playerData = sortedPlayers.get(i);

            if (playerData.getPlayerName() == null) {
                LightCoins.instance.getConsolePrinter().printDebug("Player name is null for player with UUID: " + playerData.getUuid());
                continue;
            }

            messages.add(LightCoins.instance.getMessageConfig().baltopContent()
                    .replace("#number#", String.valueOf(i + 1))
                    .replace("#name#", playerData.getPlayerName())
                    .replace("#amount#", playerData.getCoinsPlayer().getCoins().toString())
                    .replace("#currency#", playerData.getCoinsPlayer().getFormattedCurrency()));
        }
        messages.addAll(LightCoins.instance.getMessageConfig().baltopFooter());

        LightCore.instance.getMessageSender().sendPlayerMessage(player, messages);
        return true;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
