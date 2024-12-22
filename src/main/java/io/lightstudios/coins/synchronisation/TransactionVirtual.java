package io.lightstudios.coins.synchronisation;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.VirtualData;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Getter
@Setter
public class TransactionVirtual {

    private int poolSize = 1;
    private long period = 500L;
    private long delay = 500L;

    private final ConcurrentLinkedQueue<Transaction> transactionQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(poolSize);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss:SSS");

    public void startTransactions() {
        scheduler.scheduleAtFixedRate(this::processTransactions, delay, period, TimeUnit.MILLISECONDS);
    }

    public void addTransaction(VirtualData virtualData) {
        String timestamp = LocalDateTime.now().format(formatter);
        transactionQueue.add(new Transaction(virtualData, timestamp));
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
            UUID uuid = lastTransaction.virtualData.getPlayerUUID();
            String name = lastTransaction.virtualData.getPlayerName();
            BigDecimal amount = lastTransaction.virtualData.getBalance();
            String timestamp = lastTransaction.timestamp();
            File file = lastTransaction.virtualData.getFile();

            // Create a CoinsData object
            VirtualData virtualData = new VirtualData(file, uuid);
            virtualData.setPlayerName(name);
            virtualData.setBalance(amount);

            // Write the last transaction to the database asynchronously
            CompletableFuture.runAsync(() -> {
                LightCoins.instance.getVirtualDataTable().writeVirtualData(virtualData).thenAccept(result -> {
                    if (result > 0) {
                        LightCoins.instance.getConsolePrinter().printInfo(
                                "Processed [" + timestamp + "] transaction for " + uuid + ": " + amount);
                        return;
                    } else {
                        LightCoins.instance.getConsolePrinter().printError(
                                "Failed [" + timestamp + "] transaction for " + uuid + ": " + amount);
                    }
                    return;
                }).exceptionally(throwable -> {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "Failed to write last transaction for " + uuid,
                            "Amount: " + amount,
                            "Timestamp: " + timestamp));
                    throwable.printStackTrace();
                    return null;
                });
                // Log the transaction
                LightCoins.instance.getConsolePrinter().printDebug(
                        "Processed [" + timestamp + "] transaction for " + uuid + ": " + amount);
            }).thenAccept(result -> {
                LightCoins.instance.getConsolePrinter().printDebug("Cleared transaction queue, waiting for next transactions");
            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError("Failed to process last transaction for " + uuid);
                throwable.printStackTrace();
                return null;
            });
        }


        transactionQueue.clear();
    }

    private record Transaction(VirtualData virtualData, String timestamp) {

    }
}