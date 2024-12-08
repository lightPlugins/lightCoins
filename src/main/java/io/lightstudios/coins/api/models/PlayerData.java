package io.lightstudios.coins.api.models;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
public class PlayerData {

    private CoinsPlayer coinsPlayer;
    private List<VirtualCurrency> virtualCurrencies;
    private UUID uuid;

    @Nullable
    public VirtualCurrency getVirtualCurrencyByName(String name) {
        return virtualCurrencies.stream().filter(virtualCurrencies
                -> virtualCurrencies.getCurrencyName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

}
