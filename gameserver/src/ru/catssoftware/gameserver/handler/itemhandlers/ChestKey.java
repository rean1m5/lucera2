package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2ChestInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;

public class ChestKey implements IItemHandler
{
	public static final int		INTERACTION_DISTANCE	= 100;

	private static final int[]	ITEM_IDS				= { 6665, 6666, 6667, 6668, 6669, 6670, 6671, 6672 };
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();
		L2Skill skill = SkillTable.getInstance().getInfo(2229, itemId - 6664);//box key skill
		L2Object target = activeChar.getTarget();

		if (!(target instanceof L2ChestInstance))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			L2ChestInstance chest = (L2ChestInstance) target;
			if (chest.isDead() || chest.isInteracted())
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			activeChar.useMagic(skill, false, false);
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}