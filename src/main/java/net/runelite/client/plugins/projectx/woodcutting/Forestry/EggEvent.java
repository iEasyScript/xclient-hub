package net.runelite.client.plugins.projectx.woodcutting.Forestry;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;
import net.runelite.client.plugins.projectx.woodcutting.AutoWoodcuttingPlugin;
import net.runelite.client.plugins.projectx.woodcutting.enums.ForestryEvents;
import net.runelite.client.plugins.projectx.woodcutting.enums.WoodcuttingTree;

import java.util.Comparator;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;
@Slf4j
public class EggEvent implements BlockingEvent {

    private final AutoWoodcuttingPlugin plugin;
    public EggEvent(AutoWoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean validate() {
        try{
            if (plugin == null || !ProjectX.isPluginEnabled(plugin)) return false;
            if (ProjectX.getClient() == null || !ProjectX.isLoggedIn()) return false;
            var forester = ProjectX.getRs2NpcCache().query()
                    .withId(NpcID.GATHERING_EVENT_PHEASANT_FORESTER)
                    .nearest();
            return forester != null;
        } catch (Exception e) {
            log.error("EggEvent: Exception in validate method", e);
            return false;
        }
    }

    @Override
    public boolean execute() {

        ProjectX.log("EggEvent: Executing Egg event");
        var forester = ProjectX.getRs2NpcCache().query()
                .withId(NpcID.GATHERING_EVENT_PHEASANT_FORESTER)
                .nearest();
        if (forester == null) {
            ProjectX.log("EggEvent: Forester not found, cannot proceed with egg event.");
            return true;
        }

        plugin.currentForestryEvent = ForestryEvents.PHEASANT;

        //if inventory is full, we cannot proceed with the event
        if (Rs2Inventory.isFull()) {
            ProjectX.log("EggEvent: Inventory is full.");
            // drop a log to make space for the egg
            WoodcuttingTree tree = plugin.getSelectedTree();
            if (tree != null && Rs2Inventory.contains(tree.getLog())) {
                ProjectX.log("EggEvent: Dropping a log of " + tree.getName() + " to make space for the egg.");
                Rs2Inventory.drop(tree.getLog());
                sleepUntil(() -> !Rs2Inventory.isFull(), 5000);
            }
        }
        Rs2Walker.setTarget(null); // stop walking, stop moving to bank for example
        while (this.validate()) {

            // If we have an egg, interact with the forester
            if (Rs2Inventory.contains("Pheasant egg")) {
                ProjectX.log("EggEvent: Interacting with the forester to give the egg.");
                forester.click("Talk-to");
                sleepUntil(Rs2Dialogue::isInDialogue, 5000);
                while (Rs2Dialogue.isInDialogue()) Rs2Dialogue.clickContinue();
                continue;
            }

            // If we don't have an egg, interact with the pheasant nest
            var nests = ProjectX.getRs2TileObjectCache().query().where(gameObject -> gameObject.getId() == ObjectID.GATHERING_EVENT_PHEASANT_NEST02).toList();
            var pheasants = ProjectX.getRs2NpcCache().query().withId(NpcID.GATHERING_EVENT_PHEASANT).toList();

            if (nests.isEmpty() || pheasants.isEmpty()) {
                ProjectX.log("EggEvent: No pheasant nests found, cannot proceed with egg event.");
                continue;
            }
            // find nest without pheasants
            var emptyNests = nests.stream()
                    .filter(nest -> pheasants.stream()
                            .noneMatch(pheasant -> pheasant.getWorldLocation() == nest.getWorldLocation()))
                    .collect(Collectors.toList());

            ProjectX.log("EggEvent: Interacting with the pheasant nest to collect an egg.");
            var closestNest = emptyNests.stream()
                    .min(Comparator.comparingInt(o -> o.getWorldLocation().distanceTo(Rs2Player.getWorldLocation())))
                    .orElse(null);

            var interact = closestNest != null && closestNest.click();
            if (!interact) {
                ProjectX.log("EggEvent: Failed to interact with the pheasant nest.");
                ProjectX.log("EggEvent: Closest nest is null? " + (closestNest == null));
            }
            Rs2Player.waitForAnimation();
        }
        ProjectX.log("EggEvent: Ending Egg event.");
        plugin.incrementForestryEventCompleted();
        return true;
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.NORMAL;
    }
}
