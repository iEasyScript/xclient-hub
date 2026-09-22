package net.runelite.client.plugins.projectx.bradleycombat.actions;

import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.projectx.util.magic.Rs2Magic;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

public class VengeanceAction implements CombatAction {
    private final boolean useVengeance;

    public VengeanceAction(boolean useVengeance) {
        this.useVengeance = useVengeance;
    }

    @Override
    public void execute() {
        if (!useVengeance) return;
        if (ProjectX.getClient().getBoostedSkillLevel(Skill.MAGIC) <= 93) return;
        Rs2Magic.cast(MagicAction.VENGEANCE);
    }
}