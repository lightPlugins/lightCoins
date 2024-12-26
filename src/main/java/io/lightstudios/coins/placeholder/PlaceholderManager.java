package io.lightstudios.coins.placeholder;

import io.lightstudios.coins.placeholder.coins.CoinsAmountHolder;
import io.lightstudios.coins.placeholder.coins.CoinsRawAmountHolder;
import io.lightstudios.coins.placeholder.coins.CoinsCurrencyHolder;
import io.lightstudios.coins.placeholder.virtual.VirtualAmountHolder;
import io.lightstudios.coins.placeholder.virtual.VirtualCurrencyHolder;
import io.lightstudios.coins.placeholder.virtual.VirtualRawAmountHolder;
import io.lightstudios.core.placeholder.LightPlaceholder;
import io.lightstudios.core.placeholder.PlaceholderRegistrar;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderManager {

    public ArrayList<LightPlaceholder> placeholders = new ArrayList<>();

    public PlaceholderManager() {
        addPlaceHolder();
        new PlaceholderRegistrar(
                "lightcoins",
                "LightStudios",
                "1.0",
                true,
                 placeholders
        );
    }

    public void addPlaceHolder() {
        placeholders.addAll(new ArrayList<>(List.of(
                // Coins
                new CoinsAmountHolder(),
                new CoinsRawAmountHolder(),
                new CoinsCurrencyHolder(),
                // Virtual
                new VirtualAmountHolder(),
                new VirtualCurrencyHolder(),
                new VirtualRawAmountHolder()
        )));
    }

}
