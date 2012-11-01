package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;

public class Harvester implements IItemHandler
{
	private static final int[]	ITEM_IDS	=
	{
		5125
	};

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par)
	{
	}

	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable.isPlayer()))
			return;

		if (CastleManorManager.getInstance().isDisabled())
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2Object target = activeChar.getTarget();

		if (target instanceof L2MonsterInstance && ((L2Character) target).isDead())
			activeChar.useMagic(SkillTable.getInstance().getInfo(2098, 1), false, false);
		else
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}