package com.gardensmc.gardensplayers.currency;

import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

// acorns are the server's money: the player table's money column, and what Vault reports
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AcornsCurrencyHandler extends StoredCurrency {

    public static final AcornsCurrencyHandler INSTANCE = new AcornsCurrencyHandler();

    @Override
    public String name() {
        return "acorns";
    }

    @Override
    public String symbol() {
        return "<white><font:gardens:default>Ġ</font></white>";
    }

    @Override
    protected int read(PlayerEntry entry) {
        return entry.getMoney();
    }

    @Override
    protected void adjustCached(PlayerEntry entry, int delta) {
        entry.adjustMoney(delta);
    }

    @Override
    protected void addStored(UUID player, int amount) {
        Tables.playerTable.addMoney(player, amount);
    }

    @Override
    protected boolean takeStored(UUID player, int amount) {
        return Tables.playerTable.subtractMoney(player, amount);
    }

    @Override
    protected void setStored(UUID player, int amount) {
        Tables.playerTable.setMoney(player.toString(), amount);
    }
}
