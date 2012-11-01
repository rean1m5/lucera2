package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class Remedy implements IItemHandler
{
	private static final int[]	ITEM_IDS	=
											{ 1831, 1832, 1833, 1834, 3889 };
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

		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		int itemId = item.getItemId();
		if (itemId == 1831) // antidote
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for (L2Effect e : effects)
			{
				if (e.getSkill().getSkillType() == L2SkillType.POISON && e.getSkill().getLevel() <= 3)
				{
					e.exit();
					break;
				}
			}
			MagicSkillUse MSU = new MagicSkillUse(playable, playable, 2042, 1, 0, 0, false);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if (itemId == 1832) // advanced antidote
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for (L2Effect e : effects)
			{
				if (e.getSkill().getSkillType() == L2SkillType.POISON && e.getSkill().getLevel() <= 7)
				{
					e.exit();
					break;
				}
			}
			MagicSkillUse MSU = new MagicSkillUse(playable, playable, 2043, 1, 0, 0, false);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if (itemId == 1833) // bandage
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for (L2Effect e : effects)
			{
				if (e.getSkill().getSkillType() == L2SkillType.BLEED && e.getSkill().getLevel() <= 3)
				{
					e.exit();
					break;
				}
			}
			MagicSkillUse MSU = new MagicSkillUse(playable, playable, 34, 1, 0, 0, false);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if (itemId == 1834) // emergency dressing
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for (L2Effect e : effects)
			{
				if (e.getSkill().getSkillType() == L2SkillType.BLEED && e.getSkill().getLevel() <= 7)
				{
					e.exit();
					break;
				}
			}
			MagicSkillUse MSU = new MagicSkillUse(playable, playable, 2045, 1, 0, 0, false);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if (itemId == 3889) // potion of recovery
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for (L2Effect e : effects)
			{
				if (e.getSkill().getId() == 4082)
					e.exit();
			}
			activeChar.setIsImmobilized(false);
			if (activeChar.getFirstEffect(L2EffectType.ROOT) == null)
				activeChar.stopRooting(null);
			MagicSkillUse MSU = new MagicSkillUse(playable, playable, 2042, 1, 0, 0, false);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(itemId);
		activeChar.sendPacket(sm);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}