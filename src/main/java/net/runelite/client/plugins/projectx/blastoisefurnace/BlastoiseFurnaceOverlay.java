package net.runelite.client.plugins.projectx.blastoisefurnace;


import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class BlastoiseFurnaceOverlay extends OverlayPanel {
    @Inject
    BlastoiseFurnaceOverlay(BlastoiseFurnacePlugin plugin) {
        super(plugin);
        this.setPosition(OverlayPosition.TOP_LEFT);
        this.setNaughty();
    }

    public Dimension render(Graphics2D graphics) {
        try {
            this.panelComponent.setPreferredSize(new Dimension(200, 300));
            this.panelComponent.getChildren().add(TitleComponent.builder().text("Blastoise" + BlastoiseFurnacePlugin.version).color(Color.GREEN).build());
            this.panelComponent.getChildren().add(LineComponent.builder().build());
            this.panelComponent.getChildren().add(LineComponent.builder().left(ProjectX.status).build());
        } catch (Exception var3) {
            Exception ex = var3;
            ProjectX.logStackTrace(this.getClass().getSimpleName(), ex);
        }

        return super.render(graphics);
    }
}