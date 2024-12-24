package io.lightstudios.coins.storage;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.database.model.DatabaseTypes;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VirtualDataTable {

    private final String tableName = "lightcoins_virtual";

    public VirtualDataTable() {
        createTable();
    }

    public CompletableFuture<List<VirtualData>> readVirtualData() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "SELECT uuid, name, currencyName, balance FROM " + tableName;
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query);
                     ResultSet resultSet = statement.executeQuery()) {

                    List<VirtualData> virtualDataList = new ArrayList<>();
                    float start = System.currentTimeMillis();
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                        String name = resultSet.getString("name");
                        String currencyName = resultSet.getString("currencyName");
                        BigDecimal balance = resultSet.getBigDecimal("balance");

                        File file = LightCoins.instance.getVirtualCurrencyFiles().getYamlFiles().stream().filter(
                                f -> f.getName().replace(".yml", "").equals(currencyName)).findFirst().orElse(null);

                        if(file == null) {
                            LightCoins.instance.getConsolePrinter().printError(List.of(
                                    "An error occurred while reading virtual data from the database!",
                                    "The currency file for " + currencyName + " was not found!"
                            ));
                            continue;
                        }

                        VirtualData virtualData = new VirtualData(file, uuid);
                        virtualData.setCurrentBalance(balance);
                        virtualData.setPlayerName(name);
                        virtualDataList.add(virtualData);
                    }

                    float end = System.currentTimeMillis();
                    LightCoins.instance.getConsolePrinter().printInfo("Virtual data read in " + (end - start) + "ms");

                    return virtualDataList;
                } catch (Exception e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while reading virtual data from the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    return null;
                }
            }
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading virtual data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            return null;
        });
    }

    public CompletableFuture<List<VirtualData>> findVirtualDataByUUID(UUID id) {

        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "SELECT name, currencyName, balance FROM " + tableName + " WHERE uuid = ?";
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, id.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        List<VirtualData> virtualDataList = new ArrayList<>();
                        while (resultSet.next()) {
                            String name = resultSet.getString("name");
                            String currencyName = resultSet.getString("currencyName");
                            BigDecimal balance = resultSet.getBigDecimal("balance");

                            File file = LightCoins.instance.getVirtualCurrencyFiles().getYamlFiles().stream().filter(
                                    f -> f.getName().replace(".yml", "").equals(currencyName)).findFirst().orElse(null);

                            if(file == null) {
                                LightCoins.instance.getConsolePrinter().printError(List.of(
                                        "An error occurred while reading virtual data from the database!",
                                        "The currency file for " + currencyName + " was not found!"
                                ));
                                continue;
                            }

                            VirtualData virtualData = new VirtualData(file, id);
                            virtualData.setCurrentBalance(balance);
                            virtualData.setPlayerName(name);
                            virtualDataList.add(virtualData);
                        }
                        return virtualDataList;
                    }
                } catch (Exception e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while reading virtual data from the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    return null;
                }
            }
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading virtual data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            return null;
        });

    }

    public CompletableFuture<Integer> writeVirtualData(VirtualData virtualData) {

        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = getWriteVirtualQuery();
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, virtualData.getPlayerUUID().toString());
                    statement.setString(2, virtualData.getPlayerName());
                    statement.setString(3, virtualData.getCurrencyName());
                    statement.setBigDecimal(4, virtualData.getCurrentBalance());
                    return statement.executeUpdate();
                } catch (Exception e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while writing virtual data to the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    return 0;
                }
            }
        }).thenApply(result -> {
            if (result == 0) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while writing virtual data to the database!",
                        "Please check the error logs for more information."
                ));
            }
            return result;
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while writing virtual data to the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            return 0;
        });
    }

    public CompletableFuture<Boolean> deleteVirtualData(UUID uuid, String currencyName) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "DELETE FROM " + tableName + " WHERE uuid = ? AND currencyName = ?";
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, uuid.toString());
                    statement.setString(2, currencyName);
                    int result = statement.executeUpdate();
                    if (result < 1) {
                        LightCoins.instance.getConsolePrinter().printError(List.of(
                                "An error occurred while deleting virtual data from the database!",
                                "Please check the error logs for more information."
                        ));
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while deleting virtual data from the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    return false;
                }
            }
        }).thenApply(result -> {
            if (!result) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while deleting virtual data from the database!",
                        "Please check the error logs for more information."
                ));
            }
            return result;
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while deleting virtual data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            return false;
        });
    }


    public void createTable() {
        String query = createVirtualTable();
        LightCoins.instance.getConsolePrinter().printInfo("Creating virtual table...");
        try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.executeUpdate();
            LightCoins.instance.getConsolePrinter().printInfo("Virtual table created successfully!");
        } catch (SQLException e) {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while creating the virtual table!",
                    "Please check the error logs for more information.",
                    "Query: " + query
            ));
            e.printStackTrace();
        }
    }

    private @NotNull String createVirtualTable() {
        if (LightCore.instance.getSqlDatabase().getDatabaseType().equals(DatabaseTypes.SQLITE)) {
            return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "name VARCHAR(64), "
                    + "currencyName VARCHAR(64) NOT NULL, "
                    + "balance DECIMAL(65, 2), "
                    + "PRIMARY KEY (uuid, currencyName))";
        } else {
            return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "name VARCHAR(64), "
                    + "currencyName VARCHAR(64) NOT NULL, "
                    + "balance DECIMAL(65, 2), "
                    + "PRIMARY KEY (uuid, currencyName), "
                    + "UNIQUE (uuid, currencyName))";
        }
    }

    private @NotNull String getWriteVirtualQuery() {
        String query;
        if (LightCore.instance.getSqlDatabase().getDatabaseType().equals(DatabaseTypes.SQLITE)) {
            query = "INSERT OR REPLACE INTO " + tableName + " (uuid, name, currencyName, balance) VALUES (?, ?, ?, ?)";
        } else {
            query = "INSERT INTO " + tableName + " (uuid, name, currencyName, balance) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE name = VALUES(name), balance = VALUES(balance)";
        }
        return query;
    }

}
