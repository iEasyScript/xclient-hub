package net.runelite.client.plugins.projectx.bradleycombat.actions;

import net.runelite.api.Player;
import net.runelite.client.plugins.projectx.bradleycombat.BradleyCombatPlugin;
import net.runelite.client.plugins.projectx.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.player.Rs2PlayerModel;

public class AttackAction implements CombatAction {
    private final boolean shouldAttack;

    public AttackAction(boolean shouldAttack) {
        this.shouldAttack = shouldAttack;
    }

    @Override
    public void execute() {
        if (!shouldAttack) return;
        if (BradleyCombatPlugin.validTarget()) Rs2Player.attack((Rs2PlayerModel) BradleyCombatPlugin.getTarget());
    }
}