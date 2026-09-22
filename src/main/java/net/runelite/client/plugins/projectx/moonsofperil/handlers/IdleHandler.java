package net.runelite.client.plugins.projectx.moonsofperil.handlers;

import net.runelite.client.plugins.projectx.moonsofperil.enums.State;
import net.runelite.client.plugins.projectx.moonsofperil.MoonsOfPerilConfig;
import net.runelite.client.plugins.projectx.util.prayer.Rs2Prayer;

public class IdleHandler implements BaseHandler {

    private final net.runelite.client.plugins.projectx.moonsofperil.handlers.BossHandler boss;

    public IdleHandler(MoonsOfPerilConfig cfg) {
        this.boss = new BossHandler(cfg);
    }

    @Override
    public boolean validate() {
        Rs2Prayer.disableAllPrayers();
        boss.eatIfNeeded();
        boss.drinkIfNeeded();
        return false;
    }

    @Override
    public State execute() {
        return null;
    }
}
