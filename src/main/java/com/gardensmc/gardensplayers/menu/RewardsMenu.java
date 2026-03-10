package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.cache.PlayerRewardsCache;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.RewardEntry;
import com.gardensmc.gardensplayers.util.FontShift;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.StaticGuiElement;
import de.tr7zw.nbtapi.NBT;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RewardsMenu extends Menu {

    private static final String[] LAYOUT = new String[] {
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "bbb   ccc"
    };

    public RewardsMenu(Player viewer) {
        super(LAYOUT,  "<font:gardens:default>" + FontShift.INSTANCE.getShift(-8) + "\u0152</font>", viewer);
    }

    @Override
    protected void initializeElements() {
        var rewards = PlayerRewardsCache.INSTANCE.getSync(viewer.getUniqueId());
        var rewardsGrp = new GuiElementGroup('a');
        rewardsGrp.addElements(rewards.stream().map(this::makeRewardElement).collect(Collectors.toList()));
        gui.addElement(rewardsGrp);

        gui.addElement(new GuiPageElement('b', blankItem, GuiPageElement.PageAction.PREVIOUS, "<white>Back"));
        gui.addElement(new GuiPageElement('c', blankItem, GuiPageElement.PageAction.NEXT, "<white>Next"));
    }

    private GuiElement makeRewardElement(RewardEntry rewardEntry) {
        var rewardItem = NBT.itemStackFromNBT(NBT.parseNBT(rewardEntry.getReward()));
        return new StaticGuiElement('a', rewardItem, click -> {
            claimReward(rewardEntry, rewardItem);
            return true;
        });
    }

    private void claimReward(RewardEntry rewardEntry, ItemStack rewardItem) {
        CompletableFuture
                .runAsync(() -> Tables.rewardsTable.markRewardClaimed(viewer.getUniqueId().toString(), rewardEntry.getId()))
                .thenRun(() -> {
                    PlayerRewardsCache.INSTANCE.remove(viewer.getUniqueId());
                    open(() -> new RewardsMenu(viewer));
                    PlayerUtil.giveItems(viewer, List.of(rewardItem));
                    viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Claimed <white>").append(rewardItem.displayName()));
                });
    }
}
