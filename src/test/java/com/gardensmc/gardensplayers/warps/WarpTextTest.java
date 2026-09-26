package com.gardensmc.gardensplayers.warps;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// warp names and descriptions are shown to other players, so only styling may survive
class WarpTextTest {

    @Test
    void keepsColoursAndStyling() {
        assertEquals("<red>My <bold>Shop", WarpsHandler.cleanText("<red>My <bold>Shop"));
    }

    @Test
    void escapesInteractiveTags() {
        // escaped, so the full MiniMessage used for display shows them as text instead of running them
        var cleaned = WarpsHandler.cleanText("<click:run_command:'/op me'>Free stuff</click>");
        assertEquals("<click:run_command:'/op me'>Free stuff</click>", plain(cleaned));
        var hover = WarpsHandler.cleanText("<hover:show_text:'x'>hi");
        assertEquals("<hover:show_text:'x'>hi", plain(hover));
    }

    private static String plain(String miniMessage) {
        return PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(miniMessage));
    }

    @Test
    void nullStaysNull() {
        assertNull(WarpsHandler.cleanText(null));
    }

    @Test
    void nameLengthCountsVisibleCharacters() {
        assertNull(WarpsHandler.validateName("<gradient:red:blue>" + "a".repeat(WarpsHandler.MAX_NAME_LENGTH)));
        assertNotNull(WarpsHandler.validateName("a".repeat(WarpsHandler.MAX_NAME_LENGTH + 1)));
        assertNotNull(WarpsHandler.validateName("   "));
    }

    @Test
    void rawNameMustFitTheColumn() {
        assertNotNull(WarpsHandler.validateName("<red>".repeat(30) + "a"));
    }

    @Test
    void descriptionLength() {
        assertNull(WarpsHandler.validateDescription(null));
        assertNull(WarpsHandler.validateDescription("a".repeat(200)));
        assertNotNull(WarpsHandler.validateDescription("a".repeat(201)));
    }
}
