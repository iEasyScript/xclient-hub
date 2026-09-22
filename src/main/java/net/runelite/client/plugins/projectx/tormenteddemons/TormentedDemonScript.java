package net.runelite.client.plugins.projectx.tormenteddemons;

import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.util.Rs2InventorySetup;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.equipment.JewelleryLocationEnum;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.projectx.util.grounditem.LootingParameters;
import net.runelite.client.plugins.projectx.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.projectx.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.projectx.util.reflection.Rs2Reflection;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TormentedDemonScript extends Script {

    private boolean isRunning = false;
    public static int killCount = 0;
    private Rs2PrayerEnum currentDefensivePrayer = null;
    private Rs2PrayerEnum currentOffensivePrayer = null;
    private HeadIcon currentOverheadIcon = null;
    public Rs2NpcModel currentTarget;
    private boolean lootAttempted = false;
    private String lastChatMessage = "";
    private boolean isRestocking = false;

    private static final WorldPoint SAFE_LOCATION = new WorldPoint(3150, 3634, 0);

    private enum State {BANKING, TRAVEL_TO_TORMENTED, FIGHTING}

    public static State BOT_STATUS = State.BANKING;

    private enum TravelStep {LOCATION_ONE, CLIMB_FIRST_STAIRS, CLIMB_SECOND_STAIRS, CLIMB_THROUGH, LOCATION_THREE}

    private TravelStep travelStep = TravelStep.LOCATION_ONE;

    private enum BankingStep {DRINK, BANK, LOAD_INVENTORY}

    private BankingStep bankingStep = BankingStep.DRINK;

    public boolean run(TormentedDemonConfig config) {
        if (config.mode() == TormentedDemonConfig.MODE.FULL_AUTO) {
            BOT_STATUS = State.BANKING;
        } else if (config.mode() == TormentedDemonConfig.MODE.COMBAT_ONLY) {
            BOT_STATUS = State.FIGHTING;
        }

        bankingStep = BankingStep.DRINK;
        travelStep = TravelStep.LOCATION_ONE;
        ProjectX.enableAutoRunOn = false;
        isRunning = true;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn() || !super.run()) return;

                switch (BOT_STATUS) {
                    case BANKING:
                        handleBanking(config);
                        break;
                    case TRAVEL_TO_TORMENTED:
                        handleTravel(config);
                        break;
                    case FIGHTING:
                        handleFighting(config);
                        break;
                }
            } catch (Exception ex) {
                logOnceToChat("Error in main loop: " + ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }


    private void handleTravel(TormentedDemonConfig config) {
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
        }
        WorldPoint targetLocationOne = new WorldPoint(4062, 4558, 0);
        WorldPoint targetFinalLocation = new WorldPoint(4073, 4432, 0);
        WorldPoint playerLocation = ProjectX.getClientThread().invoke(() -> ProjectX.getClient().getLocalPlayer().getWorldLocation());

        switch (travelStep) {
            case LOCATION_ONE:
                ProjectX.status = "Teleporting to Guthixian temple...";
                if (Rs2Inventory.interact("Guthixian temple teleport", "Teleport")) {
                    Rs2Player.waitForAnimation();
                    sleepUntil(() -> !Rs2Player.isAnimating());
                    sleepUntil(() -> playerLocation.distanceTo(targetLocationOne) <= 5);
                    travelStep = TravelStep.CLIMB_FIRST_STAIRS;
                }
                break;

            case CLIMB_FIRST_STAIRS:
                ProjectX.status = "Climbing first stairs...";
                if (ProjectX.getRs2TileObjectCache().query().interact(53623, "Climb-up")) {
                    Rs2Player.waitForAnimation();
                    sleepUntil(() -> !Rs2Player.isAnimating());
                    travelStep = TravelStep.CLIMB_SECOND_STAIRS;
                }
                break;

            case CLIMB_SECOND_STAIRS:
                ProjectX.status = "Climbing second stairs...";
                if (ProjectX.getRs2TileObjectCache().query().interact(53624, "Climb-up")) {
                    Rs2Player.waitForAnimation();
                    sleepUntil(() -> !Rs2Player.isAnimating());
                    travelStep = TravelStep.CLIMB_THROUGH;
                }
                break;

            case CLIMB_THROUGH:
                ProjectX.status = "Climbing through the path...";
                if (ProjectX.getRs2TileObjectCache().query().interact(54082, "Climb-through")) {
                    Rs2Player.waitForAnimation();
                    sleepUntil(() -> !Rs2Player.isAnimating());
                    travelStep = TravelStep.LOCATION_THREE;
                }
                break;

            case LOCATION_THREE:
                ProjectX.status = "Approaching Tormented Demon location...";
                if (Rs2Walker.walkTo(targetFinalLocation, 2)) {
                    sleepUntil(() -> targetFinalLocation.equals(ProjectX.getClientThread().invoke(() -> ProjectX.getClient().getLocalPlayer().getWorldLocation())), 5000);
                    travelStep = TravelStep.LOCATION_ONE;
                    BOT_STATUS = State.FIGHTING;
                }
                break;
        }
    }

    private void handleBanking(TormentedDemonConfig config) {
        final int FEROX_POOL_ID = 39651;

        switch (bankingStep) {
            case DRINK:
                ProjectX.status = "Drinking at Ferox Enclave pool...";
                int currentHealth = ProjectX.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
                int maxHealth = ProjectX.getClient().getRealSkillLevel(Skill.HITPOINTS);
                int currentPrayer = ProjectX.getClient().getBoostedSkillLevel(Skill.PRAYER);
                int maxPrayer = ProjectX.getClient().getRealSkillLevel(Skill.PRAYER);

                if (currentHealth < maxHealth || currentPrayer < maxPrayer) {
                    if (ProjectX.getRs2TileObjectCache().query().interact(FEROX_POOL_ID, "Drink")) {
                        Rs2Player.waitForAnimation();
                        sleepUntil(() ->
                                ProjectX.getClient().getBoostedSkillLevel(Skill.HITPOINTS) == maxHealth &&
                                        ProjectX.getClient().getBoostedSkillLevel(Skill.PRAYER) == maxPrayer
                        );
                        bankingStep = BankingStep.BANK;
                    }
                } else {
                    bankingStep = BankingStep.BANK;
                }
                break;

            case BANK:
                if (isRestocking) {
                    Rs2InventorySetup inventorySetup = new Rs2InventorySetup("tormented", mainScheduledFuture);
                    inventorySetup.wearEquipment();
                }

                ProjectX.status = "Opening bank...";
                Rs2Bank.openBank();
                sleepUntil(Rs2Bank::isOpen);
                Rs2Bank.depositAll();
                bankingStep = BankingStep.LOAD_INVENTORY;
                break;

            case LOAD_INVENTORY:
                ProjectX.status = "Loading inventory and equipment setup...";
                Rs2InventorySetup inventorySetup = new Rs2InventorySetup("tormented", mainScheduledFuture);
                boolean equipmentLoaded = inventorySetup.loadEquipment();
                boolean inventoryLoaded = inventorySetup.loadInventory();

                if (equipmentLoaded && inventoryLoaded) {
                    Rs2Bank.closeBank();
                    bankingStep = BankingStep.DRINK;
                    BOT_STATUS = State.TRAVEL_TO_TORMENTED;
                    isRestocking = true;
                } else {
                    shutdown();
                }
                break;
        }
    }

    private void handleFighting(TormentedDemonConfig config) {
        if (currentTarget == null || currentTarget.isDead()) {
            disableAllPrayers();

            if (!lootAttempted) {
                ProjectX.pauseAllScripts.compareAndSet(false, true);
                sleep(5000);
                attemptLooting(config);
                lootAttempted = true;
                ProjectX.pauseAllScripts.compareAndSet(true, false);
                killCount++;
            }

            currentTarget = findNewTarget(config);
            if (currentTarget.getInteracting() != ProjectX.getClient().getLocalPlayer()) {
                currentTarget = findNewTarget(config);
            }
            if (currentTarget != null) {
                currentOverheadIcon = currentTarget.getHeadIcon();
                if (currentOverheadIcon == null) {
                    logOnceToChat("Failed to retrieve HeadIcon for target.");
                    return;
                }
                switchGear(config, currentOverheadIcon);
                lootAttempted = false;
            } else {
                logOnceToChat("No target found for attack.");
                return;
            }
        }

        evaluateAndConsumePotions(config);

        if (config.mode() == TormentedDemonConfig.MODE.FULL_AUTO && shouldRetreat(config)) {
            currentTarget = null;
            currentOverheadIcon = null;
            ProjectX.pauseAllScripts.set(true);
            teleportToFeroxEnclave();
            sleepUntil(() -> SAFE_LOCATION.equals(ProjectX.getClientThread().invoke(() -> ProjectX.getClient().getLocalPlayer().getWorldLocation())), 5000);
            ProjectX.pauseAllScripts.set(false);
            BOT_STATUS = State.BANKING;
            return;
        }

        if (currentTarget != null && !currentTarget.isDead()) {

            Rs2Player.eatAt(config.minEatPercent());
            Rs2Player.drinkPrayerPotionAt(config.minPrayerPercent());

            var interactingActor = Rs2Player.getInteracting();
            int interactingIndex = (interactingActor instanceof NPC) ? ((NPC) interactingActor).getIndex() : -1;

            if (currentTarget == null) return;

            if (interactingActor == null || interactingIndex != currentTarget.getIndex()) {
                boolean attackSuccessful = currentTarget.click("attack");

                if (attackSuccessful) {
                    Rs2Player.waitForAnimation();
                    sleepUntil(() -> Rs2Player.getInteracting() instanceof NPC && ((NPC) Rs2Player.getInteracting()).getIndex() == currentTarget.getIndex(), 3000);
                } else {
                    logOnceToChat("Attack failed for target: " + (currentTarget != null ? currentTarget.getName() : "null"));
                    currentTarget = null;
                    return;
                }
            }
        }

        HeadIcon newOverheadIcon = currentTarget.getHeadIcon();
        if (newOverheadIcon != currentOverheadIcon) {
            currentOverheadIcon = newOverheadIcon;
            if (!Rs2Inventory.isOpen()) {
                Rs2Inventory.open();
                sleepUntil(Rs2Inventory::isOpen, 1000);
            }
            switchGear(config, currentOverheadIcon);
            sleep(100);
        }

        if (currentTarget == null) return;

        if (config.enableOffensivePrayer()) {
            activateOffensivePrayer(config);
        }
    }


    private void switchDefensivePrayer(Rs2PrayerEnum newDefensivePrayer) {
        if (currentDefensivePrayer != null) {
            Rs2Prayer.toggle(currentDefensivePrayer, false);
        }
        Rs2Prayer.toggle(newDefensivePrayer, true);
        currentDefensivePrayer = newDefensivePrayer;
    }

    private void activateOffensivePrayer(TormentedDemonConfig config) {
        Rs2PrayerEnum newOffensivePrayer = null;
        if (config.useMagicStyle() && isGearEquipped(parseGear(config.magicGear()))) {
            newOffensivePrayer = Rs2Prayer.getBestMagePrayer();
        } else if (config.useMeleeStyle() && isGearEquipped(parseGear(config.meleeGear()))) {
            newOffensivePrayer = Rs2Prayer.getBestMeleePrayer();
        } else if (config.useRangeStyle() && isGearEquipped(parseGear(config.rangeGear()))) {
            newOffensivePrayer = Rs2Prayer.getBestRangePrayer();
        }
        if (newOffensivePrayer != null && newOffensivePrayer != currentOffensivePrayer) {
            logOnceToChat("Changing offensive prayer to " + newOffensivePrayer);
            switchOffensivePrayer(newOffensivePrayer);
            sleep(100);
        }
    }

    private void switchOffensivePrayer(Rs2PrayerEnum newOffensivePrayer) {
        if (currentOffensivePrayer != null) {
            Rs2Prayer.toggle(currentOffensivePrayer, false);
        }
        Rs2Prayer.toggle(newOffensivePrayer, true);
        currentOffensivePrayer = newOffensivePrayer;
    }

    private Rs2NpcModel findNewTarget(TormentedDemonConfig config) {
        return ProjectX.getRs2NpcCache().query()
                .withName("Tormented Demon")
                .where(npc -> !npc.isDead())
                .where(npc -> npc.getInteracting() == null || npc.getInteracting() == ProjectX.getClient().getLocalPlayer())
                .where(npc -> {
                    HeadIcon demonHeadIcon = npc.getHeadIcon();
                    if (demonHeadIcon != null) {
                        switchGear(config, demonHeadIcon);
                        return true;
                    }
                    logOnceToChat("Null HeadIcon for NPC " + npc.getName());
                    return false;
                })
                .firstOnClientThread();
    }

    private void switchGear(TormentedDemonConfig config, HeadIcon combatNpcHeadIcon) {
        if (!config.autoGearSwitch()) {
            return;
        }

        List<String> gearToEquip = new ArrayList<>();
        boolean useRange = config.useRangeStyle();
        boolean useMagic = config.useMagicStyle();
        boolean useMelee = config.useMeleeStyle();

        switch (combatNpcHeadIcon) {
            case RANGED:
                if (useMelee && useMagic) {
                    gearToEquip = Math.random() < 0.5 ? parseGear(config.meleeGear()) : parseGear(config.magicGear());
                } else if (useMelee) {
                    gearToEquip = parseGear(config.meleeGear());
                } else if (useMagic) {
                    gearToEquip = parseGear(config.magicGear());
                }
                break;

            case MAGIC:
                if (useRange && useMelee) {
                    gearToEquip = Math.random() < 0.5 ? parseGear(config.rangeGear()) : parseGear(config.meleeGear());
                } else if (useRange) {
                    gearToEquip = parseGear(config.rangeGear());
                } else if (useMelee) {
                    gearToEquip = parseGear(config.meleeGear());
                }
                break;

            case MELEE:
                if (useRange && useMagic) {
                    gearToEquip = Math.random() < 0.5 ? parseGear(config.rangeGear()) : parseGear(config.magicGear());
                } else if (useRange) {
                    gearToEquip = parseGear(config.rangeGear());
                } else if (useMagic) {
                    gearToEquip = parseGear(config.magicGear());
                }
                break;
        }

        if (!isGearEquipped(gearToEquip)) {
            logOnceToChat("Changing gear to " + gearToEquip);
            equipGear(gearToEquip);
        }
    }

    private List<String> parseGear(String gearString) {
        return Arrays.asList(gearString.split(","));
    }

    private boolean isGearEquipped(List<String> gear) {
        return gear.stream().allMatch(Rs2Equipment::isWearing);
    }

    private void equipGear(List<String> gear) {
        for (String item : gear) {
            Rs2Inventory.wield(item);
            sleep(50);
        }
    }

    private boolean shouldRetreat(TormentedDemonConfig config) {
        int currentHealth = ProjectX.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        int currentPrayer = ProjectX.getClient().getBoostedSkillLevel(Skill.PRAYER);
        boolean noFood = Rs2Inventory.getInventoryFood().isEmpty();
        boolean noPrayerPotions = Rs2Inventory.items()
                .noneMatch(item -> item != null && item.getName() != null && item.getName().toLowerCase().contains("prayer potion"));

        return (noFood || currentHealth <= config.healthThreshold()) || (noPrayerPotions && currentPrayer < 10);
    }

    public void disableAllPrayers() {
        Rs2Prayer.disableAllPrayers();
        currentDefensivePrayer = null;
        currentOffensivePrayer = null;
    }

    private void attemptLooting(TormentedDemonConfig config) {
        ProjectX.log("Checking loot..");
        List<String> lootItems = parseLootItems(config.lootItems());

        LootingParameters nameParams = new LootingParameters(10, 1, 1, 1, false, true, lootItems.toArray(new String[0]));
        Rs2GroundItem.lootItemsBasedOnNames(nameParams);

        if (config.scatterAshes()) {
            lootAndScatterInfernalAshes();
        }
    }

    private void lootAndScatterInfernalAshes() {
        String ashesName = "Infernal ashes";

        if (!Rs2Inventory.isFull() && Rs2GroundItem.lootItemsBasedOnNames(new LootingParameters(10, 1, 1, 0, false, true, ashesName))) {
            sleepUntil(() -> Rs2Inventory.contains(ashesName), 2000);

            if (Rs2Inventory.contains(ashesName)) {
                Rs2Inventory.interact(ashesName, "Scatter");
                sleep(600); // Wait briefly for scattering action
            }
        }
    }

    private List<String> parseLootItems(String lootFilter) {
        return Arrays.asList(lootFilter.toLowerCase().split(","));
    }

    private void teleportToFeroxEnclave() {
        int[] duelingRingIds = {
                ItemID.RING_OF_DUELING_1,
                ItemID.RING_OF_DUELING_2,
                ItemID.RING_OF_DUELING_3,
                ItemID.RING_OF_DUELING_4,
                ItemID.RING_OF_DUELING_5,
                ItemID.RING_OF_DUELING_6,
                ItemID.RING_OF_DUELING_7,
                ItemID.RING_OF_DUELING_8
        };
        for (int ringId : duelingRingIds) {
            if (Rs2Inventory.hasItem(ringId)) {
                Rs2Prayer.disableAllPrayers();
                Rs2Inventory.interact(ringId, "Wear");
                sleep(800);

                Rs2Equipment.interact(JewelleryLocationEnum.FEROX_ENCLAVE.getTooltip(), JewelleryLocationEnum.FEROX_ENCLAVE.getDestination());
                logOnceToChat("Teleporting to Ferox Enclave using Ring of Dueling");
                return;
            }
        }
        logOnceToChat("No Ring of Dueling found in inventory for teleporting to Ferox Enclave.");
    }

    private void evaluateAndConsumePotions(TormentedDemonConfig config) {
        int threshold = config.boostedStatsThreshold();

        if (!isCombatPotionActive(config.combatPotionType(), threshold)) {
            consumeCombatPotion(config.combatPotionType());
        }

        if (!isRangingPotionActive(config.rangingPotionType(), threshold)) {
            consumeRangingPotion(config.rangingPotionType());
        }
    }

    private boolean isCombatPotionActive(TormentedDemonConfig.CombatPotionType combatPotionType, int threshold) {
        switch (combatPotionType) {
            case SUPER_COMBAT:
                return Rs2Player.hasAttackActive(threshold) && Rs2Player.hasStrengthActive(threshold);
            case DIVINE_SUPER_COMBAT:
                return Rs2Player.hasDivineCombatActive();
            default:
                return true;
        }
    }

    private boolean isRangingPotionActive(TormentedDemonConfig.RangingPotionType rangingPotionType, int threshold) {
        switch (rangingPotionType) {
            case RANGING:
                return Rs2Player.hasRangingPotionActive(threshold);
            case DIVINE_RANGING:
                return Rs2Player.hasDivineRangedActive();
            case BASTION:
                return Rs2Player.hasDivineBastionActive();
            default:
                return true;
        }
    }

    private void consumeCombatPotion(TormentedDemonConfig.CombatPotionType combatPotionType) {
        String potion = null;
        switch (combatPotionType) {
            case SUPER_COMBAT:
                potion = "super combat";
                break;
            case DIVINE_SUPER_COMBAT:
                potion = "divine super combat";
                break;
            default:
                return;
        }
        consumePotion(potion);
    }

    private void consumeRangingPotion(TormentedDemonConfig.RangingPotionType rangingPotionType) {
        String potion = null;
        switch (rangingPotionType) {
            case RANGING:
                potion = "ranging potion";
                break;
            case DIVINE_RANGING:
                potion = "divine ranging potion";
                break;
            case BASTION:
                potion = "bastion potion";
                break;
            default:
                return;
        }
        consumePotion(potion);
    }

    private void consumePotion(String keyword) {
        Rs2Inventory.getPotions().stream()
                .filter(potion -> potion.getName().toLowerCase().contains(keyword))
                .findFirst()
                .ifPresent(potion -> {
                    Rs2Inventory.interact(potion, "Drink");
                    logOnceToChat("Drinking potion: " + potion.getName());
                });
    }

    void logOnceToChat(String message) {
        if (!message.equals(lastChatMessage)) {
            ProjectX.log(message);
            lastChatMessage = message;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        isRunning = false;
        disableAllPrayers();
        BOT_STATUS = State.BANKING;
        travelStep = TravelStep.LOCATION_ONE;
        bankingStep = BankingStep.DRINK;
        currentTarget = null;
        killCount = 0;
        lootAttempted = false;  // Reset here
        currentDefensivePrayer = null;
        currentOffensivePrayer = null;
        currentOverheadIcon = null;
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }
        logOnceToChat("Shutting down Tormented Demon script");
    }
}