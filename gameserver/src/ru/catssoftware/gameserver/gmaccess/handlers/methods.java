package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.BoatManager;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author m095
 * @version 1.0
 */
public class methods extends gmHandler
{
	private static final String[] commands =
	{ 
		"help"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;
		
		if (params[0].equals("help"))
		{
			try
			{
				String val = params[1];
				if (val.equals("tele_boats"))
					showTeleBoatMenuPage(admin, val);
				else
					showHelpPage(admin, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
		}
	}

	public static void showHelpPage(L2PcInstance targetChar, String filename)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/" + filename);
		targetChar.sendPacket(adminReply);
	}

	public static void showTeleMenuPage(L2PcInstance targetChar, String filename)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/tele/" + filename);
		targetChar.sendPacket(adminReply);
	}
	
	public static void showSubMenuPage(L2PcInstance targetChar, String filename)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/" + filename);
		targetChar.sendPacket(adminReply);
	}
	
	public static void showMenuPage(L2PcInstance targetChar, String filename)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/" + filename);
		targetChar.sendPacket(adminReply);
	}
	
	public static void showTeleBoatMenuPage(L2PcInstance targetChar, String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(5);
		String str;
		str = "<html><title>Boats Teleport Menu</title><body>";
		str += "<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>";
		str += "<br><table width=260><tr><td width=180><center><font color=\"aadd77\">Boats</font></center></td><td width=40><button value=\"Back\" action=\"bypass -h admin_help tele/teleports.htm\" width=55 height=21 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr></table><br><br><img src=\"L2UI.SquareWhite\" width=260 height=1><img src=\"L2UI.SquareBlank\" width=260 height=4><br><table width=256>";
		for (L2BoatInstance boat : BoatManager.getInstance().getBoats().values())
			str += "<tr><td><a action=\"bypass -h admin_move_to boat "+boat.getObjectId()+"\">Лодка "+boat.getId()+"</a></td></tr>";					
		str += "</table><br><img src=\"L2UI.SquareWhite\" width=260 height=1><img src=\"L2UI.SquareBlank\" width=260 height=4><br></center></body></html>";		
		html.setHtml(str);
		targetChar.sendPacket(html);
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}
