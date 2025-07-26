package com.gardensmc.gardensplayers.economy;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.player.PlayerEntryHandler;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GardensEconomyProvider implements Economy {

    @Override
    public boolean isEnabled() {
        return GardensPlayers.plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "GardensEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return -1;
    }

    @Override
    public String format(double amount) {
        return String.valueOf(round(amount));
    }

    @Override
    public String currencyNamePlural() {
        return currencyNameSingular();
    }

    @Override
    public String currencyNameSingular() {
        return GardensPlayers.config.getCurrencySymbol();
    }

    @Override
    public boolean hasAccount(String username) {
        var uuid = PlayerUtil.getUUID(username);
        return uuid != null; // whether player exists in db
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer) {
        return PlayerEntryHandler.playerExists(offlinePlayer.getUniqueId());
    }

    @Override
    public boolean hasAccount(String username, String worldName) {
        return hasAccount(username);
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer, String worldName) {
        return hasAccount(offlinePlayer);
    }

    @Override
    public double getBalance(String username) {
        var uuid = PlayerUtil.getUUID(username);
        if (uuid == null) {
            return 0;
        }
        return new PlayerEconomyHandler(uuid).getBalance();
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer) {
        return new PlayerEconomyHandler(offlinePlayer.getUniqueId()).getBalance();
    }

    @Override
    public double getBalance(String username, String worldName) {
        return getBalance(username);
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer, String worldName) {
        return getBalance(offlinePlayer);
    }

    @Override
    public boolean has(String username, double amount) {
        var uuid = PlayerUtil.getUUID(username);
        if (uuid == null) {
            return false;
        }
        return new PlayerEconomyHandler(uuid).hasEnough(round(amount));
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, double amount) {
        return new PlayerEconomyHandler(offlinePlayer.getUniqueId()).hasEnough(round(amount));
    }

    @Override
    public boolean has(String username, String worldName, double amount) {
        return has(username, amount);
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, String worldName, double amount) {
        return has(offlinePlayer, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String username, double amount) {
        var uuid = PlayerUtil.getUUID(username);
        if (uuid == null) {
            return new EconomyResponse(
                    0, 0, EconomyResponse.ResponseType.FAILURE, "User does not exist!"
            );
        }
        return withdrawPlayer(uuid, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double amount) {
        var uuid = offlinePlayer.getUniqueId();
        return withdrawPlayer(uuid, amount);
    }

    private EconomyResponse withdrawPlayer(UUID uuid, double amount) {
        int amountRounded = round(amount);
        try {
            var promise = new PlayerEconomyHandler(uuid).withdraw(amountRounded);
            try {
                promise.get(); // wait for withdrawal to occur
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new EconomyResponse(
                    amountRounded,
                    new PlayerEconomyHandler(uuid).getBalance(),
                    EconomyResponse.ResponseType.SUCCESS,
                    null
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, e.getMessage());
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String username, String worldName, double amount) {
        return withdrawPlayer(username, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String worldName, double amount) {
        return withdrawPlayer(offlinePlayer, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String username, double amount) {
        var uuid = PlayerUtil.getUUID(username);
        return depositPlayer(uuid, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double amount) {
        return depositPlayer(offlinePlayer.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String username, String worldName, double amount) {
        return depositPlayer(username, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String worldName, double amount) {
        return depositPlayer(offlinePlayer, amount);
    }

    private EconomyResponse depositPlayer(UUID uuid, double amount) {
        int amountRounded = round(amount);
        try {
            var promise = new PlayerEconomyHandler(uuid).deposit(amountRounded);
            try {
                promise.get(); // wait for deposit to occur
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new EconomyResponse(
                    amountRounded,
                    new PlayerEconomyHandler(uuid).getBalance(),
                    EconomyResponse.ResponseType.SUCCESS,
                    null
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, e.getMessage());
        }
    }

    @Override
    public EconomyResponse createBank(String name, String username) {
        return bankResponse();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer offlinePlayer) {
        return bankResponse();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return bankResponse();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return bankResponse();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return bankResponse();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return bankResponse();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return bankResponse();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String username) {
        return bankResponse();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer offlinePlayer) {
        return bankResponse();
    }

    @Override
    public EconomyResponse isBankMember(String name, String username) {
        return bankResponse();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer offlinePlayer) {
        return bankResponse();
    }

    private EconomyResponse bankResponse() {
        return new EconomyResponse(
                0,
                0,
                EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "GardensEconomy does not support bank accounts!"
        );
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String username) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(String username, String worldName) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String worldName) {
        return false;
    }

    private int round(double amount) {
        return (int) Math.round(amount);
    }
}
