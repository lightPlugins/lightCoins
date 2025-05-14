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

    private final BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>(1000);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss:SSS");

    public void startTransactions() {
        LightCoins.instance.getScheduler().scheduleAtFixedRate(this::processTransactions, delay, period, TimeUnit.MILLISECONDS);
    }

    public void addTransaction(CoinsData coinsData) {
        String timestamp = LocalDateTime.now().format(formatter);

        monitorQueue();

        if (!transactionQueue.offer(new Transaction(coinsData, timestamp))) {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "Transaction queue for Coins is full! Could not add transaction.",
                    "Consider increasing the pool size or reducing the transaction frequency.",
                    "Transaction Timestamp: " + timestamp,
                    "For UUID: " + coinsData.getUuid(),
                    "Final Amount: " + coinsData.getCurrentCoins()
            ));
        }
    }

    private synchronized void processTransactions() {
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
            BigDecimal amount = lastTransaction.coinsData.getCurrentCoins();
            String timestamp = lastTransaction.timestamp();

            // Create a CoinsData object
            // CoinsData coinsData = new CoinsData(uuid);
            // coinsData.setName(name);
            // coinsData.setCoins(amount);

            // Write the last transaction to the database asynchronously
            Transaction finalLastTransaction = lastTransaction;
            CompletableFuture.runAsync(() -> {
                LightCoins.instance.getCoinsTable().writeCoinsData(finalLastTransaction.coinsData).thenAccept(result -> {
                    if (result > 0) {
                        if(LightCoins.instance.getSettingsConfig().enableDebugMultiSync()) {
                            LightCoins.instance.getConsolePrinter().printInfo(
                                    "Processed [" + timestamp + "] vault transaction for " + uuid + ": " + amount);
                        }
                    } else {
                        LightCoins.instance.getConsolePrinter().printError(
                                "Failed [" + timestamp + "] vault transaction for " + uuid + ": " + amount);
                    }
                    // Remove the processed transaction from the queue
                    if(!transactionQueue.remove(finalLastTransaction)) {
                        LightCoins.instance.getConsolePrinter().printWarning("Could not remove transaction from queue: " + finalLastTransaction.timestamp);
                    }
                }).exceptionally(throwable -> {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "Failed to write last vault transaction for " + uuid,
                            "Amount: " + amount,
                            "Timestamp: " + timestamp));
                    if(!transactionQueue.remove(finalLastTransaction)) {
                        LightCoins.instance.getConsolePrinter().printWarning(List.of(
                                "Something went wrong on writeCoinsData to Database.",
                                "CompletionException: Could not remove transaction from queue",
                                "Transaction Timestamp: " + finalLastTransaction.timestamp,
                                "Error: " + throwable.getMessage()
                        ));
                    }
                    throwable.printStackTrace();
                    return null;
                });
            }, LightCoins.instance.getExecutor()).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError("Failed to process last vault transaction for " + uuid);
                if(!transactionQueue.remove(finalLastTransaction)) {
                    LightCoins.instance.getConsolePrinter().printWarning(List.of(
                            "Something went wrong on processing processTransactions.",
                            "CompletionException: Could not remove transaction from queue",
                            "Transaction Timestamp: " + finalLastTransaction.timestamp,
                            "Error: " + throwable.getMessage()
                    ));
                }
                throwable.printStackTrace();
                return null;
            });
        }
    }

    private record Transaction(CoinsData coinsData, String timestamp) {

    }

    public void monitorQueue() {
        LightCoins.instance.getConsolePrinter().printInfo("Transaction queue size: " + transactionQueue.size());

        if(transactionQueue.remainingCapacity() < 800) {
            LightCoins.instance.getConsolePrinter().printInfo("Current Transaction queue size: " + transactionQueue.size());
        }

        if (transactionQueue.remainingCapacity() < 500) {
            LightCoins.instance.getConsolePrinter().printWarning(List.of(
                    "Transaction queue for Coins is getting full! Remaining capacity: " + transactionQueue.remainingCapacity() + " / 1000",
                    "Consider increasing the pool size or reducing the transaction frequency."
            ));
        }
        if (transactionQueue.remainingCapacity() < 15) {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "Transaction queue for Coins is almost full! Remaining capacity: " + transactionQueue.remainingCapacity() + " / 1000",
                    "Consider increasing the pool size or reducing the transaction frequency."
            ));
        }

    }
}