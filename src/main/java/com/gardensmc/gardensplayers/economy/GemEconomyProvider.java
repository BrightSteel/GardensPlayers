package com.gardensmc.gardensplayers.economy;

import com.gardensmc.gardensplayers.currency.GemsCurrencyHandler;
import net.brcdev.shopgui.provider.economy.EconomyProvider;
import org.bukkit.entity.Player;

public class GemEconomyProvider extends EconomyProvider {

    @Override
    public String getName() {
        return "GardensGems";
    }

    @Override
    public String getCurrencySuffix() {
        return " Gems";
    }

    @Override
    public double getBalance(Player player) {
        return GemsCurrencyHandler.INSTANCE.getCurrency(player);
    }

    @Override
    public void deposit(Player player, double v) {
        GemsCurrencyHandler.INSTANCE.addCurrency(player, (int) Math.round(v));
    }

    @Override
    public void withdraw(Player player, double v) {
        GemsCurrencyHandler.INSTANCE.subtractGems(player, (int) Math.round(v));
    }
}
