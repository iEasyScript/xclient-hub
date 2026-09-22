package net.runelite.client.plugins.projectx.bradleycombat.actions;

import net.runelite.client.plugins.projectx.bradleycombat.interfaces.CombatAction;
import net.runelite.client.plugins.projectx.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.projectx.util.prayer.Rs2PrayerEnum;

public class DefensivePrayerAction implements CombatAction {
    private final Rs2PrayerEnum prayer;

    public DefensivePrayerAction(Rs2PrayerEnum prayer) {
        this.prayer = prayer;
    }

    @Override
    public void execute() {
        Rs2Prayer.toggle(prayer, true);
    }
}