package com.gardensmc.gardensplayers.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.cache.PlayerRewardsCache;
import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.mail.MailHandler;
import com.gardensmc.gardensplayers.player.PlayerEntryHandler;
import com.gardensmc.gardensplayers.quest.QuestHandler;
import com.gardensmc.gardensplayers.reward.RewardHandler;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        var player = e.getPlayer();
        // create playerEntry to trigger join message if applicable
        PlayerEntryCache.INSTANCE.get(player.getUniqueId());

        if (!player.isOnGround() && !player.isFlying() && player.hasPermission("gplayers.fly")) {
            player.setAllowFlight(true);
            player.setFlying(true);
            new SenderMessageHandler(player).sendMessage("Set your flight to on");
        }

        // check for rewards
        RewardHandler.grantPendingRewards(e.getPlayer());
        if (!PlayerRewardsCache.INSTANCE.getSync(e.getPlayer().getUniqueId()).isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>You have pending rewards! Claim them with <gray>/rewards"));
        }

        // alert for unread mail
        MailHandler.INSTANCE.alertUnread(player);

        // nudge about remaining daily quests, slightly delayed so it lands after the join clutter
        Bukkit.getScheduler().runTaskLater(
                GardensPlayers.plugin, () -> QuestHandler.INSTANCE.alertRemaining(player), 40);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        new PlayerEntryHandler(e.getPlayer().getUniqueId()).updateLastLocation();
        OnlinePlayerCache.INSTANCE.getOnlinePlayerData(e.getPlayer().getUniqueId()).resetAfkTick(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (isNewLocation(e.getFrom(), e.getTo())) {
            OnlinePlayerCache.INSTANCE.getOnlinePlayerData(e.getPlayer().getUniqueId()).resetAfkTick(e.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerExecuteCommand(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().startsWith("/afk")) {
            return;
        }
        OnlinePlayerCache.INSTANCE.getOnlinePlayerData(e.getPlayer().getUniqueId()).resetAfkTick(e.getPlayer());
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent e) {
        var playerData = OnlinePlayerCache.INSTANCE.getOnlinePlayerData(e.getPlayer().getUniqueId());
        playerData.resetAfkTick(e.getPlayer());
        if (playerData.getTextCapture() != null) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                playerData.getTextCapture().accept(PlainTextComponentSerializer.plainText().serialize(e.message()));
                playerData.setTextCapture(null);
            });
        }
    }

    private boolean isNewLocation(Location l1, Location l2) {
        return l1.getX() != l2.getX()
                || l1.getY() != l2.getY()
                || l1.getZ() != l1.getZ();
    }

    @EventHandler
    public void onPlayerChangeWorlds(PlayerChangedWorldEvent e) {
        var player = e.getPlayer();
        var permission = getWorldRtpPermission(player.getWorld().getEnvironment());
        if (permission != null && !player.hasPermission(permission)) {
            GardensPlayers.permission.playerAdd(player, permission);
        }
    }

    private String getWorldRtpPermission(World.Environment environment) {
        if (environment == World.Environment.THE_END) {
            return "gplayers.rtp.end";
        }
        if (environment == World.Environment.NETHER) {
            return "gplayers.rtp.nether";
        }
        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSneakJump(PlayerJumpEvent e) {
        var player = e.getPlayer();
        if (player.isSneaking() && isLookingUp(player) && isWingsEquipped(player)) {
            multiplyVelocity(player, 3);
            player.spawnParticle(Particle.EXPLOSION, player.getLocation(), 5);
            player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 1);
            player.setGliding(true);
        }
    }

    private void multiplyVelocity(Player player, double amount) {
        player.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(amount));
    }

    private boolean isWingsEquipped(Player player) {
        if (player.getInventory().getItem(EquipmentSlot.CHEST).getType() != Material.ELYTRA) {
            return false;
        }
        return player.hasPermission("gardens.wings.use");
    }

    private boolean isLookingUp(Player player) {
        return player.getPitch() <= -50 && player.getPitch() >= -90;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSneak(PlayerToggleSneakEvent e) {
        var player = e.getPlayer();
        if (player.isGliding() && isWingsEquipped(player)) {
            var playerData = OnlinePlayerCache.INSTANCE.getOnlinePlayerData(player.getUniqueId());
            var cooldown = playerData.getWingsBoostCooldown();
            var isBypassCooldown = player.hasPermission("gardens.wings.cooldown_bypass");
            if (cooldown > 0 && !isBypassCooldown) {
                e.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot boost for " + cooldown / 20 + " more seconds!"));
            } else {
                player.spawnParticle(Particle.FLAME, player.getLocation(), 15);
                multiplyVelocity(player, 2.5);
                if (!isBypassCooldown) {
                    playerData.setWingsBoostCooldown(2*20);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerElytraFall(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (e.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL || e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (isWingsEquipped(player)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Your wings cushioned your fall"));
                    e.setCancelled(true);
                }
            }
        }
    }

    public static void registerPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() == PacketType.Status.Server.RESPONSE) {
                    var wrapper = new WrapperStatusServerResponse(event);
                    var json = wrapper.getComponent();
                    json.getAsJsonObject("version").addProperty("name", "1.21.4+");
                    wrapper.setComponent(json);
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortalToEnd(PlayerPortalEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) return;
        if (!e.getFrom().getWorld().getName().equals("gardens")) return;
        e.setCancelled(true);
        e.getPlayer().chat("/warp tp pluto");
    }

}
