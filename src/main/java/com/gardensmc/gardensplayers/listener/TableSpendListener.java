package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensenchants.events.TableSpendEvent;
import com.gardensmc.gardensplayers.util.TransactionLog;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Enchanting table spending, into the transaction log. */
public class TableSpendListener implements Listener {

    @EventHandler
    public void onTableSpend(TableSpendEvent e) {
        TransactionLog.log(e.getPlayer(), "enchanting_table", e.getKind(), e.getItem(), e.getKind(),
                1, e.getAcorns(), e.getAcorns(), "acorns", e.getXp());
    }
}
