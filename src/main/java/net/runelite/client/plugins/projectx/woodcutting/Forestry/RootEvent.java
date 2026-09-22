package net.runelite.client.plugins.projectx.woodcutting.Forestry;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.combat.Rs2Combat;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;
import net.runelite.client.plugins.projectx.woodcutting.AutoWoodcuttingPlugin;
import net.runelite.client.plugins.projectx.woodcutting.enums.ForestryEvents;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;
@Slf4j
public class RootEvent implements BlockingEvent {

    private final AutoWoodcuttingPlugin plugin;
    public RootEvent(AutoWoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean validate() {
        try{
            if (plugin == null || !ProjectX.isPluginEnabled(plugin)) return false;
            if (ProjectX.getClient() == null || !ProjectX.isLoggedIn()) return false;
            var root = plugin.rs2TileObjectCache.query().where(x -> x.getId() == ObjectID.GATHERING_EVENT_RISING_ROOTS).nearest();
            var specialRoot = plugin.rs2TileObjectCache.query().where(x -> x.getId() == ObjectID.GATHERING_EVENT_RISING_ROOTS_SPECIAL).nearest();

            // Is the hasAction Check needed?
            // If special root is present
            if (specialRoot != null)
                return Rs2GameObject.hasAction(specialRoot.getObjectComposition(), "Chop down");
            // If regular root is present
            if (root != null)
                return (Rs2GameObject.hasAction(root.getObjectComposition(), "Chop down"));

            return false; // No roots found
        } catch (Exception e) {
            log.error("RootEvent: Exception in validate method", e);
            return false;
        }

    }

    @Override
    public boolean execute() {
        ProjectX.log("RootEvent: Executing Root event");
        plugin.currentForestryEvent = ForestryEvents.TREE_ROOT;
        Rs2Walker.setTarget(null); // stop walking, stop moving to bank for example
        while (this.validate()) {
            var root = plugin.rs2TileObjectCache.query().where(x -> x.getId() == ObjectID.GATHERING_EVENT_RISING_ROOTS).nearest();
            var specialRoot = plugin.rs2TileObjectCache.query().where(x -> x.getId() == ObjectID.GATHERING_EVENT_RISING_ROOTS_SPECIAL).nearest();

            // Use special attack if available
            if ( Rs2Equipment.isWearing(ItemID.DRAGON_AXE) || Rs2Equipment.isWearing(ItemID.DRAGON_AXE_2H) || Rs2Equipment.isWearing(ItemID.CRYSTAL_AXE) ||
                    Rs2Equipment.isWearing(ItemID.CRYSTAL_AXE_2H) || Rs2Equipment.isWearing(ItemID.INFERNAL_AXE) ||
                    Rs2Equipment.isWearing(ItemID.TRAILBLAZER_AXE))
                Rs2Combat.setSpecState(true, 1000);

            // If special root is present
            if (specialRoot != null) {
                // Interact with the special root
                ProjectX.log("RootEvent: Interacting with special root at " + specialRoot.getWorldLocation());
                specialRoot.click("Chop down");
                waitForChoppingToFinish();
            }
            // If regular root is present
            else if (root != null) {
                // Interact with the regular root
                ProjectX.log("RootEvent: Interacting with regular root at " + root.getWorldLocation());
                root.click("Chop down");
                waitForChoppingToFinish();
            }
        }
        plugin.incrementForestryEventCompleted();
        return true;
    }

    private void waitForChoppingToFinish() {
        if (sleepUntil(Rs2Player::isAnimating, 5000)) {
            sleepUntil(() -> !Rs2Player.isAnimating() || !this.validate(), 40000);
        }
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.NORMAL;
    }
}
