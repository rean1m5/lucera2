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

import java.util.Collection;
import java.util.StringTokenizer;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

import javolution.util.FastList;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class L2DoormenInstance extends L2FolkInstance
{
	private ClanHall			_clanHall;
	private static final int	COND_ALL_FALSE				= 0;
	private static final int	COND_BUSY_BECAUSE_OF_SIEGE	= 1;
	private static final int	COND_CASTLE_OWNER			= 2;
	private static final int	COND_HALL_OWNER				= 3;
	private static final int	COND_FORT_OWNER				= 4;

	/**
	 * @param template
	 */
	public L2DoormenInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	public final ClanHall getClanHall()
	{
		if (_clanHall == null)
			_clanHall = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 500);
		return _clanHall;
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
			return;
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			return;
		else if (condition == COND_CASTLE_OWNER || condition == COND_HALL_OWNER || condition == COND_FORT_OWNER)
		{
			if (command.startsWith("Chat"))
			{
				showMessageWindow(player);
				return;
			}
			else if (command.startsWith("open_doors"))
			{
				if (condition == COND_HALL_OWNER)
				{
					getClanHall().openCloseDoors(true);
					player.sendPacket(new NpcHtmlMessage(
									getObjectId(),
									"<html><body>Вы <font color=\"FF9955\">открыли</font> двери в Клан-Холл.<br>Чужаки могут попасть суда, когда открыта дверь. Пожалуйста, закройте за собой дверь, когда уйдете.<br><center><button value=\"Close\" action=\"bypass -h npc_"
											+ getObjectId()
											+ "_close_doors\" width=80 height=27 back=\"sek.cbui94\" fore=\"L2UI_CT1.Button_DF\"></center></body></html>"));
				}
				else if (condition == COND_CASTLE_OWNER)
				{
					if (!validatePrivileges(player, L2Clan.CP_CS_OPEN_DOOR)) return;
					if (!Config.SIEGE_GATE_CONTROL && getCastle().getSiege().getIsInProgress()) {
						player.sendPacket(SystemMessageId.GATES_NOT_OPENED_CLOSED_DURING_SIEGE);
						return;
					}
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid/fortid

					while (st.hasMoreTokens())
					{
						getCastle().openDoor(player, Integer.parseInt(st.nextToken()));
					}
					return;
				}
				else if (condition == COND_FORT_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid/fortid

					while (st.hasMoreTokens())
					{
						getFort().openDoor(Integer.parseInt(st.nextToken()));
					}
					return;
				}
			}
			else if (command.startsWith("close_doors"))
			{
				if (condition == COND_HALL_OWNER)
				{
					getClanHall().openCloseDoors(false);
					player.sendPacket(new NpcHtmlMessage(getObjectId(),
							"<html><body>Вы <font color=\"FF9955\">закрыли</font> двери в Клан-Хол.<br>Хороший день!<br><center><button value=\"To Beginning\" action=\"bypass -h npc_"
									+ getObjectId()
									+ "_Chat\" width=80 height=27 back=\"sek.cbui94\" fore=\"L2UI_CT1.Button_DF\"></center></body></html>"));
				}
				else if (condition == COND_CASTLE_OWNER)
				{
					if (!validatePrivileges(player, L2Clan.CP_CS_OPEN_DOOR)) return;
					if (!Config.SIEGE_GATE_CONTROL && getCastle().getSiege().getIsInProgress()) {
						player.sendPacket(SystemMessageId.GATES_NOT_OPENED_CLOSED_DURING_SIEGE);
						return;
					}
					StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid/fortid

					while (st.hasMoreTokens())
					{
						getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
					}
					return;
				}
				else if (condition == COND_FORT_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid/fortid

					while (st.hasMoreTokens())
					{
						getFort().closeDoor(Integer.parseInt(st.nextToken()));
					}
					return;
				}
			}
			//L2EMU_ADD - Scarxx
			else if (condition == COND_FORT_OWNER && command.startsWith("open_near_doors"))
			{
				for (L2DoorInstance door : getKnownDoors(player))
					door.openMe();
			}
			else if (condition == COND_FORT_OWNER && command.startsWith("close_near_doors"))
			{
				for (L2DoorInstance door : getKnownDoors(player))
					door.closeMe();
			}
			//L2EMU_ADD - Open doors near the npc
		}

		super.onBypassFeedback(player, command);
	}

	/**
	* this is called when a player interacts with this NPC
	* 
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

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance
			// player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the
			// L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the
			// L2NpcInstance
			if (!canInteract(player))
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
				showMessageWindow(player);
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to
		// avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/doormen/" + getTemplate().getNpcId() + "-no.htm";

		int condition = validateCondition(player);
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			filename = "data/html/doormen/" + getTemplate().getNpcId() + "-busy.htm"; // Busy because of siege
		else if (condition == COND_CASTLE_OWNER || condition == COND_FORT_OWNER) // Clan owns castle or fort
			filename = "data/html/doormen/" + getTemplate().getNpcId() + ".htm"; // Owner message window

		// Prepare doormen for clan hall
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String str;
		if (getClanHall() != null)
		{
			if (condition == COND_HALL_OWNER)
			{
				str = "<html><body>Добрый день!<br><font color=\"00FFFF\">" + getClanHall().getOwnerClan().getName()
						+ "</font> клан, которому я имею честь служить.<br>Как я могу помочь вам?<br>";
				str += "<center><br><button value=\"Открыть КХ\" action=\"bypass -h npc_%objectId%_open_doors\" width=80 height=27 back=\"sek.cbui94\" fore=\"sek.cbui94\"><br>";
				str += "<button value=\"Закрыть КХ\" action=\"bypass -h npc_%objectId%_close_doors\" width=80 height=27 back=\"sek.cbui94\" fore=\"sek.cbui94\"><br>";
				str += "</center></body></html>";
			}
			else
			{
				L2Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
				if (owner != null && owner.getLeader() != null)
				{
					str = "<html><body>Добрый день!<br><font color=\"LEVEL\">" + getClanHall().getName() + "</font> владеет данным клан-холом, <font color=\"55FFFF\">" + owner.getLeader().getName()
							+ " лидер ";
					str += owner.getName() + "</font> лидер.<br>";
					str += "Извини, только участники клана " + owner.getName() + " могут сюда войти.</body></html>";
				}
				else
				{
					int ClanHallID = _clanHall.getId();
					if (ClanHallID==21 ||ClanHallID==34 ||ClanHallID==35 ||ClanHallID==62 ||ClanHallID==63 ||ClanHallID==64)
						str = "<html><body>Клан-Холл <font color=\"LEVEL\">" + getClanHall().getName()
						+ "</font> не имеет владельца.<br>Чтобы завладеть этим Холл Кланом, Вы должны захватить его.</body></html>";
					else
						str = "<html><body>Клан-Холл <font color=\"LEVEL\">" + getClanHall().getName()
						+ "</font> не имеет владельца.<br>Для покупки идите на Аукцион.</body></html>";
				}
			}
			html.setHtml(str);
		}
		else
			html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
 	
	private boolean validatePrivileges(L2PcInstance player, int privilege) {
		if ((player.getClanPrivileges() & privilege) != privilege) {
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}
		return true;
	}

	private int validateCondition(L2PcInstance player)
	{
		if (player.getClan() != null)
		{
			int clanId = player.getClanId();
			// Prepare doormen for clan hall
			if (getClanHall() != null)
			{
				if (getClanHall().getOwnerId() == clanId)
					return COND_HALL_OWNER;
			}
			if (getCastle() != null && getCastle().getCastleId() > 0)
			{
				if (getCastle().getSiege().getIsInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE;
				if (getCastle().getOwnerId() == clanId) // Clan owns castle
					return COND_CASTLE_OWNER; // Owner
			}
			if (getFort() != null && getFort().getFortId() > 0)
			{
				if (getFort().getSiege().getIsInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE;
				if (getFort().getOwnerId() == clanId)
					return COND_FORT_OWNER;
			}
		}
		return COND_ALL_FALSE;
	}

	// ----------- L2Emu Addons ---------------------------------------------------
	/**
	 * Funtion get the doors near the Doormen
	 * can be open by the player
	 * @param player
	 */
	private Collection<L2DoorInstance> getKnownDoors(L2PcInstance player)
	{
		//Container
		FastList<L2DoorInstance> _doors = new FastList<L2DoorInstance>();

		// Get all objects in the doorman knownlist and select the doors
		for (L2Object object : getKnownList().getKnownObjects().values())
		{
			if (object instanceof L2DoorInstance)
			{
				L2DoorInstance door = null;
				door = (L2DoorInstance) object;

				if (door != null)
				{
					if (door.getCastle() != null && door.getCastle().getOwnerId() == player.getClanId() || door.getFort() != null
							&& door.getFort().getOwnerId() == player.getClanId())
						_doors.add(door);
				}
			}
		}
		return _doors;
	}
}