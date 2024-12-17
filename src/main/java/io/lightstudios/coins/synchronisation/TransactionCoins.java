package io.lightstudios.coins.synchronisation;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.database.model.DatabaseTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.*;

public class TransactionCoins {

    private final ConcurrentLinkedQueue<Transaction> transactionQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss:SSS");

    public TransactionCoins() {
        scheduler.scheduleAtFixedRate(this::processTransactions, 500L, 500L, TimeUnit.MILLISECONDS);
    }

    public void addTransaction(UUID uuid, String name, BigDecimal amount) {
        String timestamp = LocalDateTime.now().format(formatter);
        transactionQueue.add(new Transaction(uuid, name, amount, timestamp));
    }

    private void processTransactions() {

        if(transactionQueue.isEmpty()) {
            return;
        }

        Transaction lastTransaction = null;
        // read the last transaction from the queue
        for (Transaction transaction : transactionQueue) {
            lastTransaction = transaction;
        }

        if (lastTransaction != null) {
            UUID uuid = lastTransaction.uuid();
            String name = lastTransaction.name();
            BigDecimal amount = lastTransaction.amount();
            String timestamp = lastTransaction.timestamp();
            // Write the last transaction to the database asynchronously
            CompletableFuture.runAsync(() -> {
                LightCoins.instance.getCoinsTable().writeCoins(uuid.toString(), name, amount);
                // Log the transaction
                LightCoins.instance.getConsolePrinter().printInfo(
                        "Processed [" + timestamp + "] transaction for " + uuid + ": " + amount);
            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError("Failed to process last transaction for " + uuid);
                throwable.printStackTrace();
                return null;
            });
        }

        LightCoins.instance.getConsolePrinter().printInfo("Cleared transaction queue, waiting for next transactions");
        transactionQueue.clear();
    }

    private record Transaction(UUID uuid, String name, BigDecimal amount, String timestamp) {

    }
}