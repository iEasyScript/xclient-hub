package net.runelite.client.plugins.projectx.sailing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.sailing.features.salvaging.SalvagingScript;
import net.runelite.client.plugins.projectx.sailing.features.trials.TrialsScript;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SailingScript extends Script {

    private final SailingConfig config;
    private final SalvagingScript salvagingFeature;
    private final TrialsScript trialsFeature;

	@Inject
	public SailingScript(SailingConfig config, SalvagingScript salvagingFeature, TrialsScript trialsFeature) {
		this.config = config;
		this.salvagingFeature = salvagingFeature;
		this.trialsFeature = trialsFeature;
	}

    public boolean run() {
        ProjectX.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn()) return;
                if (!super.run()) return;

                if (config.salvaging()) {
                    salvagingFeature.run(config);
                }

                if (config.trials()) {
                    trialsFeature.run(config);
                }


            } catch (Exception ex) {
                log.trace("Exception in main loop: ", ex);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }
    
    @Override
    public void shutdown() {
        super.shutdown();
    }
}
