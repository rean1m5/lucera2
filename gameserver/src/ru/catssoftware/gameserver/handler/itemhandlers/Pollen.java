package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SquashInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

public class Pollen implements IItemHandler
{
	private static final int[]	ITEM_IDS	= { 6391 };
	public static final int		INTERACTION_DISTANCE	= 100;
	
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2Object obj = activeChar.getTarget();
		if (obj!=null && obj instanceof L2SquashInstance)
		{
			if (activeChar.getRangeToTarget(obj)>INTERACTION_DISTANCE)
			{
				activeChar.sendPacket(SystemMessageId.TARGET_TOO_FAR);
				return;
			}
			L2SquashInstance target = ((L2SquashInstance)obj);
			if (target.getOwner()!=activeChar)
			{
				activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_WRONG_PUMPKIN));
				return;
			}
			if (!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), 1, null, false))
			{
				activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			if ( Rnd.get(0, 100)>50 )
			{
				int squashId=target.getTemplate().getIdTemplate();
				int newSquashId=0;
				switch (squashId)
				{
				case 12774:
					newSquashId = 12776;
					break;
				case 12776:
					newSquashId = 12775;
					break;
				case 12775:
					newSquashId = 13016;
					break;
				case 12777:
					newSquashId = 12779;
					break;
				case 12779:
					newSquashId = 12778;
					break;
				case 12778:
					newSquashId = 13017;
					break;
				}	
				if (newSquashId>0)
				{
					L2NpcTemplate squashTemplate = NpcTable.getInstance().getTemplate(newSquashId);
					L2SquashInstance squash = new L2SquashInstance(IdFactory.getInstance().getNextId(), squashTemplate,activeChar);
					squash.getStatus().setCurrentHpMp(1, 1);
					squash.getStatus().stopHpMpRegeneration();
					squash.setLevel(activeChar.getLevel());				
					squash.spawnMe(target.getX(),target.getY(),target.getZ());
					target.deleteMe();
					activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_SQUASH_SUCCESS_GROW));
				}
			}
		}
		else
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
	}
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}		
}