package net.runelite.client.plugins.projectx.aiofighter.combat;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.aiofighter.AIOFighterConfig;
import net.runelite.client.plugins.projectx.aiofighter.AIOFighterPlugin;
import net.runelite.client.plugins.projectx.aiofighter.enums.State;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;

import java.util.concurrent.TimeUnit;

public class SafeSpot extends Script {

    public WorldPoint currentSafeSpot = null;
    private boolean messageShown = false;

public boolean run(AIOFighterConfig config) {
    mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
        try {
            if (AIOFighterPlugin.getState().equals(State.BANKING) || AIOFighterPlugin.getState().equals(State.WALKING)) return;
            if (!ProjectX.isLoggedIn() || !super.run() || !config.toggleSafeSpot() || Rs2Player.isMoving()) return;

            currentSafeSpot = config.safeSpot();
            if (isDefaultSafeSpot(currentSafeSpot)) {
                if(!messageShown){
                    ProjectX.showMessage("Please set a safespot location");
                    messageShown = true;
                }
                return;
            }

			messageShown = false;

			if (!isPlayerAtSafeSpot(currentSafeSpot)) {
				Rs2Walker.walkFastCanvas(currentSafeSpot);
				ProjectX.pauseAllScripts.compareAndSet(false, true);
				sleepUntil(() -> isPlayerAtSafeSpot(currentSafeSpot));
				ProjectX.pauseAllScripts.compareAndSet(true, false);
			}


        } catch (Exception ex) {
            ProjectX.logStackTrace(this.getClass().getSimpleName(), ex);
        }
    }, 0, 600, TimeUnit.MILLISECONDS);
    return true;
}

private boolean isDefaultSafeSpot(WorldPoint safeSpot) {
    return safeSpot.getX() == 0 && safeSpot.getY() == 0;
}

private boolean isPlayerAtSafeSpot(WorldPoint safeSpot) {
    return safeSpot.equals(Rs2Player.getWorldLocation());
}

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
