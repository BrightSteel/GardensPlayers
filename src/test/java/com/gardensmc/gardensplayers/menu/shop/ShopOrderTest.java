package com.gardensmc.gardensplayers.menu.shop;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopOrderTest {

    /** A shop as its file id and display name. */
    private record S(String id, String name) {
    }

    private static List<String> order(List<String> wanted) {
        var shops = List.of(new S("runes", "Enchanting"), new S("blocks", "Blocks"), new S("music", "Music & Banners"),
                new S("armor", "Armor & Tools"), new S("foliage", "Foliage"));
        return ShopOrder.sort(shops, S::id, S::name, wanted).stream().map(S::id).toList();
    }

    @Test
    void listedShopsComeInTheListsOrder() {
        assertEquals(List.of("music", "blocks", "foliage", "armor", "runes"), order(List.of("music", "blocks", "foliage", "armor", "runes")));
    }

    @Test
    void unlistedShopsFollowByName() {
        // Armor & Tools, Enchanting, Music & Banners: by name after the listed two
        assertEquals(List.of("foliage", "blocks", "armor", "runes", "music"), order(List.of("foliage", "blocks")));
    }

    @Test
    void noOrderIsByName() {
        assertEquals(List.of("armor", "blocks", "runes", "foliage", "music"), order(List.of()));
    }

    @Test
    void namesInTheListIgnoreCaseAndUnknownOnesAreSkipped() {
        assertEquals(List.of("blocks", "armor", "runes", "foliage", "music"), order(List.of("nope", "BLOCKS")));
    }
}
