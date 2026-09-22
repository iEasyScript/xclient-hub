package net.runelite.client.plugins.projectx.virewatch;

import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.util.combat.Rs2Combat;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;

import java.util.concurrent.TimeUnit;

public class PVirewatchScript extends Script {

    public boolean run(PVirewatchKillerConfig config, PVirewatchKillerPlugin plugin) {
        ProjectX.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn()) return;
                if (!super.run()) return;
                Rs2Combat.enableAutoRetialiate();

                if(plugin.fightArea.contains(ProjectX.getClientThread().invoke(() -> ProjectX.getClient().getLocalPlayer().getWorldLocation()))) {
                    ProjectX.status = "Figthing";
                }

                if(ProjectX.getClientThread().invoke(() -> ProjectX.getClient().getLocalPlayer().getWorldLocation()) != plugin.startingLocation) {
                    if(plugin.ticksOutOfArea > config.tickToReturn() || plugin.countedTicks > config.tickToReturnCombat()) {
                        Rs2Walker.walkTo(plugin.startingLocation, 0);
                    }
                }

                Rs2Player.eatAt(config.hitpoints());

                if(ProjectX.getClient().getBoostedSkillLevel(Skill.PRAYER) <= config.prayAt()) {
                    plugin.rechargingPrayer = true;
                    var statue = ProjectX.getRs2TileObjectCache().query().withId(39234).nearest();
                    if(statue != null) {
                        Rs2Walker.walkTo(statue.getWorldLocation(), 1);
                        sleepUntil(statue::isReachable);
                        if(statue.isReachable()) {
                            ProjectX.status = "RECHARGING PRAYER";
                            statue.click();
                            sleep(100);
                            plugin.rechargingPrayer = false;
                            if(Rs2Player.isInteracting()) {
                                sleepUntil(() -> ProjectX.getClient().getBoostedSkillLevel(Skill.PRAYER) > config.prayAt());
                                ProjectX.status = "WALKING TO STARTING POINT";
                                Rs2Walker.walkTo(plugin.startingLocation, 0);

                            }
                        }
                    }

                }

            } catch (Exception ex) {
                ProjectX.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
