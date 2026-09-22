package net.runelite.client.plugins.projectx.dailytasks;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.util.Rs2InventorySetup;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.bank.enums.BankLocation;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DailyTasksScript extends Script {


    @Inject
    private ClientThread clientThread;

    @Inject
    private DailyTasksPlugin plugin;
    @Inject
    private DailyTasksConfig config;

    private final List<DailyTask> tasksToComplete = new ArrayList<>();
    private DailyTask currentTask = null;
    private boolean initialized = false;

    @Inject
    public DailyTasksScript(DailyTasksPlugin plugin, DailyTasksConfig config) {
        this.plugin = plugin;
        this.config = config;
    }


    @Override
    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!ProjectX.isLoggedIn()) return;

            if (!initialized) {
                initialized = true;
                initializeTasks();
                if (tasksToComplete.isEmpty()) {
                    ProjectX.log("No daily tasks available to complete");
                    ProjectX.stopPlugin(plugin);
                    return;
                }

                var inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
                if (!inventorySetup.doesInventoryMatch() || !inventorySetup.doesEquipmentMatch()) {
                    Rs2Walker.walkTo(Rs2Bank.getNearestBank().getWorldPoint(), 20);
                    if (!inventorySetup.loadEquipment() || !inventorySetup.loadInventory()) {
                        ProjectX.log("Failed to load inventory setup");
                        ProjectX.stopPlugin(plugin);
                        return;
                    }
                    Rs2Bank.closeBank();
                }

                ProjectX.log("Found " + tasksToComplete.size() + " daily tasks to complete");
            }

            if (!super.run()) return;

            if (currentTask == null) {
                if (tasksToComplete.isEmpty()) {
                    DailyTasksPlugin.currentState = "Finished";
                    if (config.goToBank()) {
                        BankLocation bankLocation = Rs2Bank.getNearestBank();
                        Rs2Walker.walkTo(bankLocation.getWorldPoint());
                    }
                    ProjectX.stopPlugin(plugin);
                    return;
                }
                currentTask = tasksToComplete.remove(0);
                DailyTasksPlugin.currentState = currentTask.getName();
            }

            System.out.println("Current task: " + currentTask.getName());
            if (!currentTask.handlesOwnTravel() && Rs2Player.distanceTo(currentTask.getLocation()) > 5) {
                DailyTasksPlugin.currentState = "Walking to: " + currentTask.getName();
                Rs2Walker.walkTo(currentTask.getLocation(), 5);
            }

            DailyTasksPlugin.currentState = "Executing: " + currentTask.getName();
            currentTask.execute();
            currentTask = null;

        }, 0, 1000, TimeUnit.MILLISECONDS);

        return true;
    }

    private void initializeTasks() {
        clientThread.runOnClientThreadOptional(() -> {
            for (DailyTask task : DailyTask.values()) {
                if (task.isEnabled(config) && task.isAvailable()) {
                    tasksToComplete.add(task);
                }
            }
            return true;
        });
    }


    @Override
    public void shutdown() {
        initialized = false;
        currentTask = null;
        tasksToComplete.clear();
        super.shutdown();
    }
}