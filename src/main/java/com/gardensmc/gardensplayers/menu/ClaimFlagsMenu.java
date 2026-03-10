package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.menu.flag.Flag;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ClaimFlagsMenu extends Menu {

    private static final String[] layout = new String[] {
            "         ",
            " aaaaaaa ",
            " aaaaaaa ",
            " aaaaaaa ",
            "         ",
            "         "
    };

    public ClaimFlagsMenu(Player viewer) {
        super(layout, "", viewer);
    }

    @Override
    protected void initializeElements() {
        var flagGroup = new GuiElementGroup('a');
        var flags = new ArrayList<Flag>();
        flagGroup.addElements(flags.stream().map(this::createFlagElement).collect(Collectors.toSet()));
        gui.addElement(flagGroup);
    }

    private GuiElement createFlagElement(Flag flag) {
        return new StaticGuiElement('a', flag.getItem(), click -> {
            flag.onClick(click);

            return true;
        }, flag.getText());
    }
}
