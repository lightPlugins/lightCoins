package io.lightstudios.coins.synchronisation.subscriber;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.libs.jedis.Jedis;
import io.lightstudios.core.util.libs.jedis.JedisPubSub;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UpdateVirtualBalance {

    private final String channelName = "virtualDataUpdates";

    public UpdateVirtualBalance() {
        if(LightCore.instance.isRedis) {
            LightCoins.instance.getConsolePrinter().printInfo("Subscribing to Redis channel: " + channelName);
            receiveData();
        }
    }

    private void receiveData() {
        new Thread(() -> {
            try (Jedis jedis = LightCore.instance.getRedisManager().getJedisPool().getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        LightCoins.instance.getConsolePrinter().printInfo("[REDIS] Received redis message in channel: " + channel + " with message: " + message);
                        if (!channel.equals(channelName)) {
                            // not the channel we are looking for
                            return;
                        }
                        // message format: uuid:name:currency:balance
                        String[] splitMessage = message.split(":");
                        if (splitMessage.length != 4) {
                            // invalid message
                            return;
                        }

                        String uuidData = splitMessage[0];
                        String nameData = splitMessage[1];
                        String currencyData = splitMessage[2];
                        String currentBalanceData = splitMessage[3];

                        UUID uuid = UUID.fromString(uuidData);
                        BigDecimal currentCoins = new BigDecimal(currentBalanceData);

                        CompletableFuture<AccountData> readAccountData = LightCoins.instance.getLightCoinsAPI().createAccountDataAsync(LightCoins.instance, uuid, nameData);
                        readAccountData.thenAccept(accountData -> {
                            accountData.setName(nameData);

                            List<VirtualData> virtualDataList = accountData.getVirtualCurrencies();

                            VirtualData virtualData = virtualDataList.stream()
                                    .filter(v -> v.getCurrencyName().equalsIgnoreCase(currencyData))
                                    .findFirst()
                                    .orElse(null);

                            if (virtualData == null) {
                                return;
                            }

                            virtualData.setCurrentBalance(currentCoins);
                            virtualData.setPlayerName(nameData);

                            LightCoins.instance.getConsolePrinter().printInfo("[REDIS] Updated balance from coins data for uuid: " + uuidData);
                        });
                    }
                }, this.channelName);

            } catch (Exception e) {
                LightCoins.instance.getConsolePrinter().printError("Error while subscribing to Redis channel: " + channelName);
                e.printStackTrace();
            }
        }).start();
    }
}
