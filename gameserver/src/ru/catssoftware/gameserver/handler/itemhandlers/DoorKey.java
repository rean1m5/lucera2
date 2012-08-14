package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.PlaySound;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.tools.random.Rnd;

/**
 * @author  chris
 */
public class DoorKey implements IItemHandler
{
	private static final int[]	ITEM_IDS =
	{
		// Pagan temple
		8273, 8274, 8275,
		// Key of Splendor Room
		8056,
		// Key of Enigma
		8060
	};

	public static final int		INTERACTION_DISTANCE	= 100;
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		int itemId = item.getItemId();
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;

		// Key of Enigma (Pavel Research Quest)
		if (itemId == 8060)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(2260, 1);
			if (skill != null)
				activeChar.doSimultaneousCast(skill);
			return;
		}

		L2Object target = activeChar.getTarget();

		if (!(target instanceof L2DoorInstance))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		L2DoorInstance door = (L2DoorInstance) target;

		if (!(activeChar.isInsideRadius(door, INTERACTION_DISTANCE, false, false)))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_TOO_FAR);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.getAbnormalEffect() > 0 || activeChar.isInCombat())
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_CURRENT_IN_COMBAT));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		int openChance = 35;

		switch (itemId)
		{
		case 8273: //AnteroomKey
		{
			if (door.getDoorName().startsWith("Anteroom"))
			{
				if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
					return;

				if (openChance > 0 && Rnd.get(100) < openChance)
				{
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_OPEN),"Anterooms Door."));
					door.openMe();
					door.onOpen(); // Closes the door after 60sec
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
				}
				else
				{
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_FAILD_OPEN),"Anterooms Door."));
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
					PlaySound playSound = new PlaySound(0, "interfacesound.system_close_01");
					activeChar.sendPacket(playSound);
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			break;
		}
		case 8274: //Chapelkey, Capel Door has a Gatekeeper?? I use this key for Altar Entrance
		{
			if (door.getDoorName().startsWith("Altar_Entrance"))
			{
				if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
					return;

				if (openChance > 0 && Rnd.get(100) < openChance)
				{
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_OPEN),"Altar Entrance."));
					door.openMe();
					door.onOpen(); // Auto close
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
				}
				else
				{
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_FAILD_OPEN),"Altar Entrance."));
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
					PlaySound playSound = new PlaySound(0, "interfacesound.system_close_01");
					activeChar.sendPacket(playSound);
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			break;
		}
		case 8275: //Key of Darkness
		{
			if (door.getDoorName().startsWith("Door_of_Darkness"))
			{
				if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
					return;

				if (openChance > 0 && Rnd.get(100) < openChance)
				{
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_OPEN),"Door of Darkness."));
					door.openMe();
					door.onOpen(); // Auto close
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
				}
				else
				{
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_FAILD_OPEN),"Door of Darkness."));
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
					PlaySound playSound = new PlaySound(0, "interfacesound.system_close_01");
					activeChar.sendPacket(playSound);
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			break;
		}
		case 8056: //Splendor room
		{
			if ((door.getDoorId() != 23150001 && door.getDoorId() != 23150002 && door.getDoorId() != 23150003 && door.getDoorId() != 23150004) || door.getOpen())
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
				return;

			door.openMe();
			door.onOpen(); // Auto close
			break;
		}
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}