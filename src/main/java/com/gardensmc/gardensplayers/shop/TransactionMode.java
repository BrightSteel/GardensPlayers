package com.gardensmc.gardensplayers.shop;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TransactionMode {
    BUY("Buy", "Bought"),
    SELL("Sell", "Sold");

    private final String text;
    private final String pastText;
}
