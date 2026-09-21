package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.quest.TaskCategory;

/**
 * Maps a {@link TaskCategory} to its Bedrock quest icon, used to give {@link BedrockQuestsMenu} grid
 * tiles proper icons on Geyser clients (the Java menu uses custom GardensFurniture items, which Bedrock
 * can't render).
 *
 * <p>The bundled pack ships a texture per category under {@code textures/gardens/quests/} — both the
 * base and {@code _completed} variants — named with the same {@code icon_<category>_task} convention as
 * the Java custom items (see {@link TaskCategory#getIconItemId(boolean)}), so every category resolves to
 * a real icon.
 */
final class QuestIcons {

    private static final String ICON_DIR = "textures/gardens/quests/";

    private QuestIcons() {
    }

    /** Bedrock texture path for a category's quest icon; the {@code _completed} variant when done. */
    static String texturePath(TaskCategory category, boolean completed) {
        return ICON_DIR + category.getIconItemId(completed);
    }
}
