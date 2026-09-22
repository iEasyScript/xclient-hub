package net.runelite.client.plugins.projectx.bradleycombat.interfaces;

import net.runelite.client.config.Keybind;

public abstract interface Relay {

    default void action(Keybind key, CombatAction action) {
    }

}