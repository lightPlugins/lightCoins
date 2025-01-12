package io.lightstudios.coins.synchronisation;

import io.lightstudios.coins.LightCoins;
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
            UUID uuid = lastTransaction.virtualData.getPlayerUUID();
            BigDecimal amount = lastTransaction.virtualData.getCurrentBalance();
            String timestamp = lastTransaction.timestamp();

            // Create a CoinsData object
            // VirtualData virtualData = new VirtualData(file, uuid);
            // virtualData.setPlayerName(name);
            // virtualData.setBalance(amount);

            // Write the last transaction to the database asynchronously
            Transaction finalLastTransaction = lastTransaction;
            CompletableFuture.runAsync(() -> {
                LightCoins.instance.getVirtualDataTable().writeVirtualData(finalLastTransaction.virtualData).thenAccept(result -> {
                    if (result > 0) {
                        if(LightCoins.instance.getSettingsConfig().enableDebugMultiSync()) {
                            LightCoins.instance.getConsolePrinter().printInfo(
                                    "Processed [" + timestamp + "] virtual transaction for " + uuid + ": " + amount);
                        }
                    } else {
                        LightCoins.instance.getConsolePrinter().printError(
                                "Failed [" + timestamp + "] virtual transaction for " + uuid + ": " + amount);
                    }
                    // Remove the processed transaction from the queue
                    transactionQueue.remove(finalLastTransaction);
                }).exceptionally(throwable -> {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "Failed to write last transaction for " + uuid,
                            "Amount: " + amount,
                            "Timestamp: " + timestamp));
                    throwable.printStackTrace();
                    transactionQueue.remove(finalLastTransaction);
                    return null;
                });
            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError("Failed to process last virtual transaction for " + uuid);
                throwable.printStackTrace();
                transactionQueue.remove(finalLastTransaction);
                return null;
            });
        }
    }

    private record Transaction(VirtualData virtualData, String timestamp) {

    }
}