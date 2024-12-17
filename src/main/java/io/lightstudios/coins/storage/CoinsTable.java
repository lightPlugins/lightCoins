package io.lightstudios.coins.storage;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.database.model.DatabaseTypes;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CoinsTable {

    private final String tableName = "lightcoins_coins";

    public CoinsTable() {
        createTable();
    }

    public CompletableFuture<HashMap<UUID, CoinsPlayer>> readExistingCoinsPlayer() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "SELECT * FROM " + tableName;
                return LightCore.instance.getSqlDatabase().querySqlFuture(query, "uuid", "coins");
            }
        }).thenCompose(future -> future).thenApply(result -> {
            if (result == null) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while reading player data from the database!",
                        "Please check the error logs for more information."
                ));
                throw new RuntimeException("An error occurred while reading player data from the database!");
            } else {
                LightCoins.instance.getConsolePrinter().printInfo("Player data read from the database successfully!");
            }

            return result.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> UUID.fromString((String) ((Map.Entry<?, ?>) e).getKey()),
                            e -> {
                                Object value = ((Map.Entry<?, ?>) e).getValue();
                                BigDecimal coins;
                                if (value instanceof Integer) {
                                    coins = BigDecimal.valueOf((Integer) value);
                                } else {
                                    coins = (BigDecimal) value;
                                }
                                CoinsPlayer coinsPlayer = new CoinsPlayer(UUID.fromString((String) ((Map.Entry<?, ?>) e).getKey()));
                                coinsPlayer.setCoins(coins);
                                return coinsPlayer;
                            },
                            (existing, replacement) -> existing,
                            HashMap::new
                    ));
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading player data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException("An error occurred while reading player data from the database!");
        });
    }

    public CompletableFuture<Map<UUID, BigDecimal>> readCoins(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "SELECT * FROM " + tableName + " WHERE uuid = '" + uuid + "'";
                return LightCore.instance.getSqlDatabase().querySqlFuture(query, "uuid", "coins");
            }
        }).thenCompose(future -> future).thenApply(result -> {
            if (result == null) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while reading coins from the database!",
                        "Please check the error logs for more information."
                ));
                throw new RuntimeException("An error occurred while reading coins from the database!");
            }

            return result.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> UUID.fromString((String) ((Map.Entry<?, ?>) e).getKey()),
                            e -> (BigDecimal) ((Map.Entry<?, ?>) e).getValue()
                    ));
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading player data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException("An error occurred while reading player data from the database!");
        });
    }

    public CompletableFuture<Integer> writeCoins(String uuid, String name, BigDecimal coins) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query;
                if(LightCore.instance.getSqlDatabase().getDatabaseType().equals(DatabaseTypes.SQLITE)) {
                    query = "INSERT OR REPLACE INTO " + tableName + " (uuid, coins) VALUES ('" + uuid + "', " + coins.doubleValue() + ")";
                } else {
                    query = "INSERT INTO " + tableName + " (uuid, coins) VALUES ('" + uuid + "', " + coins.doubleValue() + ") ON DUPLICATE KEY UPDATE coins = " + coins;
                }

                return LightCore.instance.getSqlDatabase().executeSqlFuture(query);
            }
        }).thenCompose(future -> future).thenApply(result -> {
            if (result < 1) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while writing coins to the database!",
                        "Please check the error logs for more information."
                ));
                throw new RuntimeException("An error occurred while writing coins to the database!");
            }
            return result;
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading player data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException("An error occurred while reading player data from the database!");
        });
    }

    public void createTable() {
        CompletableFuture.runAsync(() -> {
            synchronized (this) {
                String query = createCoinsTable();
                try {
                    LightCore.instance.getSqlDatabase().executeSqlFuture(query).thenAccept(result -> {
                        if (result == null) {
                            LightCoins.instance.getConsolePrinter().printError(List.of(
                                    "An error occurred while creating the coins table!",
                                    "Please check the error logs for more information.",
                                    "Query: " + query
                            ));
                            throw new RuntimeException("An error occurred while creating the coins table!");
                        }
                        LightCoins.instance.getConsolePrinter().printInfo("Coins table created successfully!");
                    }).exceptionally(e -> {
                        LightCoins.instance.getConsolePrinter().printError(List.of(
                                "An error occurred while reading player data from the database!",
                                "Please check the error logs for more information."
                        ));
                        e.printStackTrace();
                        throw new RuntimeException("An error occurred while reading player data from the database!");
                    });;
                } catch (Exception e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while creating the coins table!",
                            "Please check the error logs for more information.",
                            "Query: " + query
                    ));
                    throw new RuntimeException("An error occurred while creating the coins table!");
                }
            }
        });
    }

    private @NotNull String createCoinsTable() {

        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "uuid VARCHAR(36) NOT NULL UNIQUE, "
                + "name VARCHAR(36), "
                + "coins DECIMAL(65, 2), "
                + "PRIMARY KEY (uuid))";
    }

}
