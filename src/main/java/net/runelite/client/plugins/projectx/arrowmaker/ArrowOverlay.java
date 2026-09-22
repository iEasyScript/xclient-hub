package net.runelite.client.plugins.projectx.arrowmaker;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.arrowmaker.ArrowPlugin;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;



public class ArrowOverlay extends OverlayPanel {

    @Inject
    ArrowOverlay(ArrowPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(275, 800));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Nate's Arrow Maker")
                    .color(Color.green)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(ProjectX.status)
                    .build());


        } catch(Exception ex) {
            ProjectX.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }
}
