package io.lightstudios.coins.impl.events.custom;

import lombok.Getter;
import lombok.Setter;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class LightCoinsDepositEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    private final String uuid;
    @Getter
    private final BigDecimal newBalance;
    @Getter
    private BigDecimal amount;
    private boolean isCancelled;
    @Getter
    @Setter
    private EconomyResponse.ResponseType responseType;

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public LightCoinsDepositEvent(String uuid, BigDecimal amount, BigDecimal newBalance) {
        this.uuid = uuid;
        this.amount = amount;
        this.newBalance = newBalance;
        this.responseType = EconomyResponse.ResponseType.NOT_IMPLEMENTED;
    }

    @FunctionalInterface
    public interface LightEconomyResponse {
        BigDecimal action(EconomyResponse.ResponseType status, BigDecimal amount);
    }

    public BigDecimal getAmount(LightEconomyResponse action) {
        return action.action(this.responseType, this.amount);
    }

    public BigDecimal setAmount(BigDecimal newAmount) {
        this.amount = newAmount;
        return this.amount;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}