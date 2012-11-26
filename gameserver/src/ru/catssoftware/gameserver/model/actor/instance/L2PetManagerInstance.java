/*
 * $Header$
 *
 *
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


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.xml.SummonItemsData;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.L2SummonItem;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillLaunched;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class L2PetManagerInstance extends L2MerchantInstance
{
	private final static Logger	_log			= Logger.getLogger(L2PetManagerInstance.class.getName());

	public L2PetManagerInstance(int objectID, L2NpcTemplate template)
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
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));

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

	private void showMessageWindow(L2PcInstance player)
	{
		String filename = "data/html/petmanager/" + getNpcId() + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		if (Config.ALLOW_RENTPET && Config.LIST_PET_RENT_NPC.contains(getNpcId()))
			html.replace("_Quest", "_RentPet\">Взять питомца на прокат</a><br><a action=\"bypass -h npc_%objectId%_Quest");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	@Override
	protected String getHtmlFolder() {
		return "petmanager";
	}
	

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("exchange"))
		{
			String[] params = command.split(" ");
			int val = Integer.parseInt(params[1]);
			switch (val)
			{
				case 1:
					exchange(player, 7585, 6650);
					break;
				case 2:
					exchange(player, 7583, 6648);
					break;
				case 3:
					exchange(player, 7584, 6649);
					break;
			}
		}
		else if (command.startsWith("evolve"))
		{
			String[] params = command.split(" ");
			int val = Integer.parseInt(params[1]);
			switch (val)
			{
				case 1:
					evolve(player, 2375, 9882, 55);
					break;
				case 2:
					evolve(player, 9882, 10426, 70);
					break;
				case 3:
					evolve(player, 6648, 10311, 55);
					break;
				case 4:
					evolve(player, 6650, 10313, 55);
					break;
				case 5:
					evolve(player, 6649, 10312, 55);
					break;
			}
		}
		else if (command.startsWith("restore"))
		{
			String[] params = command.split(" ");
			int val = Integer.parseInt(params[1]);
			switch (val)
			{
				case 1:
					restore(player, 10307, 9882);
					break;
				case 2:
					restore(player, 10611, 10426);
					break;
				case 3:
					restore(player, 10308, 4422);
					break;
				case 4:
					restore(player, 10309, 4423);
					break;
				case 5:
					restore(player, 10310, 4424);
					break;
			}
		}
		super.onBypassFeedback(player, command);
	}

	public final void exchange(L2PcInstance player, int itemIdtake, int itemIdgive)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (player.destroyItemByItemId("Consume", itemIdtake, 1, this, true))
		{
			player.addItem("", itemIdgive, 1, this, true);
			html.setFile("data/html/petmanager/" + getNpcId() + ".htm");
			player.sendPacket(html);
		}
		else
		{
			html.setFile("data/html/petmanager/exchange_no.htm");
			player.sendPacket(html);
		}
	}
	public final void restore(L2PcInstance player, int itemIdtake, int itemIdgive)
	{
		L2Summon Summon = player.getPet();
		if (Summon!=null)
		{
			Summon.unSummon(player);
		}
		L2SummonItem olditem = SummonItemsData.getInstance().getSummonItem(itemIdtake);
		L2ItemInstance item = player.getInventory().getItemByItemId(itemIdtake);
		
		if (olditem == null)
			return;
		if (item == null)
			return;

		int npcID = olditem.getNpcId();

		if (npcID == 0)
			return;
		
		L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(npcID);
		L2PetInstance oldSummon = L2PetInstance. spawnPet(npcTemplate, player, item);
		player.setPet(oldSummon);
		L2World.getInstance().storeObject(oldSummon);
		oldSummon.spawnMe();
		
		evolve(player,itemIdtake,itemIdgive,55);
	}
	public final void evolve(L2PcInstance player, int itemIdtake, int itemIdgive, int petminlvl)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		L2Summon curpet = player.getPet();

		if ((curpet == null) || (itemIdtake == 0) || (itemIdgive == 0) || (petminlvl == 0))
		{
			html.setFile("data/html/petmanager/evolve_no.htm");
			player.sendPacket(html);
			return;
		}

		L2ItemInstance item = null;
		long petexp = curpet.getStat().getExp();
		String oldname = curpet.getName();

		L2SummonItem olditem = SummonItemsData.getInstance().getSummonItem(itemIdtake);

		if (olditem == null)
			return;

		int oldnpcID = olditem.getNpcId();

		if (curpet.getNpcId() != oldnpcID)
		{
			html.setFile("data/html/petmanager/evolve_no.htm");
			player.sendPacket(html);
			return;
		}

		if (curpet.getStat().getLevel() < petminlvl)
		{
			html.setFile("data/html/petmanager/evolve_nolvl.htm");
			player.sendPacket(html);
			return;
		}

		item = player.getInventory().addItem("", itemIdgive, 1, player, null);
		ItemList il = new ItemList(player, true);
		player.sendPacket(il);

		L2SummonItem sitem = SummonItemsData.getInstance().getSummonItem(item.getItemId());

		int npcID = sitem.getNpcId();

		if (npcID == 0)
			return;

		L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(npcID);

		curpet.unSummon(player);

		//Summoning new pet
		L2PetInstance petSummon = L2PetInstance.spawnPet(npcTemplate, player, item);

		if (petSummon == null)
			return;

		//If new pet ok, deleting old pet item
		player.destroyItem("", curpet.getControlItemId(), 1, this, true);

		petSummon.getStat().addExp(petexp);
		petSummon.getStatus().setCurrentHp(petSummon.getMaxHp());
		petSummon.getStatus().setCurrentMp(petSummon.getMaxMp());
		petSummon.setCurrentFed(petSummon.getMaxFed());
		petSummon.setTitle(player.getName());
		petSummon.setName(oldname);
		petSummon.setRunning();
		petSummon.store();

		player.setPet(petSummon);

		player.sendPacket(new MagicSkillUse(this, 2046, 1, 1000, 600000, false));
		player.sendPacket(SystemMessageId.SUMMON_A_PET);
		L2World.getInstance().storeObject(petSummon);
		petSummon.spawnMe();
		petSummon.startFeed();
		item.setEnchantLevel(petSummon.getLevel());

		ThreadPoolManager.getInstance().scheduleGeneral(new EvolveFinalizer(player, petSummon), 900);

		if (petSummon.getCurrentFed() <= 0)
			ThreadPoolManager.getInstance().scheduleGeneral(new EvolveFeedWait(player, petSummon), 60000);
		else
			petSummon.startFeed();
	}

	static class EvolveFeedWait implements Runnable
	{
		private L2PcInstance	_activeChar;
		private L2PetInstance	_petSummon;

		EvolveFeedWait(L2PcInstance activeChar, L2PetInstance petSummon)
		{
			_activeChar = activeChar;
			_petSummon = petSummon;
		}

		public void run()
		{
			try
			{
				if (_petSummon.getCurrentFed() <= 0)
					_petSummon.unSummon(_activeChar);
				else
					_petSummon.startFeed();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	static class EvolveFinalizer implements Runnable
	{
		private L2PcInstance	_activeChar;
		private L2PetInstance	_petSummon;

		EvolveFinalizer(L2PcInstance activeChar, L2PetInstance petSummon)
		{
			_activeChar = activeChar;
			_petSummon = petSummon;
		}

		public void run()
		{
			try
			{
				_activeChar.sendPacket(new MagicSkillLaunched(_activeChar, 2046, 1, false));
				_petSummon.setFollowStatus(true);
				_petSummon.setShowSummonAnimation(false);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}