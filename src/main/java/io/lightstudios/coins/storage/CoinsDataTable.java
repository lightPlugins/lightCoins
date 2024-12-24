package io.lightstudios.coins.storage;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.database.model.DatabaseTypes;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class CoinsDataTable {
    private final String tableName = "lightcoins_coins";

    public CoinsDataTable() {
        LightCoins.instance.getConsolePrinter().printInfo("Initializing CoinsDataTable and creating Table...");
        createTable();
    }

    public CompletableFuture<List<CoinsData>> readCoinsData() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "SELECT uuid, name, coins FROM " + tableName;
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query);
                     ResultSet resultSet = statement.executeQuery()) {

                    List<CoinsData> coinsDataList = new ArrayList<>();
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                        String name = resultSet.getString("name");
                        BigDecimal coins = resultSet.getBigDecimal("coins");

                        CoinsData coinsPlayer = new CoinsData(uuid);
                        coinsPlayer.setCurrentCoins(coins);
                        coinsPlayer.setName(name);

                        coinsDataList.add(coinsPlayer);
                    }
                    return coinsDataList;
                } catch (Exception e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while reading player data from the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading player data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException(e);
        });
    }


    public CompletableFuture<CoinsData> findCoinsDataByUUID(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "SELECT uuid, name, coins FROM " + tableName + " WHERE uuid = ?";
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, id.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                            String name = resultSet.getString("name");
                            BigDecimal coins = resultSet.getBigDecimal("coins");

                            CoinsData coinsPlayer = new CoinsData(uuid);
                            coinsPlayer.setCurrentCoins(coins);
                            coinsPlayer.setName(name);
                            return coinsPlayer;
                        } else {
                            return null; // No player found with the given UUID
                        }
                    }
                } catch (Exception e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while reading player data from the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    throw new RuntimeException("An error occurred while reading player data from the database!", e);
                }
            }
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading player data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException(e);
        });
    }

    public CompletableFuture<Integer> writeCoinsData(CoinsData coinsData) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query;
                if (LightCore.instance.getSqlDatabase().getDatabaseType().equals(DatabaseTypes.SQLITE)) {
                    query = "INSERT OR REPLACE INTO " + tableName + " (uuid, name, coins) VALUES (?, ?, ?)";
                } else {
                    query = "INSERT INTO " + tableName + " (uuid, name, coins) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE coins = VALUES(coins)";
                }
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, coinsData.getUuid().toString());
                    statement.setString(2, coinsData.getName());
                    statement.setBigDecimal(3, coinsData.getCurrentCoins());
                    return statement.executeUpdate();
                } catch (Exception e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while creating a new player in the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    throw new RuntimeException("An error occurred while creating a new player in the database!", e);
                }
            }
        }).thenApply(result -> {
            if (result < 1) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "No rows were inserted in the database!",
                        "Please check the error logs for more information."
                ));
                throw new RuntimeException("No rows were inserted in the database!");
            }
            return result;
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while creating a new player in the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException(e);
        });
    }

    public CompletableFuture<Boolean> deleteAccount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "DELETE FROM " + tableName + " WHERE uuid = ?";
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, uuid.toString());
                    int result = statement.executeUpdate();
                    if (result < 1) {
                        LightCoins.instance.getConsolePrinter().printError(List.of(
                                "An error occurred while deleting account from the database!",
                                "Please check the error logs for more information."
                        ));
                        return false;
                    }
                    return true;
                } catch (SQLException e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while deleting account from the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    return false;
                }
            }
        });
    }

    public void createTable() {
        synchronized (this) {
            String query = createCoinsTable();
            LightCoins.instance.getConsolePrinter().printInfo("Creating coins table...");
            try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.executeUpdate();
                LightCoins.instance.getConsolePrinter().printInfo("Coins table created successfully!");
            } catch (SQLException e) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while creating the coins table!",
                        "Please check the error logs for more information.",
                        "Query: " + query
                ));
                e.printStackTrace();
            }
        }
    }

    private @NotNull String createCoinsTable() {

        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "uuid VARCHAR(36) NOT NULL UNIQUE, "
                + "name VARCHAR(36), "
                + "coins DECIMAL(65, 2), "
                + "PRIMARY KEY (uuid))";
    }

}
