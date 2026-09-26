package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.GardensPlayers;
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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RewardsMenu extends Menu {

    private static final String[] LAYOUT = new String[] {
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "bbb   ccc"
    };

    // reward ids this menu has a claim in flight for (main thread only)
    private final Set<String> claiming = new HashSet<>();

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
        // ignore repeat clicks while this claim is in flight; the database guard covers everything else
        if (!claiming.add(rewardEntry.getId())) {
            return;
        }
        var uuid = viewer.getUniqueId();
        CompletableFuture
                .supplyAsync(() -> Tables.rewardsTable.markRewardClaimed(uuid.toString(), rewardEntry.getId()))
                .whenComplete((claimed, error) -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                    claiming.remove(rewardEntry.getId());
                    PlayerRewardsCache.INSTANCE.remove(uuid);
                    if (error != null) {
                        GardensPlayers.plugin.getLogger().log(Level.WARNING, "Failed to claim reward " + rewardEntry.getId(), error);
                        return;
                    }
                    if (!viewer.isOnline()) {
                        if (Boolean.TRUE.equals(claimed)) {
                            // logged out before it could be given: leave it waiting in /rewards
                            CompletableFuture.runAsync(() -> Tables.rewardsTable.markRewardUnclaimed(uuid.toString(), rewardEntry.getId()));
                        }
                        return;
                    }
                    if (Boolean.TRUE.equals(claimed)) {
                        PlayerUtil.giveItems(viewer, List.of(rewardItem));
                        viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Claimed <white>").append(rewardItem.displayName()));
                    }
                    open(() -> new RewardsMenu(viewer));
                }));
    }
}
