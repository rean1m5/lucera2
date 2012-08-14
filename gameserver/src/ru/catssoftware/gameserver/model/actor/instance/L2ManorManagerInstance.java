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
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager.SeedProduction;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2TradeList;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.BuyListSeed;
import ru.catssoftware.gameserver.network.serverpackets.ExShowCropInfo;
import ru.catssoftware.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import ru.catssoftware.gameserver.network.serverpackets.ExShowProcureCropDetail;
import ru.catssoftware.gameserver.network.serverpackets.ExShowSeedInfo;
import ru.catssoftware.gameserver.network.serverpackets.ExShowSellCropList;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

import javolution.util.FastList;

public class L2ManorManagerInstance extends L2MerchantInstance
{

	public L2ManorManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		player.setLastFolkNPC(this);

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The player.getLevel() - getLevel() permit to display the correct color in the select window
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
				// If player is a lord of this manor, alternative message from NPC
				if (CastleManorManager.getInstance().isDisabled())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/npcdefault.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
				}
				else if (!player.isGM() // Player is not GM
						&& getCastle() != null && getCastle().getCastleId() > 0 // Verification of castle
						&& player.getClan() != null // Player have clan
						&& getCastle().getOwnerId() == player.getClanId() // Player's clan owning the castle
						&& player.isClanLeader() // Player is clan leader of clan (then he is the lord)
				)
				{
					showMessageWindow(player, "manager-lord.htm");
				}
				else
				{
					showMessageWindow(player, "manager.htm");
				}
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// BypassValidation Exploit plug.
		if (player.getLastFolkNPC() == null || player.getLastFolkNPC().getObjectId() != this.getObjectId())
			return;

		if (command.startsWith("manor_menu_select"))
		{
			// input string format:
			// manor_menu_select?ask=X&state=Y&time=X

			if (CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendPacket(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			String params = command.substring(command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int ask = Integer.parseInt(st.nextToken().split("=")[1]);
			int state = Integer.parseInt(st.nextToken().split("=")[1]);
			int time = Integer.parseInt(st.nextToken().split("=")[1]);

			int castleId;
			if (state == -1) // info for current manor
				castleId = getCastle().getCastleId();
			else
				// info for requested manor
				castleId = state;

			switch (ask)
			{ // Main action
			case 1: // Seed purchase
				if (castleId != getCastle().getCastleId())
				{
					player.sendPacket(SystemMessageId.HERE_YOU_CAN_BUY_ONLY_SEEDS_OF_S1_MANOR);
				}
				else
				{
					L2TradeList tradeList = new L2TradeList(0);
					FastList<SeedProduction> seeds = getCastle().getSeedProduction(CastleManorManager.PERIOD_CURRENT);

					for (SeedProduction s : seeds)
					{
						L2ItemInstance item = ItemTable.getInstance().createDummyItem(s.getId());
						if(item==null) {
							_log.warn("L2ManorManagerInstance: Item  "+s.getId()+" not exists");
							continue;
						}
						item.setPriceToSell(s.getPrice());
						item.setCount(s.getCanProduce());
						if ((item.getCount() > 0) && (item.getPriceToSell() > 0))
							tradeList.addItem(item);
					}

					BuyListSeed bl = new BuyListSeed(tradeList, castleId, player.getAdena());
					player.sendPacket(bl);
				}
				break;
			case 2: // Crop sales
				player.sendPacket(new ExShowSellCropList(player, castleId, getCastle().getCropProcure(CastleManorManager.PERIOD_CURRENT)));
				break;
			case 3: // Current seeds (Manor info)
				if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					player.sendPacket(new ExShowSeedInfo(castleId, null));
				else
					player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
				break;
			case 4: // Current crops (Manor info)
				if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					player.sendPacket(new ExShowCropInfo(castleId, null));
				else
					player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getCropProcure(time)));
				break;
			case 5: // Basic info (Manor info)
				player.sendPacket(new ExShowManorDefaultInfo());
				break;
			case 6: // Buy harvester
				this.showBuyWindow(player, Integer.parseInt("3" + getNpcId()));
				break;
			case 9: // Edit sales (Crop sales)
				player.sendPacket(new ExShowProcureCropDetail(state));
				break;
			}
		}
		else if (command.startsWith("help"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // discard first
			String filename = "manor_client_help00" + st.nextToken() + ".htm";
			showMessageWindow(player, filename);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	public String getHtmlPath()
	{
		return "data/html/manormanager/";
	}


	@Override
	public String getHtmlPath(int npcId, int val,L2PcInstance talker)
	{
		return "data/html/manormanager/manager.htm";
		
	}

	private void showMessageWindow(L2PcInstance player, String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(getHtmlPath() + filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
