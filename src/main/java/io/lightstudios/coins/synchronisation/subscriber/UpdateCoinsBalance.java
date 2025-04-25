package io.lightstudios.coins.synchronisation.subscriber;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.libs.jedis.Jedis;
import io.lightstudios.core.util.libs.jedis.JedisPubSub;

import java.math.BigDecimal;
import java.util.UUID;

public class UpdateCoinsBalance {

    private final String channelName = "coinsDataUpdates";

    public UpdateCoinsBalance() {
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

                        String[] splitMessage = message.split(":");
                        if (splitMessage.length != 3) {
                            // invalid message
                            return;
                        }

                        String uuidData = splitMessage[0];
                        String nameData = splitMessage[1];
                        String currentCoinsData = splitMessage[2];

                        UUID uuid = UUID.fromString(uuidData);
                        BigDecimal currentCoins = new BigDecimal(currentCoinsData);

                        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(uuid);
                        AccountData newAccountData = new AccountData();

                        if (accountData == null) {
                            newAccountData.setUuid(uuid);
                            newAccountData.setName(nameData);
                            CoinsData coinsData = new CoinsData(uuid);
                            coinsData.setCurrentCoins(currentCoins);
                            coinsData.setName(nameData);
                            coinsData.setUuid(uuid);
                            newAccountData.setCoinsData(coinsData);

                            LightCoins.instance.getLightCoinsAPI().getAccountData().put(uuid, newAccountData);
                            LightCoins.instance.getConsolePrinter().printInfo("[REDIS] Created new account data for uuid: " + uuidData);
                            return;
                        }
                        accountData.setName(nameData);

                        CoinsData coinsData = accountData.getCoinsData();
                        coinsData.setCurrentCoins(currentCoins);
                        coinsData.setName(nameData);
                        LightCoins.instance.getConsolePrinter().printInfo("[REDIS] Updated balance from coins data for uuid: " + uuidData);
                    }
                }, this.channelName);

            } catch (Exception e) {
                LightCoins.instance.getConsolePrinter().printError("Error while subscribing to Redis channel: " + channelName);
                e.printStackTrace();
            }
        }).start();
    }
}
