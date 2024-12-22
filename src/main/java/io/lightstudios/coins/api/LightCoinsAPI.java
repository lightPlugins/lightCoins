package io.lightstudios.coins.api;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.coins.impl.vault.VaultImplementer;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
public class LightCoinsAPI {

    private final HashMap<UUID, AccountData> playerData = new HashMap<>();
    private final List<VirtualData> virtualCurrencies = new ArrayList<>();
    public VaultImplementer vaultImplementer;

    public void getImplementer() {
        this.vaultImplementer = LightCoins.instance.getVaultImplementer();
    }
    public void createPlayerData(OfflinePlayer player) {
        initPlayer(player);
    }
    public void createPlayerData(Player player) {
        initPlayer(player);
    }

    @Nullable
    public AccountData getPlayerData(OfflinePlayer player) {
        return playerData.get(player.getUniqueId());
    }
    @Nullable
    public AccountData getPlayerData(Player player) {
        return playerData.get(player.getUniqueId());
    }
    @Nullable
    public AccountData getPlayerData(UUID uuid) {
        return playerData.get(uuid);
    }


    private void initPlayer(OfflinePlayer player) {
        AccountData playerData = new AccountData();

        CoinsData coinsPlayer = new CoinsData(player.getUniqueId());

        playerData.setCoinsData(coinsPlayer);

        if(!this.playerData.containsKey(player.getUniqueId())) {
            this.playerData.put(player.getUniqueId(), playerData);
        }
    }



}
