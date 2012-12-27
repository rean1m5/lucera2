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

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegion;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

import java.util.Collection;
import java.util.StringTokenizer;


/**
 * @author Kerberos
 */
public final class L2CastleTeleporterInstance extends L2FolkInstance
{
	private boolean	_currentTask	= false;

	/**
	* @param template
	*/
	public L2CastleTeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if (actualCommand.equalsIgnoreCase("tele"))
		{
			int delay;
			boolean longTime = getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0;
			if (!getTask())
			{
				if (longTime)
					delay = 480000;
				else
					delay = 30000;

				setTask(true);
				ThreadPoolManager.getInstance().scheduleGeneral(new oustAllPlayers(), delay);
			}

			String filename = longTime ? "data/html/castleteleporter/MassGK-4.htm" : "data/html/castleteleporter/MassGK-3.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			player.sendPacket(html);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename;
		boolean longTime = getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0;

		if (!getTask())
		{
			if (longTime)
				filename = "data/html/castleteleporter/MassGK-2.htm";
			else
				filename = "data/html/castleteleporter/MassGK-1.htm";
		}
		else if (longTime)
			filename = "data/html/castleteleporter/MassGK-4.htm";
		else
			filename = "data/html/castleteleporter/MassGK-3.htm";


		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	void oustAllPlayers()
	{
		getCastle().oustAllPlayers();
	}

	class oustAllPlayers implements Runnable
	{
		public void run()
		{
			try
			{
				NpcSay cs = new NpcSay(getObjectId(), 1, getNpcId(), "Защитники замка " + getCastle().getName()
						+ " будут перемещены в тронный зал.");
				L2MapRegion region = MapRegionManager.getInstance().getRegion(getX(), getY());
				Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers();
				{
					for (L2PcInstance player : pls)
					{
						if (region == MapRegionManager.getInstance().getRegion(player.getX(), player.getY()))
							player.sendPacket(cs);
					}
				}
				oustAllPlayers();
				setTask(false);
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
			}
		}
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
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
				showChatWindow(player);
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public boolean getTask()
	{
		return _currentTask;
	}

	public void setTask(boolean state)
	{
		_currentTask = state;
	}
}