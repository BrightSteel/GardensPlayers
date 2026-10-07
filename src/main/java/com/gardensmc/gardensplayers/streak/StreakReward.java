package com.gardensmc.gardensplayers.streak;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.currency.GemsCurrencyHandler;
import com.gardensmc.gardensplayers.title.Titles;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import com.gardensmc.gardensplayers.wardrobe.WardrobeBridge;
import emanondev.itemedit.ItemEdit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One reward from streaks.yml, written as {@code "<kind> <what> [amount]"}: {@code acorns 50}, {@code gems 1},
 * {@code item furniture_key 1} (an ItemEdit server item), {@code skin <wardrobe skin id>} or {@code title <title id>}.
 */
public record StreakReward(String kind, String what, int amount) {

    @Nullable
    public static StreakReward parse(String line) {
        var parts = line.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return null;
        var kind = parts[0].toLowerCase();
        try {
            return switch (kind) {
                case "acorns", "gems" -> new StreakReward(kind, "", Integer.parseInt(parts[1]));
                case "item" -> new StreakReward(kind, parts[1], parts.length > 2 ? Integer.parseInt(parts[2]) : 1);
                case "skin", "title" -> new StreakReward(kind, parts[1], 1);
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Gives it to the player (main thread). Items that don't fit their inventory drop at their feet. */
    public void grant(Player player) {
        switch (kind) {
            case "acorns" -> AcornsCurrencyHandler.INSTANCE.addCurrency(player, amount);
            case "gems" -> GemsCurrencyHandler.INSTANCE.addCurrency(player, amount);
            case "item", "skin" -> {
                var item = item();
                if (item == null) {
                    GardensPlayers.plugin.getLogger().warning("Streak reward " + kind + " '" + what + "' doesn't exist");
                } else {
                    PlayerUtil.giveItems(player, item, amount);
                }
            }
            case "title" -> Titles.INSTANCE.grant(player.getUniqueId(), what);
            default -> {
            }
        }
    }

    /** How it reads in chat, e.g. "50 acorns", "1 gem", "Furniture Key", "the Rooted title". */
    public String describe() {
        return switch (kind) {
            case "acorns" -> amount + " acorns";
            case "gems" -> amount + (amount == 1 ? " gem" : " gems");
            case "title" -> "the " + Titles.INSTANCE.plainName(what) + " title";
            default -> {
                var item = item();
                var name = item == null ? what : PlainTextComponentSerializer.plainText().serialize(item.effectiveName());
                yield amount > 1 ? amount + " " + name : name;
            }
        };
    }

    @Nullable
    private ItemStack item() {
        return switch (kind) {
            case "item" -> ItemEdit.get().getServerStorage().getItem(what);
            case "skin" -> WardrobeBridge.skinItem(what);
            default -> null;
        };
    }
}
