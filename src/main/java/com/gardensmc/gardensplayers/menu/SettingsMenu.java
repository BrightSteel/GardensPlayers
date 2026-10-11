package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.GardensPlayers;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.user.SavedUser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * /settings: the player's settings, as a popup over their profile (dimmed), as /homes is, without its page row. Each
 * row is a setting: its icon, and its toggle (the player_warps pack's, an item two slots wide), its name in their
 * tooltips. The pack's gardens:settings font draws the background, the popup and its close X.
 * <ul>
 *     <li>Teleport requests: whether others can send them (HuskHomes' /tpignore).</li>
 *     <li>Player sitting: whether others can sit on them, and they on others (GSit's, through its API).</li>
 *     <li>Who can message me: everyone, friends or nobody (EnderFriends' /f messages; read through its API).</li>
 *     <li>Friend requests: whether they take new ones (EnderFriends' /f requests; read through its API).</li>
 *     <li>Public profile: whether others can see their /profile (EnderWardrobe's API).</li>
 * </ul>
 * GSit and EnderFriends are reached by reflection, so neither is needed to build; a row whose plugin isn't there is
 * left out.
 */
public class SettingsMenu extends Menu {

    // as /profile, 5 rows: the popup's title and close X in row 0 (the X on column 7's slot), then two settings a row,
    // each its icon and its toggle right after it (over two slots): columns 1 to 3, and 5 to 7
    private static final String[] LAYOUT = new String[] {
            "z      xz",
            "zitT suUz",
            "zmvV frRz",
            "zpwW    z",
            "zzzzzzzzz"
    };

    private static final Key TOGGLE_ON = Key.key("gardens", "settings/toggle_on");
    private static final Key TOGGLE_OFF = Key.key("gardens", "settings/toggle_off");
    private static final Key TOGGLE_FRIENDS = Key.key("gardens", "settings/toggle_friends");
    private static final String GSIT_API = "dev.geco.gsit.api.GSitAPI";
    private static final List<String> WHO = List.of("everyone", "friends", "nobody");

    private boolean teleportRequests;
    /** Whether others can sit on them (and they on others); null without GSit. */
    private Boolean playerSitting;
    /** Who can message them: "everyone", "friends" or "nobody"; null without EnderFriends. */
    private String whoCanMessage;
    /** Whether they take new friend requests; null without EnderFriends (or one from before it had this). */
    private Boolean friendRequests;

    /** Whether others can see their /profile (EnderWardrobe's). */
    private boolean profilePublic;

    private SettingsMenu(Player viewer, boolean teleportRequests, Boolean playerSitting, String whoCanMessage,
                         Boolean friendRequests) {
        super(LAYOUT, MiniMessage.miniMessage().deserialize(title()), viewer);
        this.teleportRequests = teleportRequests;
        this.playerSitting = playerSitting;
        this.whoCanMessage = whoCanMessage;
        this.friendRequests = friendRequests;
        this.profilePublic = wardrobe() == null || wardrobe().isProfilePublic(viewer.getUniqueId());
        addElements();
    }

    /** Opens the viewer's settings, once HuskHomes and EnderFriends have them. */
    public static void open(Player viewer) {
        var teleports = HuskHomesAPI.getInstance().getUserData(viewer.getUniqueId())
                .thenApply(user -> !user.map(SavedUser::isIgnoringTeleports).orElse(false));
        CompletableFuture<String> messages = fromEnderFriends("whoCanMessage", viewer.getUniqueId());
        CompletableFuture<Boolean> requests = fromEnderFriends("takesFriendRequests", viewer.getUniqueId());
        teleports.thenCombine(messages, (tp, who) -> new Object[] {tp, who}).thenCombine(requests, (got, takes) -> {
            Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                if (!viewer.isOnline()) return;
                // over whatever's open (the profile): no history to go back to
                InventoryGui.clearHistory(viewer);
                new SettingsMenu(viewer, (Boolean) got[0], playerSitting(viewer.getUniqueId()), (String) got[1], takes).open();
            });
            return null;
        });
    }

    // the background (gardens:settings , from the chest's left edge) and "Settings" in the popup
    private static String title() {
        return "<white><font:gardens:settings></font></white>"
                + "<#494949><font:gardens:homes_title>Settings</font></#494949>";
    }

    private void addElements() {
        // the close X, or anywhere outside the popup, closes it: back to the profile behind it
        gui.addElement(new StaticGuiElement('x', blankItem, click -> {
            backToProfile();
            return true;
        }, "<white>Close"));
        gui.addElement(new StaticGuiElement('z', blankHiddenItem, click -> {
            backToProfile();
            return true;
        }));

        // teleport requests
        gui.addElement(new StaticGuiElement('i', new ItemStack(Material.ENDER_PEARL), click -> true,
                "<white>Teleport Requests", "<gray>Whether other players can send you", "<gray>teleport requests (/tpa)"));
        addToggle('t', 'T', () -> teleportRequests ? TOGGLE_ON : TOGGLE_OFF, () -> {
            // HuskHomes' own command: its permission, its message, and it saves it
            viewer.performCommand("huskhomes:tpignore");
            teleportRequests = !teleportRequests;
        }, () -> lore("Teleport Requests", teleportRequests ? "<green>On: <gray>others can send you requests"
                : "<red>Off: <gray>requests to you are blocked", "Turn " + (teleportRequests ? "off" : "on")));

        // player sitting
        if (playerSitting != null) {
            gui.addElement(new StaticGuiElement('s', new ItemStack(Material.SADDLE), click -> true,
                    "<white>Player Sitting", "<gray>Whether players can sit on your head,", "<gray>and you on theirs"));
            addToggle('u', 'U', () -> playerSitting ? TOGGLE_ON : TOGGLE_OFF, () -> {
                if (setPlayerSitting(viewer.getUniqueId(), !playerSitting)) playerSitting = !playerSitting;
            }, () -> lore("Player Sitting", playerSitting ? "<green>On: <gray>players can sit on you"
                    : "<red>Off: <gray>nobody can sit on you", "Turn " + (playerSitting ? "off" : "on")));
        }

        // who can message me: everyone, then friends, then nobody, then everyone again
        if (whoCanMessage != null) {
            gui.addElement(new StaticGuiElement('m', new ItemStack(Material.WRITABLE_BOOK), click -> true,
                    "<white>Who Can Message Me", "<gray>Who can send you private", "<gray>messages (/msg)"));
            addToggle('v', 'V', () -> switch (whoCanMessage) {
                case "friends" -> TOGGLE_FRIENDS;
                case "nobody" -> TOGGLE_OFF;
                default -> TOGGLE_ON;
            }, () -> {
                var next = WHO.get((WHO.indexOf(whoCanMessage) + 1) % WHO.size());
                // EnderFriends' own command: its message, and it saves it
                viewer.performCommand("enderfriends:friend messages " + next);
                whoCanMessage = next;
            }, () -> lore("Who Can Message Me", switch (whoCanMessage) {
                case "friends" -> "<aqua>Friends: <gray>only your friends";
                case "nobody" -> "<red>Nobody: <gray>messages to you are blocked";
                default -> "<green>Everyone: <gray>any player";
            }, "Change to " + WHO.get((WHO.indexOf(whoCanMessage) + 1) % WHO.size())));
        }

        // public profile: whether others can see it (/profile name)
        if (wardrobe() != null) {
            gui.addElement(new StaticGuiElement('p', new ItemStack(Material.ARMOR_STAND), click -> true,
                    "<white>Public Profile", "<gray>Whether other players can see", "<gray>your profile (/profile)"));
            addToggle('w', 'W', () -> profilePublic ? TOGGLE_ON : TOGGLE_OFF, () -> {
                profilePublic = !profilePublic;
                wardrobe().setProfilePublic(viewer.getUniqueId(), profilePublic);
            }, () -> lore("Public Profile", profilePublic ? "<green>On: <gray>anyone can see your profile"
                    : "<red>Off: <gray>only you can see it", "Turn " + (profilePublic ? "off" : "on")));
        }

        // friend requests
        if (friendRequests != null) {
            gui.addElement(new StaticGuiElement('f', new ItemStack(Material.PLAYER_HEAD), click -> true,
                    "<white>Friend Requests", "<gray>Whether players can send you", "<gray>new friend requests (/f add)"));
            addToggle('r', 'R', () -> friendRequests ? TOGGLE_ON : TOGGLE_OFF, () -> {
                // EnderFriends' own command: its message, and it saves it
                viewer.performCommand("enderfriends:friend requests " + (friendRequests ? "off" : "on"));
                friendRequests = !friendRequests;
            }, () -> lore("Friend Requests", friendRequests ? "<green>On: <gray>players can send you requests"
                    : "<red>Off: <gray>new requests to you are blocked", "Turn " + (friendRequests ? "off" : "on")));
        }
    }

    // EnderWardrobe's API (it's a dependency): its /profile's visibility
    private static com.enderbeam.enderwardrobe.api.WardrobeApi wardrobe() {
        return Bukkit.getServicesManager().load(com.enderbeam.enderwardrobe.api.WardrobeApi.class);
    }

    /** A toggle: drawn by the item in its left slot, over both; either one flips it. */
    private void addToggle(char left, char right, java.util.function.Supplier<Key> model, Runnable flip,
                           java.util.function.Supplier<String[]> lore) {
        GuiElement.Action action = click -> {
            flip.run();
            gui.draw();
            return true;
        };
        gui.addElement(new DynamicGuiElement(left, () -> {
            var item = new ItemStack(Material.PAPER);
            item.setData(DataComponentTypes.ITEM_MODEL, model.get());
            return new StaticGuiElement(left, item, action, lore.get());
        }));
        gui.addElement(new DynamicGuiElement(right, () -> new StaticGuiElement(right, blankItem, action, lore.get())));
    }

    private static String[] lore(String name, String state, String action) {
        return new String[] {"<white>" + name, state, " ", "<white>괧 <gray>" + action};
    }

    // GSit: whether this player can sit on others (and be sat on); null without GSit
    private static Boolean playerSitting(UUID player) {
        try {
            return (Boolean) Class.forName(GSIT_API).getMethod("canPlayerUsePlayerSit", UUID.class).invoke(null, player);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            GardensPlayers.plugin.getLogger().log(Level.WARNING, "Couldn't read player sitting from GSit", e);
            return null;
        }
    }

    private static boolean setPlayerSitting(UUID player, boolean on) {
        try {
            Class.forName(GSIT_API).getMethod("setPlayerCanUsePlayerSit", UUID.class, boolean.class).invoke(null, player, on);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            GardensPlayers.plugin.getLogger().log(Level.WARNING, "Couldn't set player sitting in GSit", e);
            return false;
        }
    }

    // EnderFriends: one of its settings for this player (whoCanMessage, takesFriendRequests); completes with null without
    // EnderFriends (or when it can't be read)
    @SuppressWarnings("unchecked")
    private static <T> CompletableFuture<T> fromEnderFriends(String method, UUID player) {
        var plugin = Bukkit.getPluginManager().getPlugin("EnderFriends");
        if (plugin == null || !plugin.isEnabled()) return CompletableFuture.completedFuture(null);
        try {
            var future = (CompletableFuture<T>) plugin.getClass().getMethod(method, UUID.class).invoke(plugin, player);
            return future.exceptionally(e -> null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // an EnderFriends from before it had this
            return CompletableFuture.completedFuture(null);
        }
    }
}
