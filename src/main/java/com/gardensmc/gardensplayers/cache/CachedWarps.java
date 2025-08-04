package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.database.entry.WarpEntry;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public record CachedWarps(
        List<WarpEntry> warps,
        Map<UUID, List<WarpEntry>> warpsByPlayer,
        Map<WarpEntry.Category, List<WarpEntry>> warpsByCategory
) {

    public static CachedWarps create(List<WarpEntry> warps) {
        var warpsByPlayer = warps.stream()
                .sorted()
                .collect(Collectors.groupingBy(
                        WarpEntry::getPlayerUUID, Collectors.mapping(Function.identity(), Collectors.toList())
                ));
        var warpsByCategory = warps.stream()
                .filter(WarpEntry::isPublicAccess)
                .sorted()
                .collect(Collectors.groupingBy(
                        WarpEntry::getCategory, Collectors.mapping(Function.identity(), Collectors.toList())
                ));
        return new CachedWarps(warps, warpsByPlayer, warpsByCategory);
    }
}
