package com.gardensmc.gardensplayers.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageUtil {

    /**
     * Word-wraps {@code text} so that no line exceeds {@code maxLineLength} characters. Words are kept whole where
     * possible; a single word longer than {@code maxLineLength} is hard-split across lines. Operates on plain text and
     * is unaware of formatting tags, so wrap before applying any.
     */
    public static List<String> wrapText(String text, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            // hard-split any word that can't fit on a line on its own
            while (word.length() > maxLineLength) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                lines.add(word.substring(0, maxLineLength));
                word = word.substring(maxLineLength);
            }
            if (word.isEmpty()) {
                continue;
            }
            int needed = line.isEmpty() ? word.length() : line.length() + 1 + word.length();
            if (needed > maxLineLength) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    public static String readableId(String id) {
        var capitalized = Arrays
                .stream(id.split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .toList();
        return String.join(" ", capitalized);
    }

    public static Component fromMiniMessage(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    public static String toMiniMessage(Component component) {
        return MiniMessage.miniMessage().serialize(component);
    }

    public static String parseIntoLegacy(String message) {
        var mm = MiniMessage.miniMessage();
        var component = mm.deserialize(message);
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    public static String parseIntoLegacy(Component component) {
        var mm = MiniMessage.miniMessage();
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    public static String parseIntoLegacyDisplayable(Component component) {
        return ChatColor.translateAlternateColorCodes(
                LegacyComponentSerializer.AMPERSAND_CHAR,
                LegacyComponentSerializer.legacyAmpersand().serialize(component)
        );
    }

    public static Component parseIntoComponent(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    public static boolean isBlank(Component component) {
        var str = PlainTextComponentSerializer.plainText().serialize(component);
        return str.isBlank();
    }

    public static String toJson(Component component) {
        return JSONComponentSerializer.json().serialize(component);
    }

    public static String toPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static Component fromPlainText(String text) {
        return PlainTextComponentSerializer.plainText().deserialize(text);
    }

    public static Component fromJson(String json) {
        return JSONComponentSerializer.json().deserialize(json);
    }

}
