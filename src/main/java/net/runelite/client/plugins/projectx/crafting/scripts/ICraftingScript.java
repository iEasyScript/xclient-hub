package net.runelite.client.plugins.projectx.crafting.scripts;

import java.util.Map;

public interface ICraftingScript {
    String getName();
    String getVersion();
    String getState();
    Map<String, String> getCustomProperties();
}
