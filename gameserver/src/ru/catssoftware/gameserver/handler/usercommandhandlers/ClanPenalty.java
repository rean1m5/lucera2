package ru.catssoftware.gameserver.handler.usercommandhandlers;

import java.text.SimpleDateFormat;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

import javolution.text.TextBuilder;


public class ClanPenalty implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 100 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;

		boolean penalty = false;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		TextBuilder htmlContent = new TextBuilder("<html><body>");
		htmlContent.append("<center><table width=270 border=0 bgcolor=111111>");
		htmlContent.append("<tr><td width=170>Штраф</td>");
		htmlContent.append("<td width=100 align=center>Окончание:</td></tr>");
		htmlContent.append("</table><table width=270 border=0><tr>");

		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			htmlContent.append("<td width=170>Вступите в клан.</td>");
			htmlContent.append("<td width=100 align=center>"+format.format(activeChar.getClanJoinExpiryTime())+"</td>");
			penalty = true;
		}
		if (activeChar.getClanCreateExpiryTime() > System.currentTimeMillis())
		{
			htmlContent.append("<td width=170>Создайте клан.</td>");
			htmlContent.append("<td width=100 align=center>"+format.format(activeChar.getClanCreateExpiryTime())+"</td>");
			penalty = true;
		}
		if (!penalty)
		{
			htmlContent.append("<td width=170>Штраф отсутствует.</td>");
			htmlContent.append("<td width=100 align=center> </td>");
		}

		htmlContent.append("</tr></table><img src=\"L2UI.SquareWhite\" width=270 height=1>");
		htmlContent.append("</center></body></html>");

		NpcHtmlMessage penaltyHtml = new NpcHtmlMessage(0);
		penaltyHtml.setHtml(htmlContent.toString());
		activeChar.sendPacket(penaltyHtml);

		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}