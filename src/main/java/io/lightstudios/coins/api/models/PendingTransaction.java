package io.lightstudios.coins.api.models;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class PendingTransaction {
    private final UUID sender;
    private final String receiverName;
    private final BigDecimal amount;

    public PendingTransaction(UUID sender, String receiverName, BigDecimal amount) {
        this.sender = sender;
        this.receiverName = receiverName;
        this.amount = amount;
    }
}