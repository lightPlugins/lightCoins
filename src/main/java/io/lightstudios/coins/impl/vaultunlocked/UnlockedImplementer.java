package io.lightstudios.coins.impl.vaultunlocked;


import io.lightstudios.coins.LightCoins;
import io.lightstudios.core.util.LightNumbers;
import net.milkbowl.vault2.economy.AccountPermission;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;

public class UnlockedImplementer implements Economy {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public @NotNull String getName() {
        return "LightCoins";
    }

    @Override
    public boolean hasSharedAccountSupport() {
        return false;
    }

    @Override
    public boolean hasMultiCurrencySupport() {
        return false;
    }

    @Override
    public @NotNull int fractionalDigits(String pluginName) {
        return LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces();
    }

    @Override
    public @NotNull String format(BigDecimal amount) {
        return format(amount, "default");
    }

    @Override
    public @NotNull String format(String pluginName, BigDecimal amount) {
        return LightNumbers.formatForMessages(amount, fractionalDigits(pluginName));
    }

    @Override
    public @NotNull String format(BigDecimal amount, String currency) {
        return "";
    }

    @Override
    public @NotNull String format(String pluginName, BigDecimal amount, String currency) {
        return "";
    }

    @Override
    public boolean hasCurrency(String currency) {
        return false;
    }

    @Override
    public @NotNull String getDefaultCurrency(String pluginName) {
        return "";
    }

    @Override
    public @NotNull String defaultCurrencyNamePlural(String pluginName) {
        return "";
    }

    @Override
    public @NotNull String defaultCurrencyNameSingular(String pluginName) {
        return "";
    }

    @Override
    public Collection<String> currencies() {
        return List.of();
    }

    @Override
    public boolean createAccount(UUID accountID, String name) {
        return false;
    }

    @Override
    public boolean createAccount(UUID accountID, String name, boolean player) {
        return false;
    }

    @Override
    public boolean createAccount(UUID accountID, String name, String worldName) {
        return false;
    }

    @Override
    public boolean createAccount(UUID accountID, String name, String worldName, boolean player) {
        return false;
    }

    @Override
    public Map<UUID, String> getUUIDNameMap() {
        return Map.of();
    }

    @Override
    public Optional<String> getAccountName(UUID accountID) {
        return Optional.empty();
    }

    @Override
    public boolean hasAccount(UUID accountID) {
        return false;
    }

    @Override
    public boolean hasAccount(UUID accountID, String worldName) {
        return false;
    }

    @Override
    public boolean renameAccount(UUID accountID, String name) {
        return false;
    }

    @Override
    public boolean renameAccount(String plugin, UUID accountID, String name) {
        return false;
    }

    @Override
    public boolean deleteAccount(String plugin, UUID accountID) {
        return false;
    }

    @Override
    public boolean accountSupportsCurrency(String plugin, UUID accountID, String currency) {
        return false;
    }

    @Override
    public boolean accountSupportsCurrency(String plugin, UUID accountID, String currency, String world) {
        return false;
    }

    @Override
    public @NotNull BigDecimal getBalance(String pluginName, UUID accountID) {
        return null;
    }

    @Override
    public @NotNull BigDecimal getBalance(String pluginName, UUID accountID, String world) {
        return null;
    }

    @Override
    public @NotNull BigDecimal getBalance(String pluginName, UUID accountID, String world, String currency) {
        return null;
    }

    @Override
    public boolean has(String pluginName, UUID accountID, BigDecimal amount) {
        return false;
    }

    @Override
    public boolean has(String pluginName, UUID accountID, String worldName, BigDecimal amount) {
        return false;
    }

    @Override
    public boolean has(String pluginName, UUID accountID, String worldName, String currency, BigDecimal amount) {
        return false;
    }

    @Override
    public @NotNull EconomyResponse withdraw(String pluginName, UUID accountID, BigDecimal amount) {
        return null;
    }

    @Override
    public @NotNull EconomyResponse withdraw(String pluginName, UUID accountID, String worldName, BigDecimal amount) {
        return null;
    }

    @Override
    public @NotNull EconomyResponse withdraw(String pluginName, UUID accountID, String worldName, String currency, BigDecimal amount) {
        return null;
    }

    @Override
    public @NotNull EconomyResponse deposit(String pluginName, UUID accountID, BigDecimal amount) {
        return null;
    }

    @Override
    public @NotNull EconomyResponse deposit(String pluginName, UUID accountID, String worldName, BigDecimal amount) {
        return null;
    }

    @Override
    public @NotNull EconomyResponse deposit(String pluginName, UUID accountID, String worldName, String currency, BigDecimal amount) {
        return null;
    }

    @Override
    public boolean createSharedAccount(String pluginName, UUID accountID, String name, UUID owner) {
        return false;
    }

    @Override
    public boolean isAccountOwner(String pluginName, UUID accountID, UUID uuid) {
        return false;
    }

    @Override
    public boolean setOwner(String pluginName, UUID accountID, UUID uuid) {
        return false;
    }

    @Override
    public boolean isAccountMember(String pluginName, UUID accountID, UUID uuid) {
        return false;
    }

    @Override
    public boolean addAccountMember(String pluginName, UUID accountID, UUID uuid) {
        return false;
    }

    @Override
    public boolean addAccountMember(String pluginName, UUID accountID, UUID uuid, AccountPermission... initialPermissions) {
        return false;
    }

    @Override
    public boolean removeAccountMember(String pluginName, UUID accountID, UUID uuid) {
        return false;
    }

    @Override
    public boolean hasAccountPermission(String pluginName, UUID accountID, UUID uuid, AccountPermission permission) {
        return false;
    }

    @Override
    public boolean updateAccountPermission(String pluginName, UUID accountID, UUID uuid, AccountPermission permission, boolean value) {
        return false;
    }
}
