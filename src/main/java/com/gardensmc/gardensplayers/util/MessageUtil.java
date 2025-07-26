package com.gardensmc.gardensplayers.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;

import java.util.Arrays;

public class MessageUtil {

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
