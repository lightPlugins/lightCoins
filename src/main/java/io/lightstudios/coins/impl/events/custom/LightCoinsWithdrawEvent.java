package io.lightstudios.coins.impl.events.custom;

import lombok.Getter;
import lombok.Setter;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class LightCoinsWithdrawEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    private final String target;
    @Getter
    private BigDecimal amount;
    private boolean isCancelled;
    @Getter
    @Setter
    private EconomyResponse.ResponseType responseType;

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public LightCoinsWithdrawEvent(String target, BigDecimal amount) {
        this.target = target;
        this.amount = amount;
        this.responseType = EconomyResponse.ResponseType.NOT_IMPLEMENTED;
    }

    @FunctionalInterface
    public interface LightEconomyResponse {
        BigDecimal action(EconomyResponse.ResponseType status, BigDecimal amount);
    }

    public BigDecimal getAmount(LightCoinsDepositEvent.LightEconomyResponse action) {
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
