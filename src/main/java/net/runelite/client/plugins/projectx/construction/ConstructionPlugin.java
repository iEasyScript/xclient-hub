package net.runelite.client.plugins.projectx.construction;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.projectx.construction.ConstructionConfig;
import net.runelite.client.plugins.projectx.construction.ConstructionOverlay;
import net.runelite.client.plugins.projectx.construction.ConstructionScript;
import net.runelite.client.plugins.projectx.construction.enums.ConstructionState;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Geoff + "Construction 2",
        description = "Geoff's ProjectX construction plugin with added new bits.",
        tags = {"skilling", "projectx", "construction"},
        version = ConstructionPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "",
        iconUrl = "",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class ConstructionPlugin extends Plugin {
    public static final String version = "1.3.2";

    @Inject
    private net.runelite.client.plugins.projectx.construction.ConstructionConfig config;

    @Provides
    net.runelite.client.plugins.projectx.construction.ConstructionConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ConstructionConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ConstructionOverlay ConstructionOverlay;

    private final net.runelite.client.plugins.projectx.construction.ConstructionScript ConstructionScript = new ConstructionScript();

    @Override
    protected void startUp() throws AWTException {
        ProjectX.pauseAllScripts.compareAndSet(true, false);
        if (overlayManager != null) {
            overlayManager.add(ConstructionOverlay);
        }
        ConstructionScript.run(config);
    }

    @Override
    protected void shutDown() {
        ConstructionScript.shutdown();
        overlayManager.remove(ConstructionOverlay);
    }

    public ConstructionState getState() {
        return ConstructionScript.getState();
    }
}
