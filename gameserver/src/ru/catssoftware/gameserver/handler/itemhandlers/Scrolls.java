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
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;

/**
* This class ...
*
* @version $Revision: 1.1.6.4 $ $Date: 2005/04/06 18:25:18 $
*/
public class Scrolls implements IItemHandler
{
	private static final int[]	ITEM_IDS	=
	{
			3926,
			3927,
			3928,
			3929,
			3930,
			3931,
			3932,
			3933,
			3934,
			3935,
			4218,
			5593,
			5594,
			5595,
			6037,
			5703,
			5803,
			5804,
			5805,
			5806,
			5807, // lucky charm
			8515,
			8516,
			8517,
			8518,
			8519,
			8520, // charm of courage
			8594,
			8595,
			8596,
			8597,
			8598,
			8599, // scrolls of recovery
			8954,
			8955,
			8956, // primeval crystal
			9146,
			9147,
			9148,
			9149,
			9150,
			9151,
			9152,
			9153,
			9154,
			9155
	};

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par)
	{
	}

	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;
		if (playable instanceof L2PcInstance)
			activeChar = (L2PcInstance) playable;
		else if (playable instanceof L2PetInstance)
			activeChar = ((L2PetInstance) playable).getOwner();
		else
			return;

		if (activeChar.isOutOfControl() ||  activeChar.isAllSkillsDisabled() || activeChar.isCastingNow())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		int itemId = item.getItemId();

		if (itemId >= 8594 && itemId <= 8599) //Scrolls of recovery XML: 2286
		{
			if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
				return;
			activeChar.broadcastPacket(new MagicSkillUse(playable, playable, 2286, 1, 1, 0, false));
			activeChar.reduceDeathPenaltyBuffLevel();
			useScroll(activeChar, 2286, itemId - 8593);
			return;
		}
		else if (itemId == 5703 || itemId >= 5803 && itemId <= 5807) // Lucky Charm
		{
			byte expIndex = (byte) activeChar.getExpertiseIndex();
			if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
				return;
			activeChar.broadcastPacket(new MagicSkillUse(playable, playable, 2168, (expIndex > 5 ? expIndex : expIndex + 1), 1, 0, false));
			useScroll(activeChar, 2168, (expIndex > 5 ? expIndex : expIndex + 1));
			activeChar.setCharmOfLuck(true);
			return;
		}
		else if (itemId >= 8515 && itemId <= 8520) // Charm of Courage XML: 5041
		{
			if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
				return;
			activeChar.broadcastPacket(new MagicSkillUse(playable, playable, 5041, 1, 1, 0, false));
			useScroll(activeChar, 5041, 1);
			return;
		}
		else if (itemId >= 8954 && itemId <= 8956)
		{
			if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
				return;
			switch (itemId)
			{
				case 8954: // Blue Primeval Crystal XML: 2306;1
				case 8955: // Green Primeval Crystal XML: 2306;2
				case 8956: // Red Primeval Crystal XML: 2306;3
					useScroll(activeChar, 2306, itemId - 8953);
					break;
				default:
					break;
			}
			return;
		}

		// for the rest, there are no extra conditions
		if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
			return;

		switch (itemId)
		{
			case 3926: // Scroll of Guidance XML:2050
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2050, 1, 1, 0, false));
				useScroll(activeChar, 2050, 1);
				break;
			case 3927: // Scroll of Death Whisper XML:2051
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2051, 1, 1, 0, false));
				useScroll(activeChar, 2051, 1);
				break;
			case 3928: // Scroll of Focus XML:2052
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2052, 1, 1, 0, false));
				useScroll(activeChar, 2052, 1);
				break;
			case 3929: // Scroll of Greater Acumen XML:2053
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2053, 1, 1, 0, false));
				useScroll(activeChar, 2053, 1);
				break;
			case 3930: // Scroll of Haste XML:2054
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2054, 1, 1, 0, false));
				useScroll(activeChar, 2054, 1);
				break;
			case 3931: // Scroll of Agility XML:2055
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2055, 1, 1, 0, false));
				useScroll(activeChar, 2055, 1);
				break;
			case 3932: // Scroll of Mystic Empower XML:2056
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2056, 1, 1, 0, false));
				useScroll(activeChar, 2056, 1);
				break;
			case 3933: // Scroll of Might XML:2057
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2057, 1, 1, 0, false));
				useScroll(activeChar, 2057, 1);
				break;
			case 3934: // Scroll of Wind Walk XML:2058
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2058, 1, 1, 0, false));
				useScroll(activeChar, 2058, 1);
				break;
			case 3935: // Scroll of Shield XML:2059
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2059, 1, 1, 0, false));
				useScroll(activeChar, 2059, 1);
				break;
			case 4218: // Scroll of Mana Regeneration XML:2064
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2064, 1, 1, 0, false));
				useScroll(activeChar, 2064, 1);
				break;
			case 5593: // SP Scroll Low Grade XML:2167;1
			case 5594: // SP Scroll Medium Grade XML:2167;2
			case 5595: // SP Scroll High Grade XML:2167;3
				useScroll(activeChar, 2167, itemId - 5592);
				break;
			case 6037: // Scroll of Waking XML:2170
				activeChar.broadcastPacket(new MagicSkillUse(playable, playable, 2170, 1, 1, 0, false));
				useScroll(activeChar, 2170, 1);
				break;
			case 9146: // Scroll of Guidance - For Event XML:2050
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2050, 1, 1, 0, false));
				useScroll(activeChar, 2050, 1);
				break;
			case 9147: // Scroll of Death Whisper - For Event XML:2051
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2051, 1, 1, 0, false));
				useScroll(activeChar, 2051, 1);
				break;
			case 9148: // Scroll of Focus - For Event XML:2052
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2052, 1, 1, 0, false));
				useScroll(activeChar, 2052, 1);
				break;
			case 9149: // Scroll of Acumen - For Event XML:2053
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2053, 1, 1, 0, false));
				useScroll(activeChar, 2053, 1);
				break;
			case 9150: // Scroll of Haste - For Event XML:2054
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2054, 1, 1, 0, false));
				useScroll(activeChar, 2054, 1);
				break;
			case 9151: // Scroll of Agility - For Event XML:2055
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2055, 1, 1, 0, false));
				useScroll(activeChar, 2055, 1);
				break;
			case 9152: // Scroll of Empower - For Event XML:2056
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2056, 1, 1, 0, false));
				useScroll(activeChar, 2056, 1);
				break;
			case 9153: // Scroll of Might - For Event XML:2057
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2057, 1, 1, 0, false));
				useScroll(activeChar, 2057, 1);
				break;
			case 9154: // Scroll of Wind Walk - For Event XML:2058
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2058, 1, 1, 0, false));
				useScroll(activeChar, 2058, 1);
				break;
			case 9155: // Scroll of Shield - For Event XML:2059
				activeChar.broadcastPacket(new MagicSkillUse(playable, activeChar, 2059, 1, 1, 0, false));
				useScroll(activeChar, 2059, 1);
				break;
			default:
				break;
		}
	}

	public void useScroll(L2PcInstance activeChar, int magicId, int level)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(magicId, level);
		if (skill != null)
			activeChar.useMagic(skill, true, false);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}