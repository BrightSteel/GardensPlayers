package com.gardensmc.gardensplayers.reward;

import de.tr7zw.nbtapi.NBT;
import emanondev.itemedit.ItemEdit;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Objects;

@Getter
public class Reward {

    private final String id;
    private final String itemNBT;
    private final int amount;
    private final String permission;
    private Instant expiration;

    public Reward(String identifier, ConfigurationSection rewardSection) {
        this.id = identifier;
        this.amount = rewardSection.getInt("amount", 1);
        var serverItemStr = rewardSection.getString("server_item");
        var serverItem = ItemEdit.get().getServerStorage().getItem(Objects.requireNonNull(serverItemStr));
        serverItem.setAmount(amount);
        this.itemNBT = NBT.itemStackToNBT(serverItem).toString();
        this.permission = rewardSection.getString("permission");
        var expirationStr = rewardSection.getString("expiration");
        if (expirationStr != null) {
            this.expiration = Instant.parse(expirationStr);
        }
    }
}
