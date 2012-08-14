package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class Firework implements IItemHandler
{
	private static final int[]	ITEM_IDS	= { 6403, 6406, 6407 };
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;
		activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();
		int skillId = -1;

		if (!FloodProtector.tryPerformAction(activeChar, Protected.FIREWORK))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(item);
			activeChar.sendPacket(sm);
			return;
		}

		switch (itemId)
		{
			case 6403: // Elven Firecracker
				skillId = 2023; // elven_firecracker, xml: 2023
				break;
			case 6406: // Firework
				skillId = 2024; // firework, xml: 2024
				break;
			case 6407: // Large Firework
				skillId = 2025; // large_firework, xml: 2025
				break;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);
		MagicSkillUse MSU = new MagicSkillUse(playable, activeChar, skillId, 1, 1, 0, false);

		playable.destroyItem("Consume", item.getObjectId(), 1, null, false);

		activeChar.sendPacket(MSU);
		activeChar.broadcastPacket(MSU);

		if (skill != null)
			activeChar.useMagic(skill, false, false);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}