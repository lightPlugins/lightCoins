package io.lightstudios.coins.commands.vault.defaults;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.LightTimers;
import io.lightstudios.core.util.interfaces.LightCommand;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PayCommand implements LightCommand {

    private final List<Player> cooldown = new ArrayList<>();

    @Override
    public List<String> getSubcommand() {
        return List.of();
    }

    @Override
    public String getDescription() {
        return "Send coins to another player";
    }

    @Override
    public String getSyntax() {
        return "/pay <player> <amount>";
    }

    @Override
    public int maxArgs() {
        return 2;
    }

    @Override
    public String getPermission() {
        return LightPermissions.PAY_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (commandSender, command, alias, args) -> {
            if (args.length == 1) {
                return Bukkit.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
            }
            return null;
        };
    }

    @Override
    public boolean performAsPlayer(Player player, String[] args) {

        if(args.length != 2) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().wrongSyntax().stream().map(s ->
                                    s.replace("#syntax#", getSyntax())
                            ).collect(Collectors.joining()));
            return false;
        }

        if (cooldown.contains(player)) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().payCooldown().stream().map(s ->
                                    s.replace("#time#", "5")
                            ).collect(Collectors.joining()));
            return false;
        }
        Player target = Bukkit.getServer().getPlayer(args[0]);

        if(target == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().payOnlyOnlinePlayer().stream().map(s ->
                                    s.replace("#target#", args[0])
                            ).collect(Collectors.joining()));
            return false;
        }

        AccountData targetData = LightCoins.instance.getLightCoinsAPI().getPlayerData(player);
        AccountData playerData = LightCoins.instance.getLightCoinsAPI().getPlayerData(player);

        if(playerData == null || targetData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(s ->
                                    s.replace("#info#", "Could not find PlayerData for " + args[0])
                            ).collect(Collectors.joining()));
            return false;
        }

        BigDecimal amount = LightNumbers.parseMoney(args[1]);

        if(amount == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            String.join("", LightCoins.instance.getMessageConfig().noNumber()));
            return false;
        }

        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().noNegativ());
            return false;
        }

        CoinsData coinsPlayer = playerData.getCoinsData();
        CoinsData targetCoinsPlayer = targetData.getCoinsData();

        EconomyResponse playerResponse = coinsPlayer.removeCoins(amount);
        EconomyResponse targetResponse = targetCoinsPlayer.addCoins(amount);

        if(playerResponse.transactionSuccess() && targetResponse.transactionSuccess()) {

            cooldown.add(player);
            LightTimers.doSync((task) -> cooldown.remove(player), 5 * 20L);

            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().pay().stream().map(s -> s
                                    .replace("#coins#", LightNumbers.formatForMessages(amount, 2))
                                    .replace("#currency#", amount.compareTo(BigDecimal.ONE) == 0 ?
                                            coinsPlayer.getNameSingular() : coinsPlayer.getNamePlural())
                                    .replace("#target#", target.getName())
                            ).collect(Collectors.joining()));


                if(target.isOnline()) {
                    LightCore.instance.getMessageSender().sendPlayerMessage(
                            target.getPlayer(),
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().payTarget().stream().map(s -> s
                                            .replace("#coins#", LightNumbers.formatForMessages(amount, 2))
                                            .replace("#currency#", amount.compareTo(BigDecimal.ONE) == 0 ?
                                                    coinsPlayer.getNameSingular() : coinsPlayer.getNamePlural())
                                            .replace("#target#", player.getName())
                                    ).collect(Collectors.joining()));

                    return true;
                } else {
                    // dummy check for Velocity compatibility
                    // search for the Proxy player and send them a message
                }

            return false;


        }

        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        LightCoins.instance.getConsolePrinter().printError("This command can only be executed by a player");
        return false;
    }
}
