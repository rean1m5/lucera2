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
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.PetDataTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2WyvernManagerInstance extends L2CastleChamberlainInstance
{
	public L2WyvernManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("RideWyvern"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
				return;
			}

			if ((SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK) && SevenSigns.getInstance().isSealValidationPeriod())
			{
				player.sendPacket(SystemMessageId.SEAL_OF_STRIFE_FORBIDS_SUMMONING);
				return;
			}

			int petItemId = 0;
			L2ItemInstance petItem = null;

			if (player.getPet() == null)
			{
				if (player.isMounted())
				{
					petItem = player.getInventory().getItemByObjectId(player.getMountObjectID());
					if (petItem != null)
						petItemId = petItem.getItemId();
				}
			}
			else
				petItemId = player.getPet().getControlItemId();

			if (petItemId == 0 || !player.isMounted() || (!PetDataTable.isStrider(PetDataTable.getPetIdByItemId(petItemId))))
			{
				player.sendPacket(SystemMessageId.YOU_MAY_ONLY_RIDE_WYVERN_WHILE_RIDING_STRIDER);
				return;
			}
			else if (player.isMounted() && (PetDataTable.isStrider(PetDataTable.getPetIdByItemId(petItemId)) && petItem != null && petItem.getEnchantLevel() < 55))
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_WRONG_STRIDER_LEVEL));
				return;
			}

			// Wyvern requires Config.MANAGER_CRYSTAL_COUNT crystal for ride...
			if (player.getInventory().getItemByItemId(1460) != null && player.getInventory().getItemByItemId(1460).getCount() >= Config.MANAGER_CRYSTAL_COUNT)
			{
				if (!player.disarmWeapons())
					return;

				if (player.isMounted())
					player.dismount();

				if (player.getPet() != null)
					player.getPet().unSummon(player);

				if (player.mount(12621, 0, true))
				{
					L2ItemInstance cryB = player.getInventory().getItemByItemId(1460);
					player.getInventory().destroyItemByItemId("Wyvern", 1460, Config.MANAGER_CRYSTAL_COUNT, player, player.getTarget());
					InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(cryB);
					player.sendPacket(iu);
					player.addSkill(SkillTable.getInstance().getInfo(4289, 1));
				}
			}
			else
				player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_NEED_ITEM), Config.MANAGER_CRYSTAL_COUNT + " Crystals: B Grade."));
		}
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
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/wyvernmanager/wyvernmanager-no.htm";

		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_OWNER) // Clan owns castle
				filename = "data/html/wyvernmanager/wyvernmanager.htm"; // Owner message window
		}
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		html.replace("%count%", String.valueOf(Config.MANAGER_CRYSTAL_COUNT));
		player.sendPacket(html);
	}
}
