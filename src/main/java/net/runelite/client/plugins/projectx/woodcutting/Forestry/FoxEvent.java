package net.runelite.client.plugins.projectx.woodcutting.Forestry;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;
import net.runelite.client.plugins.projectx.woodcutting.AutoWoodcuttingPlugin;
import net.runelite.client.plugins.projectx.woodcutting.enums.ForestryEvents;
import org.slf4j.event.Level;

@Slf4j
public class FoxEvent implements BlockingEvent {

    private final AutoWoodcuttingPlugin plugin;
    public FoxEvent(AutoWoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean validate() {
        try{
            if (plugin == null || !ProjectX.isPluginEnabled(plugin)) return false;
            if (ProjectX.getClient() == null || !ProjectX.isLoggedIn()) return false;
            var outDoorFox = ProjectX.getRs2NpcCache().query().withId(NpcID.GATHERING_EVENT_POACHERS_FOX_OUTDOORS).nearest();
            var indoorFox = ProjectX.getRs2NpcCache().query().withId(NpcID.GATHERING_EVENT_POACHERS_FOX_INDOORS).nearest();
            return outDoorFox != null || indoorFox != null;
        } catch (Exception e) {
            log.error("FoxEvent: Exception in validate method", e);
            return false;
        }
    }

    @Override
    public boolean execute() {
        ProjectX.log("FoxEvent: Executing Fox event");
        plugin.currentForestryEvent = ForestryEvents.FOX_TRAP;
        Rs2Walker.setTarget(null); // stop walking
        
        // ensure inventory space for potential fox whistle (1/30 chance)
        if (!plugin.ensureInventorySpace(1)) {
            ProjectX.log("FoxEvent: Cannot make inventory space for potential rewards, ending event.");
            return true;
        }
        
        while (this.validate()) {
            var trap = ProjectX.getRs2NpcCache().query().withId(NpcID.GATHERING_EVENT_POACHERS_TRAP).nearest();
            if (trap == null) {
                continue; // If the trap is not found, we cannot proceed with the event
            }
            ProjectX.log("FoxEvent: Interacting with the trap to disarm it.", Level.INFO);
            // Interact with the trap if it exists
            trap.click("Disarm");
            Rs2Player.waitForAnimation(1000);
        }
        ProjectX.log("FoxEvent: Finished executing the Fox event.", Level.INFO);
        plugin.incrementForestryEventCompleted();
        return true;
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.NORMAL;
    }
}
