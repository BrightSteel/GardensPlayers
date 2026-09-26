package com.gardensmc.gardensplayers.economy;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.player.PlayerEntryHandler;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class GardensEconomyProvider implements Economy {

    private static final AcornsCurrencyHandler ACORNS = AcornsCurrencyHandler.INSTANCE;

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
        return ACORNS.getCurrency(uuid);
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer) {
        return ACORNS.getCurrency(offlinePlayer.getUniqueId());
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
        return ACORNS.has(uuid, withdrawalAmount(amount));
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, double amount) {
        return ACORNS.has(offlinePlayer.getUniqueId(), withdrawalAmount(amount));
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

    private EconomyResponse withdrawPlayer(@Nullable UUID uuid, double amount) {
        int amountRounded = withdrawalAmount(amount);
        if (!hasEntry(uuid)) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "No account for that player");
        }
        try {
            var promise = ACORNS.takeCurrency(uuid, amountRounded);
            try {
                if (!promise.get()) { // wait for withdrawal to occur
                    return new EconomyResponse(0, ACORNS.getCurrency(uuid), EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
                }
            } catch (Exception e) {
                // report it to the calling plugin instead of throwing into its transaction
                GardensPlayers.plugin.getLogger().log(Level.WARNING, "Vault withdrawal failed", e);
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Database error");
            }
            return new EconomyResponse(
                    amountRounded,
                    ACORNS.getCurrency(uuid),
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

    private EconomyResponse depositPlayer(@Nullable UUID uuid, double amount) {
        int amountRounded = depositAmount(amount);
        if (!hasEntry(uuid)) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "No account for that player");
        }
        try {
            var promise = ACORNS.addCurrency(uuid, amountRounded);
            try {
                promise.get(); // wait for deposit to occur
            } catch (Exception e) {
                GardensPlayers.plugin.getLogger().log(Level.WARNING, "Vault deposit failed", e);
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Database error");
            }
            return new EconomyResponse(
                    amountRounded,
                    ACORNS.getCurrency(uuid),
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

    // a player row exists (unknown usernames resolve to null). Vault is synchronous, so for an offline player this
    // and the conditional write in withdraw/deposit block on the database; an online player's entry is cached.
    private boolean hasEntry(@Nullable UUID uuid) {
        return uuid != null && PlayerEntryCache.INSTANCE.getSync(uuid) != null;
    }

    // round up: a withdrawal never takes less than asked, so fractional prices can't create money
    static int withdrawalAmount(double amount) {
        return (int) Math.ceil(amount);
    }

    // round down: a deposit never gives more than asked
    static int depositAmount(double amount) {
        return (int) Math.floor(amount);
    }

    private int round(double amount) {
        return (int) Math.round(amount);
    }
}
