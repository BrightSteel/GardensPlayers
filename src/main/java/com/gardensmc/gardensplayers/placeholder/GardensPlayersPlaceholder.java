package com.gardensmc.gardensplayers.placeholder;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.shop.FurnitureShop;
import com.gardensmc.gardensplayers.shop.FurnitureShops;
import com.gardensmc.gardensplayers.util.MessageUtil;
import com.gardensmc.gardensplayers.util.WorldGuardUtil;
import com.gardensmc.gardensplayers.util.Worlds;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GardensPlayersPlaceholder extends PlaceholderExpansion {

    /** Invisible U+E120 gate glyph the gardens-bedrock hud_screen.json override keys off of (blank in
     *  font/glyph_E1.png). One boss bar carries Time + World on one line; the pack draws it as a single
     *  box centered top-center where the vanilla boss bar sits. */
    private static final String HUD_TOKEN = "\uE120";

    /** Invisible U+E121 gate glyph (blank in font/glyph_E1.png) marking the acorn-balance BOSS BAR so the
     *  pack can single it out (separate from the Time/World boss bar's U+E120) and redraw it right-
     *  aligned above the hunger bar instead of centered. Prefixed so its advance width falls to the
     *  LEFT of the right-aligned text. */
    private static final String ACORN_TOKEN = "\uE121";

    /** HUD icons for Bedrock, baked into gardens-bedrock font sheets. Emitted straight into the boss-bar
     *  / action-bar title (Geyser's boss-bar path isn't run through TagBadges). Compass/clock live in
     *  glyph_E4.png; the acorn is the existing glyph_E2.png cell U+E200 (E201 is the gem). */
    private static final String COMPASS_ICON = "\uE400";
    private static final String CLOCK_ICON = "\uE401";
    private static final String ACORN_ICON = "\uE200";

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
        // placeholders are requested every tick by scoreboards and tab lists: use what's cached, and on a miss
        // load in the background and show nothing for now, instead of waiting on the database
        var playerEntry = PlayerEntryCache.INSTANCE.getIfLoaded(player.getUniqueId());
        if (playerEntry == null) {
            return "";
        }
        return switch (params.toLowerCase()) {
            case "nickname" -> MessageUtil.parseIntoLegacyDisplayable(playerEntry.getNickname());
            case "streak" -> {
                var streak = com.gardensmc.gardensplayers.streak.StreakHandler.INSTANCE.cached(player.getUniqueId());
                yield streak == null ? "0" : String.valueOf(streak.current());
            }
            case "streak_best" -> {
                var streak = com.gardensmc.gardensplayers.streak.StreakHandler.INSTANCE.cached(player.getUniqueId());
                yield streak == null ? "0" : String.valueOf(streak.best());
            }
            case "title" -> {
                // with a trailing space, so a format can put it straight before the name
                var shown = com.gardensmc.gardensplayers.title.Titles.INSTANCE.shown(player.getUniqueId());
                yield shown.isEmpty() ? "" : MessageUtil.parseIntoLegacyDisplayable(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(shown)) + " ";
            }
            case "balance" -> String.valueOf(playerEntry.getMoney());
            case "gems" -> String.valueOf(playerEntry.getPoints());
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
            case "region" -> MessageUtil.readableId(PlaceholderAPI.setPlaceholders(player, "%worldguard_region_name_capitalized%"));
            case "bedrock" -> {
                var onlinePlayer = player.getPlayer();
                if (onlinePlayer == null) {
                    yield null;
                }
                var world = onlinePlayer.getWorld();
                // HUD_TOKEN sits in the middle gap (not trailing) so its invisible advance width
                // widens the Time/World gap instead of adding stray padding on the right edge.
                yield CLOCK_ICON + " " + formatWorldTime(world.getTime()) + " " + HUD_TOKEN + " "
                        + COMPASS_ICON + " " + resolveWorldLabel(onlinePlayer, world);
            }
            // Acorn balance + acorn icon, meant to be delivered as a BOSS BAR (by a separate plugin).
            // The leading ACORN_TOKEN lets gardens-bedrock hud_screen.json recognize this boss bar and
            // redraw it right-aligned above the hunger bar (see gardens_acorn_grid in the pack).
            case "bedrock_acorns" -> ACORN_TOKEN + playerEntry.getMoney() + " " + ACORN_ICON;
            default -> null;
        };
    }

    /*
     * The bedrock HUD text is rendered top-center by the gardens-bedrock resource pack
     * ({@code hud_screen.json}), which keys off the invisible U+E120 gate glyph to hide the normal
     * centered boss bar and re-draw the gated bar as its own content-hugging black box mounted where
     * the vanilla boss bar sits. A separate plugin delivers the string as a single boss bar title to
     * Geyser players. Time and World share ONE bar on ONE line (three-space gap, no "\n") so it
     * renders as a single box centered on the whole text. The glyph is appended (not prefixed) so it
     * adds no leading space.
     */

    /** Convert a world's time-of-day ticks (0..24000, 0 = 06:00) to a 12-hour "h AM/PM" clock. */
    private static String formatWorldTime(long ticks) {
        long timeOfDay = ((ticks % 24000L) + 24000L) % 24000L;
        int hour24 = (int) (((timeOfDay / 1000L) + 6L) % 24L);
        String period = hour24 < 12 ? "AM" : "PM";
        int hour12 = hour24 % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        return hour12 + " " + period;
    }

    /**
     * World label for the Bedrock HUD, in priority order (mirrors the Java nameplate config):
     * 1. WorldGuard region name, if the player is standing in a named region.
     * 2. GriefPrevention claim owner, if standing in a claim that isn't unclaimed/admin-owned.
     * 3. Otherwise the world's display name.
     */
    private static String resolveWorldLabel(Player player, World world) {
        String regionRaw = PlaceholderAPI.setPlaceholders(player, "%worldguard_region_name_capitalized%");
        if (!regionRaw.isEmpty()) {
            return MessageUtil.readableId(regionRaw);
        }

        String claimOwner = PlaceholderAPI.setPlaceholders(player, "%griefprevention_currentclaim_ownername%");
        if (!claimOwner.equals("Unclaimed") && !claimOwner.equals("an administrator")) {
            return claimOwner + "'s Claim";
        }

        return worldDisplayName(world);
    }

    private static String worldDisplayName(World world) {
        return switch (world.getName()) {
            case Worlds.GARDENS -> "Gardens";
            case Worlds.GARDENS_NETHER -> "Nether";
            case Worlds.GARDENS_END -> "The End";
            case Worlds.RESOURCE -> "Resource World";
            case Worlds.RESOURCE_NETHER -> "Resource Nether";
            case Worlds.RESOURCE_END -> "Resource End";
            default -> world.getName();
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
