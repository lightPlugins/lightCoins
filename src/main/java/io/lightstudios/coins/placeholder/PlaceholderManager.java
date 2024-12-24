package io.lightstudios.coins.placeholder;

import io.lightstudios.coins.placeholder.coins.CoinsAmountHolder;
import io.lightstudios.coins.placeholder.coins.CoinsRawAmountHolder;
import io.lightstudios.coins.placeholder.coins.CurrencyNameHolder;
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
                new CoinsAmountHolder(),
                new CoinsRawAmountHolder(),
                new CurrencyNameHolder()
        )));
    }

}
