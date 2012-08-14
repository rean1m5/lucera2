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

import java.util.Map;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.instancemanager.TownManager;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.entity.Town;
import ru.catssoftware.gameserver.model.itemcontainer.PcFreight;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.PackageToList;
import ru.catssoftware.gameserver.network.serverpackets.SortedWareHouseWithdrawalList;
import ru.catssoftware.gameserver.network.serverpackets.WareHouseDepositList;
import ru.catssoftware.gameserver.network.serverpackets.WareHouseWithdrawalList;
import ru.catssoftware.gameserver.network.serverpackets.SortedWareHouseWithdrawalList.WarehouseListType;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.IllegalPlayerAction;
import ru.catssoftware.gameserver.util.Util;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.10 $ $Date: 2005/04/06 16:13:41 $
 */
public final class L2WarehouseInstance extends L2FolkInstance
{
	private final static Logger	_log			= Logger.getLogger(L2WarehouseInstance.class.getName());

	private int					_closestTownId	= -1;

	public L2WarehouseInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	protected String getHtmlFolder() {
		return "warehouse";
	}
	


	private void showRetrieveWindow(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());

		if (player.getActiveWarehouse().getSize() == 0)
		{
			player.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
			return;
		}
		player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE, itemtype, sortorder));
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
					player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CAN_RETRIEVE_ITEMS_FROM_CLAN_WAREHOUSE);

				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.tempInvetoryDisable();
				WareHouseDepositList dl = new WareHouseDepositList(player, WareHouseDepositList.CLAN);
				player.sendPacket(dl);
			}
		}
	}

	private void showWithdrawWindowClan(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		if (player.getClan() == null || player.getClan().getLevel() == 0)
			player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
		else if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE);
			return;
		}
		else
		{
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN, itemtype, sortorder));
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showWithdrawWindowClan(L2PcInstance player)
	{
		if (player.getClan() == null || player.getClan().getLevel() == 0)
			player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
		else if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE);
			return;
		}
		else
		{
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showWithdrawWindowFreight(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		PcFreight freight = player.getFreight();

		if (freight != null)
		{
			if (freight.getSize() > 0)
			{
				if (Config.ALT_GAME_FREIGHTS)
					freight.setActiveLocation(0);
				else
					freight.setActiveLocation(getClosestTown());
				player.setActiveWarehouse(freight);
				player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT, itemtype, sortorder));
			}
			else
				player.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
		}
		else{}
	}

	private void showWithdrawWindowFreight(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		PcFreight freight = player.getFreight();

		if (freight != null)
		{
			if (freight.getSize() > 0)
			{
				if (Config.ALT_GAME_FREIGHTS)
					freight.setActiveLocation(0);
				else
					freight.setActiveLocation(getClosestTown());
				player.setActiveWarehouse(freight);
				player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT));
			}
			else
				player.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
		}
		else{}
	}

	private void showDepositWindowFreight(L2PcInstance player)
	{
		// No other chars in the account of this player
		if (player.getAccountChars().size() == 0)
			player.sendPacket(SystemMessageId.CHARACTER_DOES_NOT_EXIST);
		else // One or more chars other than this player for this account
		{

			Map<Integer, String> chars = player.getAccountChars();

			if (chars.size() < 1)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			player.sendPacket(new PackageToList(chars));
		}
	}

	private void showDepositWindowFreight(L2PcInstance player, int obj_Id)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		PcFreight freight = new PcFreight(null);
		freight.doQuickRestore(obj_Id);

		if (Config.ALT_GAME_FREIGHTS)
			freight.setActiveLocation(0);
		else
			freight.setActiveLocation(getClosestTown());

		player.setActiveWarehouse(freight);
		player.tempInvetoryDisable();
		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.FREIGHT));
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// Lil check to prevent enchant exploit
		if (player.getActiveEnchantItem() != null)
		{
			Util.handleIllegalPlayerAction(player, "Игрок " + player.getName() + " пытается использовать баг с точкой!", IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		String param[] = command.split("_");

		if (command.startsWith("WithdrawP"))
		{
			if (Config.ENABLE_WAREHOUSESORTING_PRIVATE)
			{
				String htmFile = "data/html/custom/WhSortedP.htm";
				String htmContent = HtmCache.getInstance().getHtm(htmFile,player);
				if (htmContent != null)
				{
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(npcHtmlMessage);
				}
				else
					_log.warn("Missing htm: " + htmFile + " !");
			}
			else
				showRetrieveWindow(player);
		}
		else if (command.startsWith("WithdrawSortedP"))
		{
			if (param.length > 2)
				showRetrieveWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
			else if (param.length > 1)
				showRetrieveWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
			else
				showRetrieveWindow(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
		}
		else if (command.equals("DepositP"))
			showDepositWindow(player);
		else if (command.startsWith("WithdrawC"))
		{
			if (Config.ENABLE_WAREHOUSESORTING_CLAN)
			{
				String htmFile = "data/html/custom/WhSortedC.htm";
				String htmContent = HtmCache.getInstance().getHtm(htmFile,player);
				if (htmContent != null)
				{
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(npcHtmlMessage);
				}
				else
					_log.warn("Missing htm: " + htmFile + " !");
			}
			else
				showWithdrawWindowClan(player);
		}
		else if (command.startsWith("WithdrawSortedC"))
		{
			if (param.length > 2)
				showWithdrawWindowClan(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
			else if (param.length > 1)
				showWithdrawWindowClan(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
			else
				showWithdrawWindowClan(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
		}
		else if (command.equals("DepositC"))
			showDepositWindowClan(player);
		else if (command.startsWith("WithdrawF"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				if (Config.ENABLE_WAREHOUSESORTING_FREIGHT)
				{
					String htmFile = "data/html/custom/WhSortedF.htm";
					String htmContent = HtmCache.getInstance().getHtm(htmFile,player);
					if (htmContent != null)
					{
						NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
						npcHtmlMessage.setHtml(htmContent);
						npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(npcHtmlMessage);
					}
					else
						_log.warn("Missing htm: " + htmFile + " !");
				}
				else
					showWithdrawWindowFreight(player);
			}
		}
		else if (command.startsWith("WithdrawSortedF"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				if (param.length > 2)
					showWithdrawWindowFreight(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
				else if (param.length > 1)
					showWithdrawWindowFreight(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
				else
					showWithdrawWindowFreight(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
			}
		}
		else if (command.startsWith("DepositF"))
		{
			if (Config.ALLOW_FREIGHT)
				showDepositWindowFreight(player);
		}
		else if (command.startsWith("FreightChar"))
		{
			if (Config.ALLOW_FREIGHT && param.length > 1)
				showDepositWindowFreight(player, Integer.parseInt(param[1]));
		}
		else
			super.onBypassFeedback(player, command);
	}

	private int getClosestTown()
	{
		if (_closestTownId < 0)
		{
			Town town = TownManager.getInstance().getClosestTown(this);
			if (town != null)
				_closestTownId = town.getTownId();
			else
				_closestTownId = 0;
		}

		return _closestTownId;
	}
}