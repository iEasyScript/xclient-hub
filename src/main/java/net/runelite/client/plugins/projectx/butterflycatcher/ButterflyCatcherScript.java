package net.runelite.client.plugins.projectx.butterflycatcher;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * ButterflyCatcherScript
 *
 * Two modes — both run indefinitely with no banking:
 *
 *   BAREHANDED:
 *     Click catch, wait for animation/movement to resolve, repeat.
 *     Requires Hunter level >= target.getBarehandedLevelRequired().
 *
 *   BUTTERFLY_NET:
 *     Same loop, but requires a butterfly net or magic butterfly net to be
 *     equipped. Verified once on startup. Requires Hunter level >=
 *     target.getNetLevelRequired() (10 levels lower than barehanded).
 *
 * Item IDs for nets:
 *   Butterfly net       : 10010
 *   Magic butterfly net : 11259
 */
@Slf4j
public class ButterflyCatcherScript extends Script {

    private static final int NET_ITEM_ID       = 10010;
    private static final int MAGIC_NET_ITEM_ID = 11259;
    private static final String TAG  = "[ButterflyCatcher]";
    private static final int    TICK = 600;

    private ButterflyType targetButterfly;
    private CatchMode     catchMode;
    private boolean       catchCommitted = false;

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public boolean run(ButterflyCatcherConfig config) {
        this.targetButterfly = config.butterflyType();
        this.catchMode       = config.catchMode();
        this.catchCommitted  = false;

        int requiredLevel = effectiveLevel();
        ProjectX.log(TAG + " Starting — target: " + targetButterfly.getDisplayName()
                + " | mode: " + catchMode
                + " | Required Hunter level: " + requiredLevel);

        // Net equipment check (BUTTERFLY_NET mode only)
        if (catchMode == CatchMode.BUTTERFLY_NET) {
            if (!isNetEquipped()) {
                ProjectX.log(TAG + " No butterfly net equipped! "
                        + "Equip a Butterfly Net (id 10010) or Magic Butterfly Net (id 11259) "
                        + "before starting. Stopping.");
                ProjectX.showMessage("Butterfly Catcher: no butterfly net equipped. Equip one and restart.");
                return false;
            }
            ProjectX.log(TAG + " Butterfly net verified.");
        }

        // Hunter level check
        int hunterLevel = ProjectX.getClient().getRealSkillLevel(Skill.HUNTER);
        if (hunterLevel < requiredLevel) {
            ProjectX.log(TAG + " Hunter level too low ("
                    + hunterLevel + " / " + requiredLevel
                    + ") for " + targetButterfly.getDisplayName()
                    + " in " + catchMode + " mode. Stopping.");
            ProjectX.showMessage("Butterfly Catcher: Hunter level too low ("
                    + hunterLevel + " / " + requiredLevel + ").");
            return false;
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::tick, 0, TICK, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        catchCommitted = false;
    }

    // -------------------------------------------------------------------------
    // Main tick
    // -------------------------------------------------------------------------

    private void tick() {
        try {
            if (!ProjectX.isLoggedIn()) return;
            if (!super.run()) return;

            // Stop if level drops below threshold (e.g. de-boost)
            int hunterLevel = ProjectX.getClient().getRealSkillLevel(Skill.HUNTER);
            if (hunterLevel < effectiveLevel()) {
                ProjectX.log(TAG + " Hunter level too low — stopping.");
                shutdown();
                return;
            }

            tickCatching();

        } catch (Exception e) {
            log.error(TAG + " Unexpected error in tick: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Catching logic
    // -------------------------------------------------------------------------

    private void tickCatching() {
        // Don't spam-click while already committed to a catch attempt
        if (catchCommitted) {
            if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
                return;
            }
            catchCommitted = false;
        }

        if (Rs2Player.isAnimating()) return;

        // Find nearest target NPC using the singleton cache
        var target = ProjectX.getRs2NpcCache()
                .query()
                .withIds(targetButterfly.getNpcIds())
                .nearest();

        if (target == null) {
            ProjectX.log(TAG + " No " + targetButterfly.getDisplayName()
                    + " found nearby (IDs: " + Arrays.toString(targetButterfly.getNpcIds()) + ") — waiting.");
            return;
        }

        boolean clicked = target.click("Catch");
        if (!clicked) {
            log.warn(TAG + " click(Catch) failed on {} at {}",
                    targetButterfly.getDisplayName(), target.getWorldLocation());
            return;
        }

        catchCommitted = true;
        log.debug(TAG + " Clicked Catch on {} at {}",
                targetButterfly.getDisplayName(), target.getWorldLocation());
        sleepUntil(() -> Rs2Player.isAnimating() || Rs2Player.isMoving(), 1500);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int effectiveLevel() {
        return catchMode == CatchMode.BUTTERFLY_NET
                ? targetButterfly.getNetLevelRequired()
                : targetButterfly.getBarehandedLevelRequired();
    }

    private boolean isNetEquipped() {
        return Rs2Equipment.isWearing(NET_ITEM_ID) || Rs2Equipment.isWearing(MAGIC_NET_ITEM_ID);
    }
}
