package com.gardensmc.gardensplayers.database.entry;

import de.tr7zw.nbtapi.NBT;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

@Setter
@Accessors(chain = true)
public class WardrobeEntry extends MySQLEntry {

    private String helmet, backpack, offhand, balloon;

    @Override
    public WardrobeEntry populate(ResultSet resultSet) throws SQLException {
        this.helmet = resultSet.getString("helmet");
        this.backpack = resultSet.getString("backpack");
        this.offhand = resultSet.getString("offhand");
        this.balloon = resultSet.getString("balloon");
        return this;
    }

    public boolean hasBackpack() {
        return backpack != null;
    }

    public ItemStack getHelmet() {
        return toItemStack(helmet);
    }

    public ItemStack getBackpack() {
        return toItemStack(backpack);
    }

    public ItemStack getOffhand() {
        return toItemStack(offhand);
    }

    public ItemStack getBalloon() {
        return toItemStack(balloon);
    }

    @Nullable
    private ItemStack toItemStack(String nbt) {
        if (Strings.isEmpty(nbt)) {
            return null;
        }
        return NBT.itemStackFromNBT(NBT.parseNBT(nbt));
    }

}
