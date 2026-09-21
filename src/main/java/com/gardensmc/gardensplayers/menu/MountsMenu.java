package com.gardensmc.gardensplayers.menu;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.StaticGuiElement;
import fr.nocsy.mcpets.data.Pet;
import org.bukkit.entity.Player;

import java.util.List;

public class MountsMenu extends Menu {

    private static final String[] LAYOUT = new String[]{
            "         ",
            " aaaaaaa ",
            "baaaaaaac",
            "baaaaaaac",
            " aaaaaaa ",
            "         "
    };

    private final List<Pet> pets;

    public MountsMenu(Player viewer) {
        super(LAYOUT, "<font:gardens:default>" + getShift(-8) + "\u015B</font>", viewer);
        this.pets = Pet.getAvailablePets(viewer).stream().filter(Pet::isMountable).toList();
        initElements();
    }

    public void initElements() {
        var mountGrp = new GuiElementGroup('a');
        mountGrp.addElements(pets.stream().map(this::buildPetElement).toList());
        gui.addElement(mountGrp);

        gui.addElement(new GuiPageElement(
                'b', blankItem, GuiPageElement.PageAction.PREVIOUS, "Back"
        ));
        gui.addElement(new GuiPageElement(
                'c', blankItem, GuiPageElement.PageAction.NEXT, "Next"
        ));
    }

    private GuiElement buildPetElement(Pet pet) {
        return new StaticGuiElement(
                'a',
                pet.getIcon(),
                _ -> {
                    gui.close();
                    pet.spawnWithMessage(viewer);
                    return true;
                }
        );
    }
}
