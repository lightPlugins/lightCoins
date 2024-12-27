package io.lightstudios.coins.commands.transfer;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.storage.LightEconomyTable;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightTimers;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

public class TransferCommand implements LightCommand {

    private int step = 0;

    @Override
    public List<String> getSubcommand() {
        return List.of("transfer");
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getSyntax() {
        // sqlite: -> '/lightcoins transfer lighteconomy sqlite databaseName start'
        // mysql: -> '/lightcoins transfer lighteconomy mysql <host> <port> <username> <password> <databaseName> start'
        return "/lightcoins transfer lighteconomy mysql <host> <port> <username> <password> <databaseName> start";
    }

    @Override
    public int maxArgs() {
        return 8;
    }

    @Override
    public String getPermission() {
        return "lightcoins.transfer.via.console";
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return null;
    }

    @Override
    public boolean performAsPlayer(Player player, String[] args) {

        LightCore.instance.getMessageSender().sendPlayerMessage(
                player,
                LightCoins.instance.getMessageConfig().prefix() +
                        String.join("\n", LightCoins.instance.getMessageConfig().onlyConsole()));
        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] args) {

        if (step != 0) {
            LightCoins.instance.getConsolePrinter().printError("Transfer already in progress.");
            return false;
        }

        if(!args[1].equalsIgnoreCase("lighteconomy")) {
            LightCoins.instance.getConsolePrinter().printError("You can only transfer data from LightEconomy.");
            return false;
        }

        LightEconomyTable oldEconomyTable = new LightEconomyTable();

        if(args[2].equalsIgnoreCase("sqlite")) {
            try {
                oldEconomyTable.connectViaSQLite();
            } catch (SQLException e) {
                e.printStackTrace();
                LightCoins.instance.getConsolePrinter().printError("Failed to connect to the SQLite database.");
                return false;
            }
        }

        if(args[2].equalsIgnoreCase("mysql")) {

            if(args.length != 8) {
                LightCoins.instance.getConsolePrinter().printError("Invalid arguments. Please check the syntax. " + getSyntax());
                return false;
            }

            String host = args[3];
            String port = args[4];
            String username = args[5];
            String password = args[6];
            String databaseName = args[7];

            LightCoins.instance.getConsolePrinter().printInfo(List.of(
                    "Starting transfer from LightEconomy to LightCoins with MySQL database:",
                    "Host: " + host,
                    "Port: " + port,
                    "Username: " + username,
                    "Password: " + password,
                    "Database Name: " + databaseName
            ));

            // start the transfer
            try {
                step = 1; // connecting to the database
                LightCoins.instance.getConsolePrinter().printInfo("Connecting to the target MySQL database...");
                oldEconomyTable.connectViaMYSQL(host, port, databaseName, username, password);
            } catch (SQLException e) {
                e.printStackTrace();
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "Failed to connect to the MySQL database.",
                        "Please check your database configuration.",
                        "Error: " + e.getMessage(),
                        "Step: " + step
                ));
                return false;
            }
        }

        try {
            step = 2; // reading data from the database
            LightCoins.instance.getConsolePrinter().printInfo("Reading data from LightEconomy 'MoneyTable'...");
            oldEconomyTable.readMoneyTable();
            step = 3; // transfer completed
            oldEconomyTable.disconnect();
            LightCoins.instance.getConsolePrinter().printInfo(List.of(
                    "Transfer completed successfully.",
                    "If all Data is correct, you can now delete the LightEconomy plugin.",
                    "The Server will be restarted in few seconds ..."
            ));
            LightTimers.startTaskWithCounter((task, count) -> {
                LightCoins.instance.getConsolePrinter().printInfo("§cServer will be restarted in §4" + (10 - count) + "§c seconds.");
                if (count == 10) {
                    task.cancel();
                    LightCoins.instance.getConsolePrinter().printInfo(List.of(
                            "Server is restarting...",
                            "Please wait..."
                    ));
                    Bukkit.getServer().shutdown();
                }
            }, 20L, 20L);
        } catch (SQLException e) {
            e.printStackTrace();
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "Failed to connect to the MySQL database.",
                    "Please check your database configuration.",
                    "Error: " + e.getMessage(),
                    "Step: " + step
            ));
            return false;

        }

        return false;
    }
}
