package io.lightstudios.coins.api.models;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
public class AccountData {

    private CoinsData coinsData;
    private List<VirtualData> virtualCurrencies;
    private UUID uuid;
    @Nullable
    private String name;
    @Nullable
    private OfflinePlayer offlinePlayer;

    public AccountData() {
        this.virtualCurrencies = new ArrayList<>();
    }

    @Nullable
    public VirtualData getVirtualCurrencyByName(String name) {
        return virtualCurrencies.stream().filter(virtualCurrencies
                -> virtualCurrencies.getCurrencyName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

}
