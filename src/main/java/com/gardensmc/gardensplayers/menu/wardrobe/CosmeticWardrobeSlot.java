package com.gardensmc.gardensplayers.menu.wardrobe;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.wrap.WrapHandler;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.player.PlayerEntryHandler;
import com.hibiscusmc.hmccosmetics.api.HMCCosmeticsAPI;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;

public class CosmeticWardrobeSlot implements WardrobeSlot {

    private final CosmeticSlot cosmeticSlot;

    public CosmeticWardrobeSlot(CosmeticSlot cosmeticSlot) {
        this.cosmeticSlot = cosmeticSlot;
    }

    @Override
    public ItemStack getItem(Player player) {
        var wardrobe = PlayerEntryCache.INSTANCE.getSync(player.getUniqueId()).getWardrobe();
        if (wardrobe != null) {
            if (cosmeticSlot == CosmeticSlot.HELMET) {
                return wardrobe.getHelmet();
            }
            if (cosmeticSlot == CosmeticSlot.BACKPACK) {
                return wardrobe.getBackpack();
            }
            if (cosmeticSlot == CosmeticSlot.OFFHAND) {
                return wardrobe.getOffhand();
            }
            if (cosmeticSlot == CosmeticSlot.BALLOON) {
                return wardrobe.getBalloon();
            }
        }
        return null;
    }

    @Override
    public boolean isValidItem(ItemStack itemStack) {
        var cosmetic = getCosmetic(itemStack);
        return cosmetic != null && cosmetic.getSlot() == cosmeticSlot;
    }

    @Override
    public void updateWardrobe(Player player, ItemStack itemStack) {
        var isNullOrAir = itemStack == null || itemStack.getType() == Material.AIR;
        // save changes to db
        var nbt = isNullOrAir ? null : NBT.itemStackToNBT(itemStack).toString();
        try {
            new PlayerEntryHandler(player.getUniqueId()).updateWardrobe(cosmeticSlot, nbt).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // update cosmetic
        var user = HMCCosmeticsAPI.getUser(player.getUniqueId());
        if (user == null) {
            return;
        }
        if (isNullOrAir) {
            user.removeCosmeticSlot(cosmeticSlot);
        } else {
            var cosmetic = getCosmetic(itemStack);
            if (cosmetic != null) {
                user.addCosmetic(cosmetic, itemStack.getItemMeta() instanceof LeatherArmorMeta meta ? meta.getColor() : null);
            }
        }
    }

    @Nullable
    private Cosmetic getCosmetic(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }

        // prioritise wraps
        var wrapItem = new WrapHandler(itemStack).getWrapFromItem();
        if (wrapItem != null && wrapItem.getCosmeticId() != null) {
            return HMCCosmeticsAPI.getCosmetic(wrapItem.getCosmeticId());
        }

        var furniture = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        if (furniture == null || furniture.getCosmeticId() == null) {
            return null;
        }
        return HMCCosmeticsAPI.getCosmetic(furniture.getCosmeticId());
    }
}
