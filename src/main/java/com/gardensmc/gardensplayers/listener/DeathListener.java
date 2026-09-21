package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.death.DeathLocationTracker;
import com.gardensmc.gardensplayers.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    /** Permission required to see the clickable death message and use {@code /deathback}. */
    public static final String PERMISSION = "gplayers.deathback";

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!player.hasPermission(PERMISSION)) {
            return;
        }

        var location = player.getLocation();
        DeathLocationTracker.INSTANCE.set(player.getUniqueId(), location);

        player.sendMessage(MessageUtil.parseIntoComponent(
                "<gray>You died at <white>%d, %d, %d</white>. ".formatted(
                        location.getBlockX(), location.getBlockY(), location.getBlockZ()
                )
                        + "<hover:show_text:'<green>Click to travel back to your death'>"
                        + "<click:run_command:'/deathback'><green><underlined>[Travel back to your death]</underlined></green></click>"
                        + "</hover>"
        ));
    }
}
