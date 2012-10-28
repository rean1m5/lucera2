package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;

public class olympiad extends gmHandler
{
	private static final String[] commands =
	{
		"saveolymp",
		"endolympiad"
	};
	
	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		String command = params[0];
		
		if (command.equals("saveolymp"))
		{
			try
			{
				Olympiad.getInstance().saveOlympiadStatus();
				admin.sendMessage("Олимпиада успешно сохранена");
			}
			catch (Exception e)
			{
				e.printStackTrace();
				admin.sendMessage("Произошла ошибка при сохранении олимпиады");
			}
			return;
		}
		else if (command.equals("endolympiad"))
		{
			try
			{
				Olympiad.getInstance().manualSelectHeroes();
				admin.sendMessage("Герои успешно обновлены");
			}
			catch (Exception e)
			{
				e.printStackTrace();
				admin.sendMessage("Произошла ошибка при обновлении героев");
			}
			return;
		}
	}
	
	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}