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

public class SpiritShot implements IItemHandler
{
	// All the item IDs that this handler knows.
	private static final int[]	ITEM_IDS	= { 5790, 2509, 2510, 2511, 2512, 2513, 2514 };
	private static final int[]	SKILL_IDS	= { 2061, 2155, 2156, 2157, 2158, 2159 };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean animation)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		int itemId = item.getItemId();

		// Check if Spiritshot can be used
		if (weaponInst == null || weaponItem.getSpiritShotCount() == 0)
		{
			if (!activeChar.getAutoSoulShot().containsKey(itemId))
				activeChar.sendPacket(SystemMessageId.CANNOT_USE_SPIRITSHOTS);
			return;
		}

		// Check if Spiritshot is already active
		if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
			return;

		// Check for correct grade
		int weaponGrade = weaponItem.getCrystalType();
		if ((weaponGrade == L2Item.CRYSTAL_NONE && itemId != 5790 && itemId != 2509) || (weaponGrade == L2Item.CRYSTAL_D && itemId != 2510)
				|| (weaponGrade == L2Item.CRYSTAL_C && itemId != 2511) || (weaponGrade == L2Item.CRYSTAL_B && itemId != 2512)
				|| (weaponGrade == L2Item.CRYSTAL_A && itemId != 2513) || (weaponGrade == L2Item.CRYSTAL_S && itemId != 2514))
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

		// Charge Spiritshot
		weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);
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