package ru.catssoftware.gameserver.handler.voicedcommandhandlers;

import javolution.text.TextBuilder;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.banmanager.BanManager;
import ru.catssoftware.gameserver.handler.VoicedCommandHandler;
import ru.catssoftware.gameserver.handler.IVoicedCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.info.Version;

/**
 * Автор: L2CatsSoftware
 * Хандлер для вывода справки по голосовым командам.
 **/

public class Help implements IVoicedCommandHandler
{
	public Help() {
		
	}
	private static final String[] VOICED_COMMANDS	=
	{
		"help"
	};

	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (activeChar == null)
			return false;

		if (command.startsWith("devinfo")) {
			String html = "<html><title>Server info</title><body>";
			html+="<br><center>";
			html+="Version "+Version.Version+"<br1>";
			html+="Server IP "+Config.GAMESERVER_HOSTNAME+"<br>";
			html+=	"</center></body></html>";
			NpcHtmlMessage msg = new NpcHtmlMessage(0);
			msg.setHtml(html);
			activeChar.sendPacket(msg);
		}
		else if (command.startsWith("help"))
		{

			NpcHtmlMessage help = new NpcHtmlMessage(5);
			TextBuilder html = new TextBuilder("<html><body><br>");
			html.append("<center><font color=\"LEVEL\">Список доступных голосовых команд и их описание.</font><table>");
			for(String comm : VoicedCommandHandler.getInstance().getVoicedCommandHandlers().keySet())
			try {
				if(comm.equals("devinfo")) 
					continue;
				IVoicedCommandHandler handler = VoicedCommandHandler.getInstance().getVoicedCommandHandler(comm);
				try {
					String desc = handler.getDescription(comm);
					if(desc == null)
						desc = "Описание не доступно.";
					html.append("<tr><td width=190><font color=\"00FF00\">" + comm + "</font></td><td> - " + desc + "</td></tr>");
				} catch(AbstractMethodError e) {
					
				}
			} catch(Exception e) {
				continue;
			}
			html.append("</table></center>");
			html.append("</body></html>");
			help.setHtml(html.toString());
			activeChar.sendPacket(help);
			return true;
		}
		return false;
	}

	public String getDescription(String command)
	{
		if(command.equals("help"))
			return "Выводит текущее меню.";
		return null;
	}

	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	
}