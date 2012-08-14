package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExAutoSoulShot;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.util.Broadcast;

public class BlessedSpiritShot implements IItemHandler
{
	// all the items ids that this handler knowns
	private static final int[]	ITEM_IDS	= { 3947, 3948, 3949, 3950, 3951, 3952 };
	private static final int[]	SKILL_IDS	= { 2061, 2160, 2161, 2162, 2163, 2164 };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean animation)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		int itemId = item.getItemId();

		if (activeChar.isInOlympiadMode() && !Config.ALT_OLY_ALLOW_BSS)
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		// Check if Blessed Spiritshot can be used
		if (weaponInst == null || weaponItem.getSpiritShotCount() == 0)
		{
			if (!activeChar.getAutoSoulShot().containsKey(itemId))
				activeChar.sendPacket(SystemMessageId.CANNOT_USE_SPIRITSHOTS);
			return;
		}

		// Check if Blessed Spiritshot is already active (it can be charged over Spiritshot)
		if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
			return;

		// Check for correct grade
		int weaponGrade = weaponItem.getCrystalType();
		if ((weaponGrade == L2Item.CRYSTAL_NONE && itemId != 3947) || (weaponGrade == L2Item.CRYSTAL_D && itemId != 3948)
				|| (weaponGrade == L2Item.CRYSTAL_C && itemId != 3949) || (weaponGrade == L2Item.CRYSTAL_B && itemId != 3950)
				|| (weaponGrade == L2Item.CRYSTAL_A && itemId != 3951) || (weaponGrade == L2Item.CRYSTAL_S && itemId != 3952))
		{
			if (!activeChar.getAutoSoulShot().containsKey(itemId))
				activeChar.sendPacket(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH);
			return;
		}

		if (Config.CONSUME_SPIRIT_SOUL_SHOTS)
		{
			if (!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), weaponItem.getSpiritShotCount(), null, false))
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
					activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SPIRITSHOTS);
				return;
			}
		}

		// Charge Blessed Spiritshot
		weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
		if (animation)
		{
			// Send message to client
			activeChar.sendPacket(SystemMessageId.ENABLED_SPIRITSHOT);
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