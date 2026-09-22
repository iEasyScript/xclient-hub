// filepath: c:\Users\marcu\IdeaProjects\projectx\runelite-client\src\main\java\net.runelite.client.plugins.projectx\kittentracker\KittenScript.java
package net.runelite.client.plugins.projectx.kittentracker;

import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;

import java.util.concurrent.TimeUnit;

public class KittenScript extends Script {

    private KittenPlugin kittenPlugin;

    public boolean run(KittenConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn()) return;
                if (!super.run()) {
                }



            }
            catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleKittenNeeds(KittenConfig config) {
        if (config.kittenHungryOverlay() 
            && Rs2Inventory.contains(ItemID.TBWT_RAW_KARAMBWANJI)
            && (KittenPlugin.HUNGRY_FIRST_WARNING_TIME_LEFT_IN_SECONDS * 1000) >= kittenPlugin.getTimeBeforeHungry()) {
            feedKitten();
        }
        if (config.kittenAttentionOverlay() 
            && Rs2Inventory.contains(ItemID.BALL_OF_WOOL)
            && (KittenPlugin.ATTENTION_FIRST_WARNING_TIME_LEFT_IN_SECONDS * 1000) >= kittenPlugin.getTimeBeforeNeedingAttention()) {
            giveKittenAttention();
        }
    }

    private void feedKitten() {
        ProjectX.getRs2NpcCache().query().withName("Kitten").toListOnClientThread().stream().findFirst().ifPresent(kitten -> Rs2Inventory.useItemOnNpc(ItemID.TBWT_RAW_KARAMBWANJI, kitten.getNpc()));
        sleep(1000, 2000);
    }

    private void giveKittenAttention() {
        ProjectX.getRs2NpcCache().query().withName("Kitten").toListOnClientThread().stream().findFirst().ifPresent(kitten -> Rs2Inventory.useItemOnNpc(ItemID.BALL_OF_WOOL, kitten.getNpc()));
        sleep(1000, 2000);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
