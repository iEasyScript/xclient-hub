package net.runelite.client.plugins.projectx.woodcutting.Forestry;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;
import net.runelite.client.plugins.projectx.woodcutting.AutoWoodcuttingPlugin;
import net.runelite.client.plugins.projectx.woodcutting.AutoWoodcuttingScript;
import net.runelite.client.plugins.projectx.woodcutting.enums.ForestryEvents;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.runelite.api.gameval.ObjectID.*;
@Slf4j
public class StrugglingSaplingEvent implements BlockingEvent {
    private final AutoWoodcuttingPlugin plugin;
    private final List<Integer> ingredientIds = List.of(
        GATHERING_EVENT_SAPLING_INGREDIENT_1,
        GATHERING_EVENT_SAPLING_INGREDIENT_2,
        GATHERING_EVENT_SAPLING_INGREDIENT_3,
        GATHERING_EVENT_SAPLING_INGREDIENT_4A,
        GATHERING_EVENT_SAPLING_INGREDIENT_4B,
        GATHERING_EVENT_SAPLING_INGREDIENT_4C,
        GATHERING_EVENT_SAPLING_INGREDIENT_5
    );

    public StrugglingSaplingEvent(AutoWoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean validate() {
        try{
            if (plugin == null || !ProjectX.isPluginEnabled(plugin)) return false;
            if (ProjectX.getClient() == null || !ProjectX.isLoggedIn()) return false;        
            var strugglingSaplings = ProjectX.getRs2TileObjectCache().query()
                    .withName("Struggling sapling")
                    .toListOnClientThread();
            if (strugglingSaplings.isEmpty()) return false;
            return strugglingSaplings.stream().anyMatch(obj ->
                    Rs2GameObject.hasAction(obj.getObjectComposition(), "Add-mulch") &&
                            obj.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= AutoWoodcuttingScript.FORESTRY_DISTANCE
            );
        } catch (Exception e) {
            log.error("StrugglingSaplingEvent: Exception in validate method", e);
            return false;
        }
    }

    @Override
    public boolean execute() {
        try {
            ProjectX.log("StrugglingSaplingEvent: Executing Struggling Sapling event");
            plugin.currentForestryEvent = ForestryEvents.STRUGGLING_SAPLING;
            // Find the struggling sapling
            var sapling = ProjectX.getRs2TileObjectCache().query()
                    .withName("Struggling sapling")
                    .toListOnClientThread()
                    .stream()
                    .filter(obj ->
                            Rs2GameObject.hasAction(obj.getObjectComposition(), "Add-mulch") &&
                                    obj.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= AutoWoodcuttingScript.FORESTRY_DISTANCE
                    )
                    .findFirst()
                    .orElse(null);

            var ingredients = ProjectX.getRs2TileObjectCache().query()
                    .where(gameObject -> ingredientIds.contains(gameObject.getId()))
                    .toList()
                    .stream()
                    .filter(obj -> Rs2GameObject.hasAction(obj.getObjectComposition(), "Collect"))
                    .collect(Collectors.toList());

            if (ingredients.isEmpty()) {
                ProjectX.log("StrugglingSaplingEvent: No leaf ingredients available to collect. Ending event.");
                return true;
            }

            Set<Integer> triedFirstIngredients = new HashSet<>();
            Set<Integer> triedSecondIngredients = new HashSet<>();
            Set<Integer> triedThirdIngredients = new HashSet<>();
            Rs2Walker.setTarget(null); // stop walking, stop moving to bank for example
            
            // ensure inventory space for mulch items and reward (up to 25 items)
            if (!plugin.ensureInventorySpace(5)) {
                ProjectX.log("StrugglingSaplingEvent: Cannot make enough inventory space, ending event.");
                return true;
            }
            
            while (this.validate()) {
                // If we have mulch stage 3 in inventory, add them to the sapling
                if (Rs2Inventory.contains(ItemID.GATHERING_EVENT_SAPLING_MULCH_STAGE3)) {
                    ProjectX.log("StrugglingSaplingEvent: Adding mulch to the struggling sapling.");
                    sapling.click("Add-mulch");
                    Rs2Player.waitForAnimation();
                    continue;
                }

                //if we have mulch in inventory, check if we know the correct ingredient or pick a random one
                // Determine current stage
                int stage;
                if (Rs2Inventory.contains(ItemID.GATHERING_EVENT_SAPLING_MULCH_STAGE2)) {
                    stage = 2;
                } else if (Rs2Inventory.contains(ItemID.GATHERING_EVENT_SAPLING_MULCH_STAGE1)) {
                    stage = 1;
                } else {
                    stage = 0;
                }
                var correctIngredient = plugin.saplingOrder[stage];

                if (correctIngredient != null) {
                    // Look for matching ingredient in our available ingredients
                    for (Rs2TileObjectModel ingredient : ingredients) {
                        if (ingredient.getId() == correctIngredient.getId()) {
                            ProjectX.log("StrugglingSaplingEvent: Collecting known correct ingredient: " + ingredient.getWorldLocation());
                            ingredient.click("Collect");
                            Rs2Player.waitForAnimation();
                        }
                    }
                    continue;
                }

                // If we don't know the correct ingredient, try to collect a random one
                Set<Integer> triedIngredients;
                if (stage == 2) triedIngredients = triedThirdIngredients;
                else if (stage == 1) triedIngredients = triedSecondIngredients;
                else triedIngredients = triedFirstIngredients;

                var availableIngredients = ingredients.stream()
                        .filter(ingredient -> !triedIngredients.contains(ingredient.getId()))
                        .collect(Collectors.toList());
                if (availableIngredients.isEmpty()) {
                    ProjectX.log("StrugglingSaplingEvent: All ingredients have been tried for stage " + stage + ".");
                    break;
                }

                ProjectX.log("StrugglingSaplingEvent: No known correct ingredient, collecting the closest untried one.");
                var closestIngredient = availableIngredients.get(0);
                int closestDist = closestIngredient.getWorldLocation().distanceTo(Rs2Player.getWorldLocation());
                for (var ingredient : availableIngredients) {
                    int dist = ingredient.getWorldLocation().distanceTo(Rs2Player.getWorldLocation());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestIngredient = ingredient;
                    }
                }

                ProjectX.log("StrugglingSaplingEvent: Collecting ingredient: " + closestIngredient.getWorldLocation());
                closestIngredient.click("Collect");
                triedIngredients.add(closestIngredient.getId());
                Rs2Player.waitForAnimation();
            }

            ProjectX.log("StrugglingSaplingEvent: Finished processing struggling sapling.");
            plugin.saplingOrder[0] = null; // Reset the sapling order after processing
            plugin.saplingOrder[1] = null;
            plugin.saplingOrder[2] = null;
            plugin.incrementForestryEventCompleted();
            return true;
        }
        catch (Exception e) {
            ProjectX.log("StrugglingSaplingEvent: Error during execution: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
            return this.validate();
        }
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.NORMAL;
    }

}
