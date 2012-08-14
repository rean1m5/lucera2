/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.catssoftware.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;


/**
 * @author Vice 
 */
public class L2FortSiegeNpcInstance extends L2NpcWalkerInstance
{
	public L2FortSiegeNpcInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
				showMessageWindow(player);
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		String par = "";
		if (st.countTokens() >= 1) {par = st.nextToken();}

		if (actualCommand.equalsIgnoreCase("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(par);
			}
			catch (IndexOutOfBoundsException ioobe){}
			catch (NumberFormatException nfe){}
			showMessageWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("register"))
		{
			if (player.getClan() == null || !player.isClanLeader()
					|| (getFort().getOwnerClan() != null && player.getClan().getHasCastle() > 0
							&& player.getClan().getHasCastle() != getFort().getCastleId())
					|| player.getClan().getLevel() < 4)
			{
				showMessageWindow(player, 7);
			}
			else if (getFort().getSiege().getAttackerClans().size() == 0 && player.getInventory().getAdena() < 250000)
				showMessageWindow(player, 8);
			else
			{
				if (getFort().getSiege().registerAttacker(player, false))
				{
					player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_REGISTERD_ON_FORT_SIEGE), getFort().getName()));
				}
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	private void showMessageWindow(L2PcInstance player)
	{
		showMessageWindow(player, 0);
	}

	private void showMessageWindow(L2PcInstance player, int val)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		String filename;

		if (val == 0)
			filename = "data/html/fortress/merchant.htm";
		else
			filename = "data/html/fortress/merchant-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		if (getFort().getOwnerClan() != null)
			html.replace("%clanname%", getFort().getOwnerClan().getName());
		else
			html.replace("%clanname%", "NPC");
		
		player.sendPacket(html);
	} 

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
