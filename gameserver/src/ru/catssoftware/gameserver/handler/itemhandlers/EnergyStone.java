package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class EnergyStone implements IItemHandler
{
	private static final int[]	ITEM_IDS	= { 5589 };
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;
		if (playable.isPlayer())
			activeChar = (L2PcInstance) playable;
		else if (playable instanceof L2PetInstance)
			activeChar = ((L2PetInstance) playable).getOwner();
		else
			return;

		if (activeChar.isAllSkillsDisabled())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return;
		}

		L2Skill skill = activeChar.getChargeSkill();
		if (skill == null)
		{
			// Player is not a charger class
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(5589);
			activeChar.sendPacket(sm);
		}
		else
		{
			activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
			L2Skill energy = SkillTable.getInstance().getInfo(2165, 1);
			activeChar.doCast(energy);
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}