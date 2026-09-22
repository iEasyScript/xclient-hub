package net.runelite.client.plugins.projectx.playermonitor;

import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.security.Login;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PlayerMonitorScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(PlayerMonitorScript.class);
    public static String version = "1.0.0";
    @Inject
    private PlayerMonitorConfig config;
    @Inject
    private PlayerMonitorPlugin plugin;
    @Inject
    private OverlayManager overlayManager;
    private static FlashOverlay flashOverlay;
    boolean playAlarm;
    boolean newPlayer;
    WorldPoint otherPlayerLocation;
    int otherPlayerWorld;
    private boolean logoutInitiated = false;
    Color offColor = new Color(0, 0, 0, 0);
    private boolean naturalmouse = false;

    public boolean run(PlayerMonitorConfig config, OverlayManager overlayManager) {
        this.config = config;
        this.overlayManager = overlayManager;
        otherPlayerWorld = 0;
        newPlayer = false;
        playAlarm = false;
        flashOverlay = new FlashOverlay();
        flashOverlay.setFlashColor(offColor);
        overlayManager.add(flashOverlay);
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!ProjectX.isLoggedIn()) return;
            try {
                if (!config.liteMode()) {
                    if (ProjectX.getClient().getGameCycle() % 20 >= 10) {
                        if (!playAlarm) {
                            if (plugin.isOverlayOn()) {
                                if (config.playAlarmSound()) {
                                    ProjectX.getClientThread().invokeLater(() -> ProjectX.getClient().playSoundEffect(config.alarmSoundID().getId(), 127));
                                }
                                playAlarm = true;
                                if (config.useFlash()) {
                                    flashOverlay.setFlashColor(config.flashColor());
                                }
                                if ((config.useFlash() || config.playAlarmSound()) && config.useEmergency() && !newPlayer && (config.onlyWilderness() && ProjectX.getVarbitValue(Varbits.IN_WILDERNESS) == 1) || !config.onlyWilderness()) {
                                    newPlayer = true;
                                    otherPlayerLocation = Rs2Player.getWorldLocation();
                                    otherPlayerWorld = Rs2Player.getWorld();
                                    ProjectX.getClientThread().runOnSeperateThread(() -> {
                                        switch (config.emergencyAction()) {
                                            case LOGOUT:
                                                logoutPlayer();
                                                break;
                                            case HOP_WORLDS:
                                                ClientUI.getClient().setEnabled(false);
                                                if (this.isRunning()) {
                                                    sleep(61, 93);
                                                }
                                                if (this.isRunning()) {
                                                    ProjectX.getClient().openWorldHopper();
                                                }
                                                if (this.isRunning()) {
                                                    sleepUntil(() -> Rs2Widget.hasWidget("Current world - " + Rs2Player.getWorld()));
                                                }
                                                if (this.isRunning()) {
                                                    sleep(61, 93);
                                                }
                                                if (this.isRunning()) {
                                                    ProjectX.hopToWorld(Login.getRandomWorld(Rs2Player.isMember()));
                                                }
                                                if (this.isRunning()) {
                                                    sleep(61, 93);
                                                }
                                                ClientUI.getClient().setEnabled(true);
                                                break;
                                            case USE_ITEM:
                                                ClientUI.getClient().setEnabled(false);
                                                if (this.isRunning()) {
                                                    sleep(61, 93);
                                                }
                                                if (Pattern.compile("[0-9]+").matcher(config.emergencyItem()).matches()) {
                                                    Rs2Inventory.interact(Integer.parseInt(config.emergencyItem()), config.emergencyItemMenu());
                                                } else {
                                                    Rs2Inventory.interact(config.emergencyItem(), config.emergencyItemMenu());
                                                }
                                                if (this.isRunning()) {
                                                    sleep(61, 93);
                                                }
                                                ClientUI.getClient().setEnabled(true);
                                                break;
                                        }
                                        return true;
                                    });
                                }
                            }
                            if (PlayerMonitorPlugin.mouseAlarm && config.mouseAlarm()) {
                                ProjectX.getClientThread().invokeLater(() -> ProjectX.getClient().playSoundEffect(config.mouseAlarmSound().getId(), 127));
                            }
                        }
                    } else {
                        if (playAlarm) {
                            playAlarm = false;
                            flashOverlay.setFlashColor(offColor);
                        }
                    }
                    if (newPlayer && !plugin.isOverlayOn() && (Rs2Player.getWorldLocation().distanceTo(otherPlayerLocation) > 32 || otherPlayerWorld != Rs2Player.getWorld())) {
                        newPlayer = false;
                    }
                }

                if (config.liteMode()) {
                    if (plugin.isPlayerDetected() && !logoutInitiated) {
                        log.info("Player detected - initiating logout");
                        ProjectX.log("PlayerMonitorLite: Player detected - logging out");
                        logoutInitiated = true;
                        // Perform logout
                        if (Rs2AntibanSettings.naturalMouse) {Rs2AntibanSettings.naturalMouse = false; naturalmouse = true;}
                        ProjectX.getClientThread().runOnSeperateThread(() -> {
                            logoutPlayer();
                            return true;
                        });
                        if (naturalmouse && !Rs2AntibanSettings.naturalMouse) {Rs2AntibanSettings.naturalMouse = true; naturalmouse = false;}
                    } else if (!plugin.isPlayerDetected() && logoutInitiated) {
                        logoutInitiated = false;
                    }
                }

            } catch (Exception ex) {
                ProjectX.log(ex.getMessage());
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
        return true;
    }

    private void logoutPlayer() {
        ProjectX.getClientThread().invokeLater(() -> {
            ClientUI.getClient().setEnabled(false);
            Rs2Player.logout();
            ClientUI.getClient().setEnabled(true);
        });
    }

    @Override
    public void shutdown() {
        if (mainScheduledFuture != null) {
            mainScheduledFuture.cancel(true);
        }
        if (flashOverlay != null) {
            flashOverlay.setFlashColor(offColor);
            overlayManager.remove(flashOverlay);
        }
        super.shutdown();
    }
}