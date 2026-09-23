package net.runelite.client.plugins.projectx.bankseller;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.projectx.ProjectX;

import javax.inject.Inject;
import java.awt.AWTException;
import net.runelite.client.plugins.projectx.PluginConstants;

@PluginDescriptor(
        name = PluginConstants.KSP + "Bank Seller",
        description = "Withdraws tradeable bank items and sells them on the Grand Exchange",
        tags = {"bank", "ge", "seller"},
		authors = {"KSP"},
		version = BankSellerPlugin.version,
		minClientVersion = "1.9.8",
		iconUrl = "https://ieasyscript.github.io/xclient-hub/BankSellerPlugin/assets/bank.png",
        cardUrl = "https://ieasyscript.github.io/xclient-hub/BankSellerPlugin/assets/card.png",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
	    isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class BankSellerPlugin extends Plugin {

	static final String version = "1.0.2";
    @Inject
    private BankSellerConfig config;

    @Provides
    BankSellerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BankSellerConfig.class);
    }

    @Inject
    private BankSellerScript bankSellerScript;

    @Override
    protected void startUp() throws AWTException {
        ProjectX.pauseAllScripts.compareAndSet(true, false);
        bankSellerScript.run(this);
    }

    @Override
    protected void shutDown() {
        bankSellerScript.shutdown();
    }
}
