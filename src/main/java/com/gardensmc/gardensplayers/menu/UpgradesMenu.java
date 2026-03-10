package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.currency.GemsCurrencyHandler;
import com.gardensmc.gardensplayers.util.FontShift;
import com.gardensmc.gardensplayers.util.PermissionUtil;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.function.Supplier;

public class UpgradesMenu extends Menu {

    private static final String[] layout = new String[] {
            "    a    ",
            "         ",
            " b  c  d ",
            "         ",
            " e  f  g ",
            "         "
    };
    private static final String MAX_HOMES_PERMISSION = "huskhomes.max_homes";
    private static final String MAX_WARPS_PERMISSION = "gplayers.max_warps";
    private static final String MAX_EC_PERMISSION = "betterenderchest.slots";

    private static final ItemStack UPGRADE_AT_MAX_ICON = GardensFurniture.itemRegistry.buildItem("upgrade_at_max_button");
    private static final ItemStack UPGRADE_ICON = GardensFurniture.itemRegistry.buildItem("upgrade_button");
    private static final ItemStack UPGRADE_NO_BALANCE_ICON = GardensFurniture.itemRegistry.buildItem("upgrade_no_balance_button");

    private static final String HOMES_UPGRADE = "Homes";
    private static final String WARPS_UPGRADE = "Player Warps";
    private static final String EC_UPGRADE = "Enderchest Rows";

    private static final int HOMES_MAX = 5;
    private static final int WARPS_MAX = 5;
    private static final int EC_MAX = 3;

    public UpgradesMenu(Player viewer) {
        super(layout, "<font:gardens:default>" + FontShift.INSTANCE.getShift(-48) + "\u0137", viewer);
    }

    @Override
    protected void initializeElements() {
        gui.addElement(homeButton());
        gui.addElements(
                upgradeDisplay('b', MAX_HOMES_PERMISSION, HOMES_MAX, HOMES_UPGRADE),
                upgradeButton('e', MAX_HOMES_PERMISSION, HOMES_MAX, 10, HOMES_UPGRADE),

                upgradeDisplay('c', MAX_WARPS_PERMISSION, WARPS_MAX, WARPS_UPGRADE),
                upgradeButton('f', MAX_WARPS_PERMISSION, WARPS_MAX, 10, WARPS_UPGRADE),

                upgradeDisplay('d', MAX_EC_PERMISSION, EC_MAX, EC_UPGRADE),
                upgradeButton('g', MAX_EC_PERMISSION, EC_MAX, 14, EC_UPGRADE)
        );
    }

    private StaticGuiElement homeButton() {
        return new StaticGuiElement('a', blankItem, click -> {
            InventoryGui.clearHistory(viewer);
            viewer.chat("/shop");
            return true;
        }, "<#DF2E38>← ᴍᴀɪɴ ᴍᴇɴᴜ");
    }

    private DynamicGuiElement upgradeDisplay(char character, String permission, int max, String upgradeName) {
        return new DynamicGuiElement(character, () -> {
            var amount = permission.equals(MAX_EC_PERMISSION) ? getOwnedECUpgrades() + 3 : PermissionUtil.getMaxPermission(viewer, permission);
            var finalMax = permission.equals(MAX_EC_PERMISSION) ? max + 3 : max;
            var item = GardensFurniture.itemRegistry.buildItem(upgradeName.toLowerCase().replace(" ", "_") + "_button");
            Objects.requireNonNull(item).setAmount(amount);
            return new StaticGuiElement(
                    character,
                    item,
                    "<green>" + upgradeName,
                    "<gray>Unlocked: <green>" + amount + "<gray>/" + finalMax
            );
        });
    }

    private DynamicGuiElement upgradeButton(char character, String permission, int max, int price, String upgradeName) {
        return new DynamicGuiElement(character, () -> buildUpgradeButton(character, permission, max, price, upgradeName).get());
    }

    private Supplier<GuiElement> buildUpgradeButton(char character, String permission, int max, int price, String upgradeName) {
        return () -> {
            var amountOwned = permission.equals(MAX_EC_PERMISSION) ? getOwnedECUpgrades() : PermissionUtil.getMaxPermission(viewer, permission);
            if (amountOwned >= max) {
                return new StaticGuiElement(
                        character,
                        UPGRADE_AT_MAX_ICON,
                        "<gold>Max Upgrades Reached"
                );
            }
            if (GemsCurrencyHandler.INSTANCE.getCurrency(viewer) >= price) {
                return new StaticGuiElement(
                        character,
                        UPGRADE_ICON,
                        click -> {
                            if (GemsCurrencyHandler.INSTANCE.getCurrency(viewer) >= price) {
                                GemsCurrencyHandler.INSTANCE.subtractGems(viewer, price);
                                var permissionUpgrade = permission.equals(MAX_EC_PERMISSION)
                                        ? MAX_EC_PERMISSION + ".upgrade" + (amountOwned + 1)
                                        : permission + "." + (amountOwned + 1);
                                GardensPlayers.permission.playerAdd(viewer, permissionUpgrade);
                                viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Upgraded " + upgradeName + "!"));
                                gui.draw();
                            }
                            return true;
                        },
                        "<green>Upgrade",
                        " <gray>▪ ʙᴜʏ ᴘʀɪᴄᴇ: <#82CD47>" + price + " Gems",
                        " ",
                        "<white>괧 <gray>Buy"
                );
            }
            return new StaticGuiElement(
                    character,
                    UPGRADE_NO_BALANCE_ICON,
                    "<green>Upgrade",
                    " <gray>▪ ʙᴜʏ ᴘʀɪᴄᴇ: <#82CD47>" + price + " Gems",
                    " ",
                    "<gray>Not enough gems!"
            );
        };
    }

    private int getOwnedECUpgrades() {
        if (viewer.hasPermission(MAX_EC_PERMISSION + ".upgrade3")) {
            return 3;
        }
        if (viewer.hasPermission(MAX_EC_PERMISSION + ".upgrade2")) {
            return 2;
        }
        if (viewer.hasPermission(MAX_EC_PERMISSION + ".upgrade1")) {
            return 1;
        }
        return 0;
    }
}
