package io.lightstudios.coins.storage;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class LightEconomyTable {

    private Connection connection;

    public void connectViaMYSQL(String host, String port, String database, String username, String password) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
        connection = DriverManager.getConnection(url, username, password);
    }

    public void connectViaSQLite() throws SQLException {
        String path = LightCoins.instance.getDataFolder().getPath() + File.separator + "lightEconomy.db";
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void readMoneyTable() {
        String query = "SELECT * FROM MoneyTable";
        int count = 0;
        AtomicInteger failed = new AtomicInteger();
        float start = System.currentTimeMillis();
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String uuid = resultSet.getString("uuid");
                String name = resultSet.getString("name");
                double money = resultSet.getDouble("money");
                boolean isPlayer = resultSet.getBoolean("isPlayer");

                String userName = name;
                UUID accountUUID = UUID.fromString(uuid);

                if(!isPlayer) {
                    userName = "nonplayer_account";
                    accountUUID = LightCore.instance.getHookManager().getTownyInterface().getTownyObjectUUID(name);
                }

                if(accountUUID == null) {
                    failed.getAndIncrement();
                    LightCoins.instance.getConsolePrinter().printError("Could not found Towny uuid for town: " + name + " -> skipping ...");
                    continue;
                }

                count++;
                LightCoins.instance.getConsolePrinter().printInfo("Found economy data from LightEconomy: " + name + " (" + accountUUID + ")");
                CoinsData coinsData = new CoinsData(accountUUID);
                coinsData.setName(userName);
                coinsData.setCurrentCoins(LightNumbers.convertToBigDecimal(money));

                LightCoins.instance.getCoinsTable().writeCoinsData(coinsData).thenAccept(result -> {
                    if (result > 0) {
                        LightCoins.instance.getConsolePrinter().printInfo("Successfully transferred user " + name + " to LightCoins.");
                    } else {
                        failed.getAndIncrement();
                        LightCoins.instance.getConsolePrinter().printError("Failed transferred user " + name + " to LightCoins.");
                    }
                }).join();
            }

            float end = System.currentTimeMillis();
            LightCoins.instance.getConsolePrinter().printInfo(
                    "Successfully transferred " + count + " economy accounts to LightCoins. Failed: " + failed.get() + ". Took " + (end - start) + "ms.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
