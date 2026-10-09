package com.gardensmc.gardensplayers.menu.tile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@RequiredArgsConstructor
@Setter
@Getter
public class TileCategory {

    private final String id;
    private final String displayName;
    private final String iconId;

    private final List<? extends TileItem> items;

    /** Lines under its name on its tab (MiniMessage); none unless set. */
    private List<String> lore = List.of();

}
