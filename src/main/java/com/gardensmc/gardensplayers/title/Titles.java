package com.gardensmc.gardensplayers.title;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.Tables;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Titles players earn (from streaks, later the season pass) and pick one of to show before their name, through the
 * {@code %gardensplayers_title%} placeholder in chat and the tab list. What each title looks like is in titles.yml.
 */
public final class Titles {

    public static final Titles INSTANCE = new Titles();

    /** A player's earned titles and the one they show (null for none). */
    private record Owned(List<String> ids, @Nullable String selected) {
    }

    private Map<String, String> looks = Map.of();
    private final Map<UUID, Owned> owned = new ConcurrentHashMap<>();

    private Titles() {
    }

    public void reload() {
        var file = new File(GardensPlayers.plugin.getDataFolder(), "titles.yml");
        if (!file.exists()) {
            GardensPlayers.plugin.saveResource("titles.yml", false);
        }
        var section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("titles");
        var loaded = new LinkedHashMap<String, String>();
        if (section != null) {
            for (var id : section.getKeys(false)) {
                loaded.put(id, section.getString(id, id));
            }
        }
        looks = loaded;
    }

    public void load(UUID player) {
        PlayerEntryCache.INSTANCE.get(player).thenApplyAsync(entry -> Tables.titleTable.get(player)).thenAccept(entries -> {
            var ids = new ArrayList<String>();
            String selected = null;
            for (var entry : entries) {
                ids.add(entry.getTitleId());
                if (entry.isSelected()) selected = entry.getTitleId();
            }
            owned.put(player, new Owned(List.copyOf(ids), selected));
        });
    }

    public void unload(UUID player) {
        owned.remove(player);
    }

    public boolean exists(String id) {
        return looks.containsKey(id);
    }

    public List<String> owned(UUID player) {
        var o = owned.get(player);
        return o == null ? List.of() : o.ids();
    }

    @Nullable
    public String selected(UUID player) {
        var o = owned.get(player);
        return o == null ? null : o.selected();
    }

    /** Gives the player a title; their first one is shown straight away. */
    public CompletableFuture<Void> grant(UUID player, String id) {
        var o = owned.get(player);
        if (o != null && !o.ids().contains(id)) {
            var ids = new ArrayList<>(o.ids());
            ids.add(id);
            owned.put(player, new Owned(List.copyOf(ids), o.selected() == null ? id : o.selected()));
        }
        boolean showIt = o != null && o.selected() == null;
        return CompletableFuture.runAsync(() -> {
            Tables.titleTable.grant(player, id);
            if (showIt) Tables.titleTable.select(player, id);
        });
    }

    /** Shows one of the player's titles, or none for null. False if they don't have it. */
    public boolean select(UUID player, @Nullable String id) {
        var o = owned.get(player);
        if (o == null || (id != null && !o.ids().contains(id))) return false;
        owned.put(player, new Owned(o.ids(), id));
        CompletableFuture.runAsync(() -> Tables.titleTable.select(player, id));
        return true;
    }

    /** A title as MiniMessage, as titles.yml has it. */
    public String look(String id) {
        return looks.getOrDefault(id, id);
    }

    public String plainName(String id) {
        return PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(look(id)));
    }

    /** The title the player shows, as MiniMessage, or "" for none. */
    public String shown(UUID player) {
        var id = selected(player);
        return id == null ? "" : look(id);
    }
}
