package io.lightstudios.coins.impl.events;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.impl.events.custom.LightCoinsDepositEvent;
import io.lightstudios.coins.impl.events.custom.LightCoinsWithdrawEvent;
import io.lightstudios.core.player.title.countupdown.AnimatedCountTitle;
import io.lightstudios.core.player.title.countupdown.AnimatedCountTitleSettings;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.math.BigDecimal;

public class SendTitlesOnEvents implements Listener {


    @EventHandler
    public void onDeposit(LightCoinsDepositEvent event) {

        Player player = Bukkit.getPlayer(event.getUuid());
        BigDecimal amount = event.getAmount();
        BigDecimal newBalance = event.getNewBalance();

        if(!event.getResponseType().equals(EconomyResponse.ResponseType.SUCCESS)) {
            return;
        }

        if(event.isCancelled()) {
            return;
        }

        if(player == null) {
            return;
        }

        AnimatedCountTitle title = LightCoins.instance.getTitleConfig().getAnimatedCountTitle();
        AnimatedCountTitleSettings depositSettings = LightCoins.instance.getTitleConfig().getTitleSettings().get("onDeposit");

        title.sendCountUpTitle(player, amount, newBalance, 20, depositSettings);

    }

    @EventHandler
    public void onWithdraw(LightCoinsWithdrawEvent event) {


    }


}
