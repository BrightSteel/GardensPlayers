package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.menu.TileMenu;
import com.hibiscusmc.hmccosmetics.api.HMCCosmeticsAPI;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import de.themoep.inventorygui.GuiElement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CosmeticTileItem extends TileItem {

    private final Cosmetic cosmetic;

    public CosmeticTileItem(Cosmetic cosmetic) {
        super(false);
        this.cosmetic = cosmetic;
    }

    @Override
    public ItemStack getItemStack(Player viewer) {
        return cosmetic.getItem();
    }

    @Override
    public void onClick(GuiElement.Click click, TileMenu tileMenu) {
        Player player = (Player) click.getWhoClicked();
        var cosmeticUser = HMCCosmeticsAPI.getUser(player.getUniqueId());
        if (cosmeticUser == null) {
            return; // shouldn't happen ?
        }
        if (cosmeticUser.getCosmetics().contains(cosmetic)) {
            cosmeticUser.removeCosmeticSlot(cosmetic);
        } else if (isOwnedCosmetic(player)) {
            cosmeticUser.addCosmetic(cosmetic);
        }
    }

    @Override
    public TileColor getTileColor(Player viewer) {
        var cosmeticUser = HMCCosmeticsAPI.getUser(viewer.getUniqueId());
        if (cosmeticUser != null && cosmeticUser.hasCosmeticInSlot(cosmetic)) {
            return TileColor.GREEN;
        }
        if (isOwnedCosmetic(viewer)) {
            return TileColor.GRAY;
        }
        return TileColor.RED;
    }

    private boolean isOwnedCosmetic(Player viewer) {
        return !cosmetic.requiresPermission() || viewer.hasPermission(cosmetic.getPermission());
    }
}
