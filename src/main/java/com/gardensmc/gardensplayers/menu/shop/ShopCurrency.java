package com.gardensmc.gardensplayers.menu.shop;

import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.currency.Currency;
import com.gardensmc.gardensplayers.currency.GemsCurrencyHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ShopCurrency {
    VAULT(AcornsCurrencyHandler.INSTANCE),
    GEMS(GemsCurrencyHandler.INSTANCE);

    private final Currency currencyHandler;
}
