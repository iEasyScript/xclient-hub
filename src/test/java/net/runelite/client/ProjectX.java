package net.runelite.client;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.runelite.client.plugins.kourendlibrary.KourendLibraryPlugin;
import net.runelite.client.plugins.projectx.GiantSeaweedFarmer.GiantSeaweedFarmerPlugin;
import net.runelite.client.plugins.projectx.agentserver.AgentServerPlugin;
import net.runelite.client.plugins.projectx.arceuuslibrary.ArceuusLibraryPlugin;
import net.runelite.client.plugins.projectx.astralrc.AstralRunesPlugin;
import net.runelite.client.plugins.projectx.birdhouseruns.FornBirdhouseRunsPlugin;
import net.runelite.client.plugins.projectx.autofishing.AutoFishingPlugin;
import net.runelite.client.plugins.projectx.crafting.jewelry.JewelryPlugin;
import net.runelite.client.plugins.projectx.example.ExamplePlugin;
import net.runelite.client.plugins.projectx.karambwans.GabulhasKarambwansPlugin;
import net.runelite.client.plugins.projectx.kraken.KrakenPlugin;
import net.runelite.client.plugins.projectx.leftclickcast.LeftClickCastPlugin;
import net.runelite.client.plugins.projectx.pitfallhunter.PitfallHunterPlugin;
import net.runelite.client.plugins.projectx.sailing.MSailingPlugin;
import net.runelite.client.plugins.projectx.thieving.ThievingPlugin;
import net.runelite.client.plugins.projectx.motherloadmine.MotherloadMinePlugin;
import net.runelite.client.plugins.projectx.woodcutting.AutoWoodcuttingPlugin;
import net.runelite.client.plugins.woodcutting.WoodcuttingPlugin;

public class ProjectX
{

	private static final Class<?>[] debugPlugins = {
		AgentServerPlugin.class,
		FornBirdhouseRunsPlugin.class,
		GiantSeaweedFarmerPlugin.class,
		PitfallHunterPlugin.class,
		GabulhasKarambwansPlugin.class,
		MotherloadMinePlugin.class,
		KourendLibraryPlugin.class,
		ArceuusLibraryPlugin.class
	};

    public static void main(String[] args) throws Exception
    {
		List<Class<?>> _debugPlugins = Arrays.stream(debugPlugins).collect(Collectors.toList());
        RuneLiteDebug.pluginsToDebug.addAll(_debugPlugins);
        RuneLiteDebug.main(args);
    }
}
