package io.lightstudios.coins.api;

import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.coins.api.models.VirtualCurrency;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
public class LightCoinsAPI {

    private final HashMap<UUID, PlayerData> playerData = new HashMap<>();
    private final List<VirtualCurrency> virtualCurrencies = new ArrayList<>();

    public void createPlayerData(OfflinePlayer player) {
        initPlayer(player);
    }
    public void createPlayerData(Player player) {
        initPlayer(player);
    }

    @Nullable
    public PlayerData getPlayerData(OfflinePlayer player) {
        return playerData.get(player.getUniqueId());
    }
    @Nullable
    public PlayerData getPlayerData(Player player) {
        return playerData.get(player.getUniqueId());
    }
    @Nullable
    public PlayerData getPlayerData(UUID uuid) {
        return playerData.get(uuid);
    }


    private void initPlayer(OfflinePlayer player) {
        PlayerData playerData = new PlayerData();

        CoinsPlayer coinsPlayer = new CoinsPlayer(player.getUniqueId());

        playerData.setCoinsPlayer(coinsPlayer);

        if(!this.playerData.containsKey(player.getUniqueId())) {
            this.playerData.put(player.getUniqueId(), playerData);
        }
    }



}
