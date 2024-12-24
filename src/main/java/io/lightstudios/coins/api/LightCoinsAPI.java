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

    private final HashMap<UUID, AccountData> accountData = new HashMap<>();

    public VaultImplementer getImplementer() {
        return LightCoins.instance.getVaultImplementer();
    }

    @Nullable
    public AccountData getAccountData(OfflinePlayer player) {
        return accountData.get(player.getUniqueId());
    }
    @Nullable
    public AccountData getAccountData(Player player) {
        return accountData.get(player.getUniqueId());
    }
    @Nullable
    public AccountData getAccountData(UUID uuid) {
        return accountData.get(uuid);
    }
    @Nullable
    public AccountData getAccountData(String playerName) {
        for(AccountData data : accountData.values()) {
            if(data.getName() != null && data.getName().equalsIgnoreCase(playerName)) {
                return data;
            }
        }
        return null;
    }

    public List<String> getAccountDataPlayerNames() {
        List<String> names = new ArrayList<>();
        for(AccountData data : accountData.values()) {
            if(data.getName() != null && !data.getName().equalsIgnoreCase("nonplayer_account")) {
                names.add(data.getName());
            }
        }

        return names;

    }
}
