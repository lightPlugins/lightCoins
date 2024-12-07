package io.lightstudios.coins.storage;

import io.lightstudios.core.LightCore;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
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

    public CompletableFuture<Map<UUID, BigDecimal>> readCoins(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "SELECT * FROM " + tableName + " WHERE uuid = '" + uuid + "'";
                return LightCore.instance.getSqlDatabase().querySqlFuture(query, "uuid", "coins");
            }
        }).thenCompose(future -> future).thenApply(result -> {
            if (result == null) {
                LightCore.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while reading coins from the database!",
                        "Please check the error logs for more information."
                ));
                throw new RuntimeException("An error occurred while reading coins from the database!");
            } else {
                LightCore.instance.getConsolePrinter().printInfo("Coins read from the database successfully!");
            }

            return result.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> UUID.fromString((String) ((Map.Entry<?, ?>) e).getKey()),
                            e -> (BigDecimal) ((Map.Entry<?, ?>) e).getValue()
                    ));
        });
    }

    public CompletableFuture<Integer> writeCoins(String uuid, BigDecimal coins) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "INSERT INTO " + tableName + " (uuid, coins) VALUES ('" + uuid + "', " + coins.doubleValue() + ") ON DUPLICATE KEY UPDATE coins = " + coins;
                return LightCore.instance.getSqlDatabase().executeSqlFuture(query);
            }
        }).thenCompose(future -> future).thenApply(result -> {
            if (result < 1) {
                LightCore.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while writing coins to the database!",
                        "Please check the error logs for more information."
                ));
                throw new RuntimeException("An error occurred while writing coins to the database!");
            }
            LightCore.instance.getConsolePrinter().printInfo("Coins written to the database successfully!");
            return result;
        });
    }

    public void createTable() {
        CompletableFuture.runAsync(() -> {
            synchronized (this) {
                String query = createCoinsTable();
                try {
                    LightCore.instance.getSqlDatabase().executeSqlFuture(query).thenAccept(result -> {
                        if (result == null) {
                            LightCore.instance.getConsolePrinter().printError(List.of(
                                    "An error occurred while creating the coins table!",
                                    "Please check the error logs for more information.",
                                    "Query: " + query
                            ));
                            throw new RuntimeException("An error occurred while creating the coins table!");
                        }
                        LightCore.instance.getConsolePrinter().printInfo("Coins table created successfully!");
                    });
                } catch (Exception e) {
                    LightCore.instance.getConsolePrinter().printError(List.of(
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
                + "coins DECIMAL(65, 2), "
                + "PRIMARY KEY (uuid))";
    }

}
