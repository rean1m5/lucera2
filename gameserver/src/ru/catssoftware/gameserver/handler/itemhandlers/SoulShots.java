package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExAutoSoulShot;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.util.Broadcast;

public class SoulShots implements IItemHandler
{
	private static final int[]	ITEM_IDS	= { 5789, 1835, 1463, 1464, 1465, 1466, 1467 };
	private static final int[]	SKILL_IDS	= { 2039, 2150, 2151, 2152, 2153, 2154 };


	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean animation)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		int itemId = item.getItemId();

		// Check if Soulshot can be used
		if (weaponInst == null || weaponItem.getSoulShotCount() == 0)
		{
			if (!activeChar.getAutoSoulShot().containsKey(itemId))
				activeChar.sendPacket(SystemMessageId.CANNOT_USE_SOULSHOTS);
			return;
		}

		// Check if Soulshot is already active
		if (weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE) 
			return;

		// Check for correct grade
		int weaponGrade = weaponItem.getCrystalType();
		if ((weaponGrade == L2Item.CRYSTAL_NONE && itemId != 5789 && itemId != 1835) || (weaponGrade == L2Item.CRYSTAL_D && itemId != 1463)
				|| (weaponGrade == L2Item.CRYSTAL_C && itemId != 1464) || (weaponGrade == L2Item.CRYSTAL_B && itemId != 1465)
				|| (weaponGrade == L2Item.CRYSTAL_A && itemId != 1466) || (weaponGrade == L2Item.CRYSTAL_S && itemId != 1467))
		{
			if (!activeChar.getAutoSoulShot().containsKey(itemId))
				activeChar.sendPacket(SystemMessageId.SOULSHOTS_GRADE_MISMATCH);
			return;
		}
		// Consume Soulshots if player has enough of them
		int saSSCount = (int) activeChar.getStat().calcStat(Stats.SOULSHOT_COUNT, 0, null, null);
		int SSCount = saSSCount == 0 ? weaponItem.getSoulShotCount() : saSSCount;

		if (Config.CONSUME_SPIRIT_SOUL_SHOTS)
		{
			if (!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), SSCount, null, false))
			{
				if (activeChar.getAutoSoulShot().containsKey(itemId))
				{
					activeChar.removeAutoSoulShot(itemId);
					activeChar.sendPacket(new ExAutoSoulShot(itemId, 0));

					SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
					sm.addString(item.getItem().getName());
					activeChar.sendPacket(sm);
				}
				else
					activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS);
				return;
			}
		}

		if (saSSCount > 0)
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MISER_CONSUME),saSSCount));

		// Charge soulshot
		weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT,false);
		if (animation)
		{
			// Send message to client
			activeChar.sendPacket(SystemMessageId.ENABLED_SOULSHOT);
			Broadcast.toSelfAndKnownPlayersInRadius(activeChar, new MagicSkillUse(activeChar, activeChar, SKILL_IDS[weaponGrade], 1, 0, 0, false), 360000);
		}
	}

	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		useItem(playable,item,true);
	}
 
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}