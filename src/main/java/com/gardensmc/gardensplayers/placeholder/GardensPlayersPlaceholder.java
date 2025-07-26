package com.gardensmc.gardensplayers.placeholder;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.shop.FurnitureShop;
import com.gardensmc.gardensplayers.shop.FurnitureShops;
import com.gardensmc.gardensplayers.util.MessageUtil;
import com.gardensmc.gardensplayers.util.WorldGuardUtil;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GardensPlayersPlaceholder extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "GardensPlayers";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Bright_Steel";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        var playerEntry = PlayerEntryCache.INSTANCE.getSync(player.getUniqueId());
        if (playerEntry == null) {
            return null;
        }
        return switch (params.toLowerCase()) {
            case "nickname" -> MessageUtil.parseIntoLegacyDisplayable(playerEntry.getNickname());
            case "balance" -> String.valueOf(playerEntry.getMoney());
            case "afk" -> {
                if (!player.isOnline()) {
                    // avoid creating cache entry if not online
                    yield null;
                }
                yield String.valueOf(OnlinePlayerCache.INSTANCE
                        .getOnlinePlayerData(player.getUniqueId())
                        .isAFK());
            }
            case "shop" -> {
                var onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    var furnitureShop = getFurnitureShopLookingAt(onlinePlayer);
                    if (furnitureShop != null) {
                        yield furnitureShop.customItem().getDisplay();
                    }
                }
                yield "Shop";
            }
            case "shop_price" -> {
                var onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    var furnitureShop = getFurnitureShopLookingAt(onlinePlayer);
                    if (furnitureShop != null) {
                        yield String.valueOf(furnitureShop.price());
                    }
                }
                yield "Shop";
            }
            default -> null;
        };
    }

    public static FurnitureShop getFurnitureShopLookingAt(Player player) {
        ApplicableRegionSet regions = WorldGuardUtil.getRegionsAt(player.getPlayer().getLocation());
        if (regions == null) return null;
        var region = regions.getRegions().stream().filter(rg -> rg.getId().contains("shop")).findFirst().orElse(null);
        if (region != null) {
            var shop = region.getId();
            var furniture = getFurnitureOrBlockLookingAt(player);
            if (furniture != null) {
                return FurnitureShops.INSTANCE.getFurnitureShop(shop, furniture);
            }
        }
        return null;
    }

    @Nullable
    private static CustomItem getFurnitureOrBlockLookingAt(Player player) {
        var furniture = getFurnitureLookingAt(player);
        if (furniture != null) {
            return furniture;
        }
        var block = player.getTargetBlockExact(5);
        if (block != null && block.getType() == Material.NOTE_BLOCK) {
            return CustomBlockManager.getItemFromLeaves(block);
        }
        return null;
    }

    @Nullable
    private static CustomItem getFurnitureLookingAt(Player player) {
        var nearbyEntities = OnlinePlayerCache.INSTANCE.getOnlinePlayerData(player.getUniqueId()).getNearbyEntities();
        if (nearbyEntities == null) {
            return null;
        }
        return nearbyEntities.stream()
                .filter(entity -> entity instanceof ItemDisplay && isLookingAtEntity(player, entity))
                .map(entity -> CustomBlockManager.getItemFromDisplay((ItemDisplay) entity))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    // only works with living entities, ie armor stands, mobs
    private static boolean isLookingAtEntity(Player player, Entity entity) {
        BoundingBox boundingBox = entity.getBoundingBox().shift(0, -1.5, 0).expand(0.5, 0.5, 0.5);
        RayTraceResult hitResult = boundingBox.rayTrace(player.getLocation().toVector(), player.getLocation().getDirection(), 4);

        if (hitResult != null) {
            double dist = player.getLocation().toVector().distanceSquared(hitResult.getHitPosition());
            return dist < Double.MAX_VALUE;
        }

        return false;
    }


}
