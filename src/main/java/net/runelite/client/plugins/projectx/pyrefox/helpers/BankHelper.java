package net.runelite.client.plugins.projectx.pyrefox.helpers;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.bank.enums.BankLocation;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

public class BankHelper
{
	public static boolean walkToAndOpenBank()
	{
		if (Rs2Bank.isOpen())
			return true;

		// Wait until our bank is opened.
		boolean isNearBank = Rs2Bank.walkToBank();
		Rs2Player.waitForWalking();
		if (!isNearBank || !Rs2Bank.isNearBank(6))
		{
			ProjectX.status = "Walking to bank.";
			return false;
		}

		if (!Rs2Bank.isOpen()) {
			ProjectX.status = "Opening bank.";
			Rs2Bank.openBank();
			Rs2Player.waitForWalking();
			return false;
		}
		return true;
	}

	public static boolean walkToAndOpenBank(BankLocation bankLocation)
	{
		if (Rs2Bank.isOpen())
			return true;

		// Wait until our bank is opened.
		boolean isNearBank = Rs2Bank.walkToBankAndUseBank(bankLocation);
		Rs2Player.waitForWalking();

		if (!isNearBank || !Rs2Bank.isNearBank(bankLocation, 6))
		{
			ProjectX.status = "Walking to bank.";
			return false;
		}

		if (!Rs2Bank.isOpen()) {
			ProjectX.status = "Opening bank.";
			Rs2Bank.openBank();
			Rs2Player.waitForWalking();
			return false;
		}
		return true;
	}
}
