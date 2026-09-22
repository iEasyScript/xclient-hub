package net.runelite.client.plugins.projectx.kittentracker;


import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.util.Global;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.ProjectX;

import javax.inject.Inject;


public class FeedKittenEvent implements BlockingEvent {
    private final KittenPlugin kittenPlugin;
    @Inject
    public FeedKittenEvent(KittenPlugin kittenPlugin) {
        this.kittenPlugin = kittenPlugin;
    }

    @Override
    public boolean validate() {
        return Rs2Inventory.contains(ItemID.TBWT_RAW_KARAMBWANJI)
                && (KittenPlugin.HUNGRY_FIRST_WARNING_TIME_LEFT_IN_SECONDS * 1000) >= kittenPlugin.getTimeBeforeHungry() && (kittenPlugin.playerHasFollower() && kittenPlugin.isKitten());

    }

    @Override
    public boolean execute() {
        ProjectX.getRs2NpcCache().query().withName("Kitten").toListOnClientThread().stream().findFirst().ifPresent(kitten -> Rs2Inventory.useItemOnNpc(ItemID.TBWT_RAW_KARAMBWANJI, kitten.getNpc()));
        Global.sleepUntil(() -> (KittenPlugin.HUNGRY_FIRST_WARNING_TIME_LEFT_IN_SECONDS * 1000) < kittenPlugin.getTimeBeforeHungry());
        return true;
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.NORMAL;
    }
}
