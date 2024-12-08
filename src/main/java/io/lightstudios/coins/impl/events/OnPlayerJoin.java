package io.lightstudios.coins.impl.events;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.core.LightCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class OnPlayerJoin implements Listener {

    // ensure the player data is loaded before the player joins the server
    // and other plugins can access the player data from the LightCoinsAPI
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (LightCoins.instance.getLightCoinsAPI().getPlayerData().containsKey(uuid)) {
            LightCore.instance.getConsolePrinter().printInfo("Player data already exists for " + uuid);
            return;
        }

        LightCoins.instance.getCoinsTable().readCoins(uuid.toString()).thenAcceptAsync(result -> {
            if (result.isEmpty()) {
                if (!LightCoins.instance.getVaultImplementer().createPlayerAccount(uuid.toString())) {
                    LightCore.instance.getConsolePrinter().printError("Failed to create player account for " + uuid);
                }
            } else {
                PlayerData playerData = new PlayerData();
                CoinsPlayer coinsPlayer = new CoinsPlayer(uuid);
                coinsPlayer.setCoins(result.get(uuid));
                playerData.setCoinsPlayer(coinsPlayer);
                LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);
                LightCore.instance.getConsolePrinter().printInfo("Player data loaded for " + uuid);
            }
        });
    }
}
