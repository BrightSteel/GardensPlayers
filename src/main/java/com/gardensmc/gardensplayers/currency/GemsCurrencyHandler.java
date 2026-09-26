package com.gardensmc.gardensplayers.currency;

import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

// gems: the player table's points column
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GemsCurrencyHandler extends StoredCurrency {

    public static final GemsCurrencyHandler INSTANCE = new GemsCurrencyHandler();

    @Override
    public String name() {
        return "gems";
    }

    @Override
    public String symbol() {
        return "<white><font:gardens:default>Ň</font></white>";
    }

    @Override
    protected int read(PlayerEntry entry) {
        return entry.getPoints();
    }

    @Override
    protected void adjustCached(PlayerEntry entry, int delta) {
        entry.adjustPoints(delta);
    }

    @Override
    protected void addStored(UUID player, int amount) {
        Tables.playerTable.addPoints(player, amount);
    }

    @Override
    protected boolean takeStored(UUID player, int amount) {
        return Tables.playerTable.subtractPoints(player, amount);
    }

    @Override
    protected void setStored(UUID player, int amount) {
        Tables.playerTable.setPoints(player.toString(), amount);
    }
}
