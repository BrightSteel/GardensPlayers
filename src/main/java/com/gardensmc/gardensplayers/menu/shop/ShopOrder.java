package com.gardensmc.gardensplayers.menu.shop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * The order shop categories are shown in: shops/order.yml lists shop files by name (without .yml), and any shop it
 * doesn't list comes after them, by display name.
 */
public final class ShopOrder {

    public static final String FILE = "order.yml";

    private ShopOrder() {
    }

    public static <T> List<T> sort(List<T> shops, Function<T, String> id, Function<T, String> name, List<String> wanted) {
        var order = wanted.stream().map(w -> w.toLowerCase(Locale.ROOT)).toList();
        var sorted = new ArrayList<>(shops);
        sorted.sort(Comparator.<T>comparingInt(shop -> {
                    int at = order.indexOf(id.apply(shop).toLowerCase(Locale.ROOT));
                    return at < 0 ? Integer.MAX_VALUE : at;
                })
                .thenComparing(shop -> String.valueOf(name.apply(shop))));
        return List.copyOf(sorted);
    }
}
