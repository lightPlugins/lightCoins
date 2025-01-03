package io.lightstudios.coins.synchronisation;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Getter
@Setter
public class TransactionCoins {

    private int poolSize = 1;
    private long period = 500L;
    private long delay = 500L;

    private final ConcurrentLinkedQueue<Transaction> transactionQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(poolSize);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss:SSS");

    public void startTransactions() {
        scheduler.scheduleAtFixedRate(this::processTransactions, delay, period, TimeUnit.MILLISECONDS);
    }

    public void addTransaction(CoinsData coinsData) {
        String timestamp = LocalDateTime.now().format(formatter);
        transactionQueue.add(new Transaction(coinsData, timestamp));
    }

    private void processTransactions() {
        if (transactionQueue.isEmpty()) {
            return;
        }

        Transaction lastTransaction = null;
        // read the last transaction from the queue
        for (Transaction transaction : transactionQueue) {
            lastTransaction = transaction;
        }

        if (lastTransaction != null) {
            UUID uuid = lastTransaction.coinsData.getUuid();
            String name = lastTransaction.coinsData.getName();
            BigDecimal amount = lastTransaction.coinsData.getCurrentCoins();
            String timestamp = lastTransaction.timestamp();

            // Create a CoinsData object
            CoinsData coinsData = new CoinsData(uuid);
            coinsData.setName(name);
            coinsData.setCoins(amount);

            // Write the last transaction to the database asynchronously
            CompletableFuture.runAsync(() -> {
                LightCoins.instance.getCoinsTable().writeCoinsData(coinsData).thenAccept(result -> {
                    if (result > 0) {
                        if(LightCoins.instance.getSettingsConfig().enableDebugMultiSync()) {
                            LightCoins.instance.getConsolePrinter().printInfo(
                                    "Processed [" + timestamp + "] transaction for " + uuid + ": " + amount);
                        }

                    } else {
                        LightCoins.instance.getConsolePrinter().printError(
                                "Failed [" + timestamp + "] transaction for " + uuid + ": " + amount);
                    }
                }).exceptionally(throwable -> {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "Failed to write last transaction for " + uuid,
                            "Amount: " + amount,
                            "Timestamp: " + timestamp));
                    throwable.printStackTrace();
                    return null;
                });

            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError("Failed to process last transaction for " + uuid);
                throwable.printStackTrace();
                return null;
            });
        }

        transactionQueue.clear();
    }

    private record Transaction(CoinsData coinsData, String timestamp) {

    }
}