package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.Shutdown.ShutdownModeType;
import ru.catssoftware.gameserver.gmaccess.gmController;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author m095
 * @version 1.0
 */

public class admin extends gmHandler
{
	private static final String[] commands =
	{
		"admin",
		"players",
		"effects",
		"gamemenu",
		"gmmenu",
		"bar",
		"listall"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;
		final String command = params[0];
		if (command.equals("bar")) {
			_log.info("======================================================================");
		}
		else if (command.equals("admin"))
			sendHtml(admin, "main");
		else if (command.equals("players"))
			sendHtml(admin, "players");
		else if (command.equals("effects"))
			sendHtml(admin, "effects");
		else if (command.equals("gamemenu"))
			sendHtml(admin, "game");
		else if (command.equals("gmmenu"))
			sendHtml(admin, "submenus/gmmenu");
		else if (command.equals("listall"))
			gmController.getInstance().showCommands(admin);
		
	}
	
	/**
	 * Отпрвка страницы
	 * @param admin
	 * @param patch
	 */
	private void sendHtml(L2PcInstance admin, String patch)
	{
		String name = (patch + ".htm");
		NpcHtmlMessage html = new NpcHtmlMessage(admin.getObjectId());
		html.setFile("data/html/admin/menus/" + name);
		admin.sendPacket(html);
	}
	
	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}