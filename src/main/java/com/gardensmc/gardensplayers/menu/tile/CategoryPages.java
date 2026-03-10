package com.gardensmc.gardensplayers.menu.tile;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CategoryPages {

    private final List<CategoryPage> pages = new ArrayList<>();

    public CategoryPages(List<TileCategory> tileCategories) {
        buildPages(tileCategories);
    }

    public CategoryPage getPage(int page) {
        return pages.get(page);
    }

    private void buildPages(List<TileCategory> tileCategories) {
        var page = new CategoryPage();
        pages.add(page);
        for (int i = 0; i < tileCategories.size(); i++) {
            page.addTileCategory(tileCategories.get(i));
            if (page.atMaxCategories(pages.size() == 1, tileCategories.size() - (i + 1))) {
                if (i + 1 < tileCategories.size()) {
                    page.setHasNextPage(true);
                }
                page = new CategoryPage();
                pages.add(page);
            }
        }
    }

    @Setter
    @Getter
    public static class CategoryPage {

        private static final int MAX_SPACE = 9;

        private final List<TileCategory> categories = new ArrayList<>();
        private boolean hasNextPage;

        public void addTileCategory(TileCategory tileCategory) {
            categories.add(tileCategory);
        }

        public boolean atMaxCategories(boolean isFirstPage, int categoriesLeft) {
            int remainingSpace = MAX_SPACE;
            remainingSpace -= categories.size();
            if (!isFirstPage) {
                remainingSpace--; // leave space for prev button
            }
            if (remainingSpace - categoriesLeft >= 0) {
                // fits within remaining space, no more pages needed
                return false;
            }
            remainingSpace--; // leave space for next button
            return remainingSpace <= 0;
        }
    }
}
