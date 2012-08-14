package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2SiegeStatus;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.L2SiegeStatus.PlayerInfo;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import javolution.text.TextBuilder;

public class SiegeStatus implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 99 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (activeChar.isClanLeader() && activeChar.isNoble())
		{
			L2PcInstance player=null;
			NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
			TextBuilder replyMSG = new TextBuilder("<html><body>");
			replyMSG.append("<img src=\"L2UI.SquareWhite\" width=280 height=1>");
			replyMSG.append("<table width=280 border=0 bgcolor=\"000000\">" +
					"<tr><td width=140 align=center>Член клана</td>" +
					"<td width=70 align=center>Убил</td>" +
					"<td width=70 align=center>Умер</td>" +
			"</tr></table>");
			replyMSG.append("<img src=\"L2UI.SquareWhite\" width=280 height=1>");
			replyMSG.append("<table width=280 border=0 bgcolor=\"000000\">" +
					"<tr><td width=140 align=center> </td>" +
					"<td width=70 align=center></td>" +
					"<td width=70 align=center></td>" +
			"</tr>");

			L2Clan clan = activeChar.getClan();
			if (clan != null)
			{
				for (PlayerInfo playerInfo : L2SiegeStatus.getInstance().getMembers(clan.getClanId()))
				{
					player=L2World.getInstance().getPlayer(playerInfo._playerId);
					if(player!=null)
						replyMSG.append("<tr><td width=140>  "+player.getName()+"</td><td width=70 align=center>"+playerInfo._kill+"</td><td width=70 align=center>"+playerInfo._death +"</td></tr>");
				}
			}
			replyMSG.append("</table><img src=\"L2UI.SquareWhite\" width=280 height=1>");
			replyMSG.append("</body></html>");
			html.setHtml(replyMSG.toString());
			activeChar.sendPacket(html);
		}
		else
			activeChar.sendPacket(SystemMessageId.ONLY_NOBLESSE_LEADER_CAN_VIEW_SIEGE_STATUS_WINDOW);
		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}