package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensenchants.api.AirTime;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.util.BedrockUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Vanilla's "Flying is not enabled on this server" kick, for Java players only. allow-flight is on in
 * server.properties because Bedrock players (through Geyser) trip vanilla's check falsely, which turned it off for
 * everyone. Same rule as vanilla: up in the air with no block near you, not falling, for more than 4 seconds (longer
 * with lower gravity). The kick goes through LiteBans, so it's on the player's record. Off with flight-kick.enabled.
 * <p>
 * Our own enchants that hold players up (Lift, Drift, Grapple and the rest) mark them in {@link AirTime}, and they
 * don't count while marked.
 */
public class FlyCheck implements Listener {

    static final String REASON = "Flying is not enabled on this server";
    // vanilla's: rising, level, or sinking slower than this a tick counts as floating
    private static final double FLOATING_DROP = -0.03125;
    private static final int BASE_TICKS = 80;
    private static final double BASE_GRAVITY = 0.08;

    // EnderPets and MCPets mounts seat riders through ModelEngine, which doesn't always make them a vanilla passenger
    private static final boolean MODEL_ENGINE = Bukkit.getPluginManager().getPlugin("ModelEngine") != null;

    private final Map<UUID, Double> lastY = new HashMap<>();
    private final Map<UUID, Integer> floatingTicks = new HashMap<>();
    private final Set<UUID> kicked = new HashSet<>();

    public void start() {
        Bukkit.getScheduler().runTaskTimer(GardensPlayers.plugin, this::tick, 1, 1);
    }

    private void tick() {
        // read every tick, so /gardensplayers reload turns it on or off; while off, nothing carries over to when it's on
        if (!GardensPlayers.config.isFlightKickEnabled()) {
            lastY.clear();
            floatingTicks.clear();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            var id = player.getUniqueId();
            double y = player.getLocation().getY();
            Double previous = lastY.put(id, y);
            if (previous == null || exempt(player)) {
                floatingTicks.remove(id);
                continue;
            }
            if (y - previous < FLOATING_DROP || !noBlocksAround(player)) {
                floatingTicks.remove(id);
                continue;
            }
            int ticks = floatingTicks.merge(id, 1, Integer::sum);
            if (ticks > maxTicks(player) && kicked.add(id)) kick(player);
        }
    }

    private static boolean exempt(Player player) {
        var mode = player.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR
                || player.getAllowFlight() || player.isFlying() || player.isGliding() || player.isRiptiding()
                || player.isInsideVehicle() || player.isSleeping() || player.isDead()
                || player.hasPotionEffect(PotionEffectType.LEVITATION)
                || AirTime.isAllowed(player) || BedrockUtil.isBedrock(player)
                || MODEL_ENGINE && ModelMounts.isMounted(player);
    }

    // vanilla's: 80 ticks at normal gravity, longer as gravity drops (Featherweight), never at none
    private static int maxTicks(Player player) {
        var attribute = player.getAttribute(Attribute.GRAVITY);
        double gravity = attribute == null ? BASE_GRAVITY : attribute.getValue();
        if (gravity < 1e-5) return Integer.MAX_VALUE;
        return (int) Math.ceil(BASE_TICKS * Math.max(BASE_GRAVITY / gravity, 1));
    }

    // vanilla's: any block that isn't air within 1/16 of the player, or a little over half a block below
    private static boolean noBlocksAround(Player player) {
        var box = player.getBoundingBox().expand(0.0625).expandDirectional(0, -0.55, 0);
        var world = player.getWorld();
        for (int x = (int) Math.floor(box.getMinX()); x <= (int) Math.floor(box.getMaxX()); x++) {
            for (int z = (int) Math.floor(box.getMinZ()); z <= (int) Math.floor(box.getMaxZ()); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
                for (int y = (int) Math.floor(box.getMinY()); y <= (int) Math.floor(box.getMaxY()); y++) {
                    if (!world.getBlockAt(x, y, z).getType().isAir()) return false;
                }
            }
        }
        return true;
    }

    private static void kick(Player player) {
        GardensPlayers.plugin.getLogger().info("Kicking " + player.getName() + " for flying at " + player.getLocation());
        if (Bukkit.getPluginManager().isPluginEnabled("LiteBans")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "litebans:kick " + player.getName() + " " + REASON);
        } else {
            player.kick(Component.text(REASON));
        }
    }

    // a teleport or respawn moves them without falling: start counting again from where they land
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        lastY.remove(e.getPlayer().getUniqueId());
        floatingTicks.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        lastY.remove(e.getPlayer().getUniqueId());
        floatingTicks.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var id = e.getPlayer().getUniqueId();
        lastY.remove(id);
        floatingTicks.remove(id);
        kicked.remove(id);
    }
}
