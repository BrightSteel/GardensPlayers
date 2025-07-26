package com.gardensmc.gardensplayers.cache;

import java.util.UUID;

public record ShopLimitKey(UUID playerUUID, String shopItem) {
}
