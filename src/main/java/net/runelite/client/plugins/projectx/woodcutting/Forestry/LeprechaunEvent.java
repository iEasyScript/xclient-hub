package net.runelite.client.plugins.projectx.woodcutting.Forestry;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.Global;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;
import net.runelite.client.plugins.projectx.woodcutting.AutoWoodcuttingPlugin;
import net.runelite.client.plugins.projectx.woodcutting.enums.ForestryEvents;
import org.slf4j.event.Level;

import static net.runelite.client.plugins.projectx.util.Global.sleepGaussian;
@Slf4j
public class LeprechaunEvent implements BlockingEvent {

    private final AutoWoodcuttingPlugin plugin;

    public LeprechaunEvent(AutoWoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean validate() {
        try{
            if (plugin == null || !ProjectX.isPluginEnabled(plugin)) return false;
            if (ProjectX.getClient() == null || !ProjectX.isLoggedIn()) return false;
            var leprechaun = ProjectX.getRs2NpcCache().query()
                    .withId(NpcID.GATHERING_EVENT_WOODCUTTING_LEPRECHAUN)
                    .nearest();
            return leprechaun != null;
        } catch (Exception e) {
            log.error("LeprechaunEvent: Exception in validate method", e);
            return false;
        }
    }

    @Override
    public boolean execute() {
        ProjectX.log("LeprechaunEvent: Executing Leprechaun event");
        plugin.currentForestryEvent = ForestryEvents.RAINBOW;
        Rs2Walker.setTarget(null); // stop walking, stop moving to bank for example
        while (this.validate()) {
            log.info("LeprechaunEvent: Leprechaun event still valid, continuing execution get opbject");
            var endOfRainbow = ProjectX.getRs2TileObjectCache().query().withId(ObjectID.GATHERING_EVENT_WOODCUTTING_LEPRECHAUN_RAINBOW).nearest();
            if (endOfRainbow == null) {
                log.warn("LeprechaunEvent: End of the rainbow not found, retrying...");
                sleepGaussian(900, 300);
                continue; // If the end of the rainbow is not found, we cannot proceed with the event
            }
            // Move to the end of the rainbow
            var location = endOfRainbow.getWorldLocation();
            if (!Rs2Player.getWorldLocation().equals(location)) {
                ProjectX.log("LeprechaunEvent: Walking to the end of the rainbow at " + location, Level.INFO);
                Rs2Walker.walkFastCanvas(location);
                Global.sleepUntil(() -> Rs2Player.getWorldLocation().equals(location), 5000);
            }
        }
        plugin.incrementForestryEventCompleted();
        return true;

        //TODO: Implement interaction with the leprechaun for banking
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.NORMAL;
    }
}
