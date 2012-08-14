package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.util.PcAction;

public class premium extends gmHandler
{
	private static final String[] commands =
	{
		"premium_menu",
		"premium_add1",
		"premium_add2",
		"premium_add3"
	};
	
	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;
		
		String command = params[0];

		if (command.equals("premium_menu"))
		{
			methods.showSubMenuPage(admin, "premium_menu.htm");
			return;
		}
		else if (command.equals("premium_add1"))
		{
			
			try
			{
				if (params.length > 1)
					PcAction.addPremiumServices(admin, params[1],30);
				else
					admin.sendMessage("Используйте: //premium_add1 [acc]");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				admin.sendMessage("Используйте: //premium_add1 [acc]");
			}
			methods.showSubMenuPage(admin, "premium_menu.htm");
			return;
		}
		else if (command.equals("premium_add2"))
		{
			try
			{
				if (params.length > 1)
					PcAction.addPremiumServices(admin, params[1],60);
				else
					admin.sendMessage("Используйте: //premium_add1 [acc]");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				admin.sendMessage("Используйте: //premium_add1 [acc]");
			}
			methods.showSubMenuPage(admin, "premium_menu.htm");
			return;
		}
		else if (command.equals("premium_add3"))
		{
			try
			{
				if (params.length > 1)
					PcAction.addPremiumServices(admin, params[1],90);
				else
					admin.sendMessage("Используйте: //premium_add1 [acc]");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				admin.sendMessage("Используйте: //premium_add1 [acc]");
			}
			methods.showSubMenuPage(admin, "premium_menu.htm");
			return;
		}
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}