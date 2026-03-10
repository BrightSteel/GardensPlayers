package com.gardensmc.gardensplayers.menu.shop;

import com.gardensmc.gardensplayers.GardensPlayers;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Getter
public class ShopsHandler {

    public static final ShopsHandler INSTANCE = new ShopsHandler();
    private List<Shop> shops;


}
