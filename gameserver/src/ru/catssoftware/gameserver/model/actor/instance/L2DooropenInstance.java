/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;


/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class L2DooropenInstance extends L2FolkInstance
{
	/**
	 * @param template
	 */
	public L2DooropenInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("Chat"))
		{
			showMessageWindow(player);
			return;
		}
		else if (command.startsWith("open_doors"))
		{
			DoorTable doorTable = DoorTable.getInstance();
			StringTokenizer st = new StringTokenizer(command.substring(10), ", ");

			while (st.hasMoreTokens())
			{
				int _doorid = Integer.parseInt(st.nextToken());
				doorTable.getDoor(_doorid).openMe();
			}
			return;

		}
		super.onBypassFeedback(player, command);
	}

	/**
	* this is called when a player interacts with this NPC
	* @param player
	*/
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
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				showMessageWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void showMessageWindow(L2PcInstance player)
	{
		//player.sendPacket(new ActionFailed());
		String filename = "data/html/dooropen/" + getTemplate().getNpcId() + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
}
