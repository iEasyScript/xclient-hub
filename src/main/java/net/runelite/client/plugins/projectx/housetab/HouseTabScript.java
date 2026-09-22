package net.runelite.client.plugins.projectx.housetab;

import net.runelite.api.Point;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.projectx.housetab.enums.HOUSETABS_CONFIG;

import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.inventory.Rs2RunePouch;
import net.runelite.client.plugins.projectx.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.projectx.util.magic.Runes;
import net.runelite.client.plugins.projectx.util.math.Rs2Random;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class HouseTabScript extends Script {
    private final int RIMMINGTON_PORTAL_OBJECT = 15478;
    private final int HOUSE_PORTAL_OBJECT = 4525;

    private final int HOUSE_ADVERTISEMENT_OBJECT = 29091;

    private final int HOUSE_ADVERTISEMENT_NAME_PARENT_INTERFACE = 3407881;

    private final Map<Integer, Integer> lecternToHouseTabButton = Map.of(
            ObjectID.POH_LECTERN_6, 26411031,
            ObjectID.POH_LECTERN_8, 26411033
    );

    private final HOUSETABS_CONFIG houseTabConfig;
    private final String[] playerHouses;

    private final ScheduledExecutorService scheduledExecutorService;

    private int lecternTabletWidgetId = 26411033;

    private boolean hasSoftClay() {
        return Rs2Inventory.hasItem(1761);
    }

    private boolean hasSoftClayNoted() {
        return Rs2Inventory.hasItem(1762);
    }

    private boolean hasLawRune() {
        if (Rs2Inventory.hasRunePouch()) {Rs2RunePouch.fullUpdate();}
        return Rs2Inventory.hasItem(ItemID.LAWRUNE) || (Rs2Inventory.hasRunePouch() && Rs2RunePouch.contains(Runes.LAW));
    }

    public HouseTabScript(HOUSETABS_CONFIG houseTabConfig, String[] playerHouses) {
        this.houseTabConfig = houseTabConfig;
        this.playerHouses = playerHouses;
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    private void lookForHouseAdvertisementObject() {
        Widget houseAdvertisementPanel = ProjectX.getClient().getWidget(HOUSE_ADVERTISEMENT_NAME_PARENT_INTERFACE);
        if (!hasSoftClay() || houseAdvertisementPanel != null || ProjectX.getRs2TileObjectCache().query().withId(HOUSE_PORTAL_OBJECT).nearest() != null)
            return;


        boolean success = ProjectX.getRs2TileObjectCache().query()
                .interact(HOUSE_ADVERTISEMENT_OBJECT, "View");


        if (success) {
            sleepUntilOnClientThread(() -> ProjectX.getClient().getWidget(HOUSE_ADVERTISEMENT_NAME_PARENT_INTERFACE) != null);
        }
    }

    private void lookForPlayerHouse() {
        Widget houseAdvertisementNameWidget = ProjectX.getClient().getWidget(HOUSE_ADVERTISEMENT_NAME_PARENT_INTERFACE);
        if (houseAdvertisementNameWidget == null || houseAdvertisementNameWidget.getChildren() == null) return;
        if (!hasSoftClay())
            return;
        if (ProjectX.getRs2TileObjectCache().query().withId(HOUSE_PORTAL_OBJECT).nearest() != null)
            return;

        int enterHouseButtonHeight = 21;
        int houseIndexToJoin = 0;

        for (int i = 0; i < houseAdvertisementNameWidget.getChildren().length; i++) {
            Widget child = houseAdvertisementNameWidget.getChild(i);
            if (child == null) continue;
            if (Arrays.stream(this.playerHouses).anyMatch(x -> child.getText().equalsIgnoreCase(x))) {
                houseIndexToJoin = i;
                break;
            }
        }

        Widget mainWindow = ProjectX.getClient().getWidget(3407879);
        if (mainWindow == null) return;
        int HOUSE_ADVERTISEMENT_ENTER_HOUSE_PARENT_INTERFACE = 3407891;
        Widget houseAdvertisementEnterHouseWidget = ProjectX.getClient().getWidget(HOUSE_ADVERTISEMENT_ENTER_HOUSE_PARENT_INTERFACE);
        if (houseAdvertisementEnterHouseWidget == null) return;
        Widget enterHouseButton = houseAdvertisementEnterHouseWidget.getChild(houseIndexToJoin);
        int buttonRelativeY = houseAdvertisementEnterHouseWidget.getChild(houseIndexToJoin).getRelativeY() + enterHouseButtonHeight;
        if (buttonRelativeY > (mainWindow.getScrollY() + mainWindow.getHeight())) {
            keepExecuteUntil(() -> {
                int x = (int) mainWindow.getBounds().getCenterX() + Rs2Random.between(-50, 50);
                int y = (int) mainWindow.getBounds().getCenterY() + Rs2Random.between(-50, 50);
                ProjectX.getMouse().scrollDown(new Point(x, y));
            }, () -> buttonRelativeY <= (mainWindow.getScrollY() + mainWindow.getHeight()), 500);
        } else {
            ProjectX.getMouse()
                    .click(enterHouseButton.getCanvasLocation());
            sleepUntilOnClientThread(() -> ProjectX.getRs2TileObjectCache().query().withId(HOUSE_PORTAL_OBJECT).nearest() != null);
            sleep(2000, 3000);
        }
    }

    private Integer getHouseLectern() {
        Rs2TileObjectModel lectern = null;
        for (Integer id : lecternToHouseTabButton.keySet()) {
            lectern = ProjectX.getRs2TileObjectCache().query().withId(id).nearest();
            if (lectern != null) break;
        }
        if (lectern != null) {
            lecternTabletWidgetId = lecternToHouseTabButton.get(lectern.getId());
            return lectern.getId();
        }

        return null;
    }

    public void lookForLectern() {
        if (getHouseLectern() == null) { ProjectX.log("Can't find lectern"); shutdown(); return;} //can't find lectern
        if (!hasSoftClay() || ProjectX.getRs2TileObjectCache().query().withId(HOUSE_ADVERTISEMENT_OBJECT).nearest() != null || ProjectX.isGainingExp)
            return;

        Widget houseTabInterface = ProjectX.getClient().getWidget(lecternTabletWidgetId);
        if (houseTabInterface != null || ProjectX.getRs2TileObjectCache().query().withId(HOUSE_PORTAL_OBJECT).nearest() == null) return;

        boolean success = ProjectX.getRs2TileObjectCache().query().withIds(lecternToHouseTabButton.keySet().stream().mapToInt(Integer::intValue).toArray()).interact("Study");
        if (success) {
            sleepUntilOnClientThread(() -> ProjectX.getClient().getWidget(lecternTabletWidgetId) != null);
        }
    }

    public void createHouseTablet() {
        Widget houseTabInterface = ProjectX.getClient().getWidget(lecternTabletWidgetId);
        if (houseTabInterface == null) return;
        if (!hasSoftClay() || ProjectX.getRs2TileObjectCache().query().withId(HOUSE_PORTAL_OBJECT).nearest() == null)
            return;

        while (ProjectX.getClient().getWidget(lecternTabletWidgetId) != null) {
            if (mainScheduledFuture.isCancelled()) break;
            ProjectX.getMouse()
                    .click(houseTabInterface.getCanvasLocation());
            sleep(1000, 2000);
            Rs2Widget.clickWidget(26411022); // create button on spell tablet widget
            sleep(1000, 2000);
        }

        sleepUntilOnClientThread(() -> !hasSoftClay()
                || ProjectX.getClient().getWidget(lecternTabletWidgetId) != null
                || !ProjectX.isGainingExp, 55000);
    }

    public void leaveHouse() {
        if (hasSoftClay() || ProjectX.getRs2TileObjectCache().query().withId(HOUSE_PORTAL_OBJECT).nearest() == null)
            return;

        boolean success = ProjectX.getRs2TileObjectCache().query().interact(HOUSE_PORTAL_OBJECT, "Enter");
        if (success)
            sleepUntil(() -> ProjectX.getRs2TileObjectCache().query().withId(HOUSE_PORTAL_OBJECT).nearest() == null);
    }

    public void unnoteClay() {
        if (hasSoftClay() || ProjectX.getRs2TileObjectCache().query().withId(HOUSE_ADVERTISEMENT_OBJECT).nearest() == null)
            return;
        if (ProjectX.getClient().getWidget(14352385) == null) {
            while (true) {
                ProjectX.getClientThread().invoke(() -> {
                    Rs2Inventory.use("Soft clay");
                });
                sleep(300, 380);
                var phials = ProjectX.getRs2NpcCache().query().withName("Phials").nearestOnClientThread();
                if (phials != null && phials.click("Use")) break;
            }
        }

        sleep(2500, 5000);
        if (ProjectX.getClient().getWidget(14352385) != null) {
            Rs2Keyboard.keyPress('3');
            sleep(300, 380);
        }
    }

    public boolean run(HouseTabConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn()) return;
                if (!super.run()) return;
                if (!hasSoftClayNoted() || !hasLawRune()) {
                    shutdown();
                    return;
                }
                if (ProjectX.isGainingExp) return;

                Rs2Player.toggleRunEnergy(true);
                if (ProjectX.getClient().getEnergy() < 3000 && !Rs2Widget.hasWidget("Teleport to House") && ProjectX.getRs2TileObjectCache().query().withIds(ObjectID.XMAS20_POH_POOL_REGENERATION, ObjectID.POH_POOL_REJUVENATION).nearest() != null) {
                    ProjectX.getRs2TileObjectCache().query().withIds(ObjectID.XMAS20_POH_POOL_REGENERATION, ObjectID.POH_POOL_REJUVENATION).interact("drink");
                    return;
                }

                boolean isInHouse = getHouseLectern() != null;
                if (isInHouse) {
                    lookForLectern();
                    createHouseTablet();
                    leaveHouse();
                } else {
                    unnoteClay();
                    if (config.ownHouse()) {
                        if (ProjectX.getRs2TileObjectCache().query().interact(ObjectID.POH_RIMMINGTON_PORTAL, "Home")) {
                            sleep(800, 1200);
                        }
                        return;
                    }
                    if (ProjectX.getRs2TileObjectCache().query().interact(ObjectID.POH_RIMMINGTON_PORTAL, "Friend's house")) {
                        sleepUntil(() -> Rs2Widget.hasWidget("Enter name"));
                        if (Rs2Widget.hasWidget(config.housePlayerName())) {
                            Rs2Widget.clickWidget(config.housePlayerName());
                            sleep(800, 1200);
                        } else {
                            if (Rs2Widget.hasWidget("Enter name")) {
                                Rs2Keyboard.typeString(config.housePlayerName());
                                Rs2Keyboard.enter();
                                sleep(1000, 1800);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    public ScheduledFuture<?> keepExecuteUntil(Runnable callback, BooleanSupplier awaitedCondition, int time) {
        scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (awaitedCondition.getAsBoolean()) {
                scheduledFuture.cancel(true);
                scheduledFuture = null;
                return;
            }
            callback.run();
        }, 0, time, TimeUnit.MILLISECONDS);
        return scheduledFuture;
    }
}