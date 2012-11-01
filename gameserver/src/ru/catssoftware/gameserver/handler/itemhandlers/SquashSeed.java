package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SquashInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class SquashSeed implements IItemHandler
{
	private static final int[]	ITEM_IDS	= { 6389 , 6390 };
	
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable.isPlayer()))
			return;
		L2PcInstance activeChar = (L2PcInstance) playable;
		if (!Config.BIGSQUASH_USE_SEEDS)
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_GROW_PUMPKIN));
			return;
		}
		if (!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), 1, null, false))
		{
			activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return;
		}
		L2NpcTemplate squashTemplate = null;
		L2SquashInstance squash = null;
		switch (item.getItemId())
		{
		case 6389:
			squashTemplate = NpcTable.getInstance().getTemplate(12774);
			squash = new L2SquashInstance(IdFactory.getInstance().getNextId(), squashTemplate,activeChar);
			squash.getStatus().setCurrentHpMp(1, 1);
			squash.getStatus().stopHpMpRegeneration();
			squash.setLevel(activeChar.getLevel());
			squash.spawnMe(activeChar.getX()+5,activeChar.getY()+5,activeChar.getZ());
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_SQUASH_SUCCESS_GROW));
			break;
		case 6390:
			squashTemplate = NpcTable.getInstance().getTemplate(12777);
			squash = new L2SquashInstance(IdFactory.getInstance().getNextId(), squashTemplate,activeChar);
			squash.getStatus().setCurrentHpMp(1, 1);
			squash.getStatus().stopHpMpRegeneration();
			squash.setLevel(activeChar.getLevel());			
			squash.spawnMe(activeChar.getX()+5,activeChar.getY()+5,activeChar.getZ());
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_SQUASH_SUCCESS_GROW));
			break;
		}
		
	}
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}	
}