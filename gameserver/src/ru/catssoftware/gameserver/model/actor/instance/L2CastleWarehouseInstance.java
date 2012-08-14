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

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.WareHouseDepositList;
import ru.catssoftware.gameserver.network.serverpackets.WareHouseWithdrawalList;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.Util;

/**
 * @author l3x
 */
public class L2CastleWarehouseInstance extends L2FolkInstance
{
	protected static final int	COND_ALL_FALSE				= 0;
	protected static final int	COND_BUSY_BECAUSE_OF_SIEGE	= 1;
	protected static final int	COND_OWNER					= 2;

	private static final int 	BLOOD_ALLIANCE				= 9911;
	
	/**
	 * @param template
	 */
	public L2CastleWarehouseInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	private void showRetrieveWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());

		if (player.getActiveWarehouse().getSize() == 0)
		{
			player.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
			return;
		}

		player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE));
	}

	private void showDepositWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());
		player.tempInvetoryDisable();

		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.PRIVATE));
	}

	private void showDepositWindowClan(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		if (player.getClan() != null)
		{
			if (player.getClan().getLevel() == 0)
				player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
			else
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
				{
					player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CAN_RETRIEVE_ITEMS_FROM_CLAN_WAREHOUSE);
				}
				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.tempInvetoryDisable();

				WareHouseDepositList dl = new WareHouseDepositList(player, WareHouseDepositList.CLAN);
				player.sendPacket(dl);
			}
		}
	}

	private void showWithdrawWindowClan(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE);
			return;
		}

		if (player.getClan().getLevel() == 0)
			player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
		else
		{
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
		}
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (player.getActiveEnchantItem() != null)
		{
			Util.handleIllegalPlayerAction(player, "Внимание! Игрок " + player.getName() + " (Аккаунт: " + player.getAccountName()
					+ ") пытается использовать чит на точку!", Config.DEFAULT_PUNISH);
			return;
		}

		if (command.startsWith("WithdrawP"))
			showRetrieveWindow(player);
		else if (command.equals("DepositP"))
			showDepositWindow(player);
		else if (command.equals("WithdrawC"))
			showWithdrawWindowClan(player);
		else if (command.equals("DepositC"))
			showDepositWindowClan(player);
		else if (command.equals("HonorItem"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			String filename = "data/html/castlewarehouse/castlewarehouse-no.htm";
			int condition = validateCondition(player);
			if (condition == COND_OWNER)
			{
				Castle castle = getCastle();
				if ( castle != null && castle.getCastleId() > 0 && castle.getOwnerId()==player.getClanId())
				{
					int bloodAlianceCnt = getCastle().getBloodAliance(); 
					if (bloodAlianceCnt>0)
					{
						getCastle().setBloodAliance(0);
						player.getClan().getWarehouse().addItem("DefendSuccess", BLOOD_ALLIANCE, bloodAlianceCnt, null, null);
						filename = "data/html/castlewarehouse/castlewarehouse-reward.htm";						
					}
				}
			}
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%npcname%", getName());
			player.sendPacket(html);
		}
		else if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			showChatWindow(player, val);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/castlewarehouse/castlewarehouse-no.htm";

		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castlewarehouse/castlewarehouse-busy.htm"; // Busy because of siege
			else if (condition == COND_OWNER)
			{
				if (val == 0)
					filename = "data/html/castlewarehouse/castlewarehouse.htm";
				else
					filename = "data/html/castlewarehouse/castlewarehouse-" + val + ".htm";
			}
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	protected int validateCondition(L2PcInstance player)
	{
		if (player.isGM())
			return COND_OWNER;
		if (getCastle() != null && getCastle().getCastleId() > 0)
		{
			if (player.getClan() != null)
			{
				if (getCastle().getSiege().getIsInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				else if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
					return COND_OWNER;
			}
		}
		return COND_ALL_FALSE;
	}
}