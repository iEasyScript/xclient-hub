package net.runelite.client.plugins.projectx.natepieshells;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

import java.util.concurrent.TimeUnit;

public class PieScript extends Script {

    public static int totalPieShellsMade = 0;

    public boolean run(PieConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!ProjectX.isLoggedIn()) return;
                if (Rs2Inventory.count("pie dish") > 0 && (Rs2Inventory.count("pastry dough") > 0)) {
                    Rs2Inventory.combine("pie dish", "pastry dough");
                    sleepUntilOnClientThread(() -> Rs2Widget.getWidget(17694734) != null);
                    Rs2Keyboard.keyPress('1');
                    sleepUntilOnClientThread(() -> !Rs2Inventory.hasItem("pie dish"), 25000);

                    totalPieShellsMade += 14;   // rough example, but you get the point
                    return;
                } else {
                    bank();
                }
            } catch (Exception ex) {
                ProjectX.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void bank() {
        Rs2Bank.openBank();
        if (Rs2Bank.isOpen()) {
            Rs2Bank.depositAll();
            if (Rs2Bank.hasItem("pie dish") && Rs2Bank.hasItem("pastry dough")) {
                Rs2Bank.withdrawDeficit("pie dish", 14);
                sleepUntilOnClientThread(() -> Rs2Inventory.hasItem("pie dish"));
                Rs2Bank.withdrawDeficit("pastry dough", 14);
                sleepUntilOnClientThread(() -> Rs2Inventory.hasItem("pastry dough"));
            } else {
                ProjectX.getNotifier().notify("Run out of Materials");
                shutdown();
            }
        }
        Rs2Bank.closeBank();
        sleepUntilOnClientThread(() -> !Rs2Bank.isOpen());
    }
}
