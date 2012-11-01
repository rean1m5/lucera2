package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.RainbowSpringSiege;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.restriction.AvailableRestriction;
import ru.catssoftware.gameserver.model.restriction.ObjectRestrictions;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SetupGauge;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class ScrollOfEscape implements IItemHandler
{
	// all the items IDs that this handler knows
	private static final int[]	ITEM_IDS	=
	{
			736,
			1830,
			1829,
			1538,
			3958,
			5858,
			5859,
			6663, // Scroll of Escape: Orc Village
			6664, // Scroll of Escape: Silenos Village
			7117,
			7118,
			7119,
			7120,
			7121,
			7122,
			7123,
			7124,
			7125,
			7126,
			7127,
			7128,
			7129,
			7130,
			7131,
			7132,
			7133,
			7134,
			7135,
			7554,
			7555,
			7556,
			7557,
			7558,
			7559,
			7618,
			7619,
			9156 // Blessed Scroll of Escape (Event)
	};

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par)
	{
		// none
	}

	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable.isPlayer()))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;

		if (ObjectRestrictions.getInstance().checkRestriction(activeChar, AvailableRestriction.PlayerScrollTeleport))
		{
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_USE),"SOE"));
			return;
		}

		if (checkConditions(activeChar))
			return;
		
		if (activeChar.isInsideZone(L2Zone.FLAG_NOESCAPE))
		{
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_USE),"SOE"));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return;
		}
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}
		if (activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_USE),"SOE"));
			return;
		}
		if(RainbowSpringSiege.getInstance().isPlayerInArena(activeChar))
		{
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_USE),"SOE"));
			return;
		}
			
		if ((activeChar.getGameEvent()!=null && activeChar.getGameEvent().isRunning()) && !activeChar.getGameEvent().canLogout(activeChar) )
		{
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_USE),"SOE"));
			return;
		}
		if (activeChar.isInJail() || activeChar.isInsideZone(L2Zone.FLAG_JAIL))
		{
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_USE),"SOE"));
			return;
		}
		if (activeChar.inObserverMode())
		{
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_USE),"SOE"));
			return;
		}
		if (activeChar.isInDuel())
		{
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_USE),"SOE"));
			return;
		}

		boolean ret = false;
		switch(item.getItemId())
		{
			case 5859:
				if (activeChar.getClan() != null && (CastleManager.getInstance().getCastleByOwner(activeChar.getClan()) == null && FortManager.getInstance().getFortByOwner(activeChar.getClan()) == null))
					ret = true;
				break;
			case 5858:
				ret = true;
				if (activeChar.getClan() != null) 
					if(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()) != null || FortManager.getInstance().getFortByOwner(activeChar.getClan())!=null)
						ret = false;
				break;
		}		
		if (ret)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(item);
			activeChar.sendPacket(sm);
			return;
		}
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		int itemId = item.getItemId();
		int skillId = 0;
		int skillLvl = 0;
		
		String[] skillsSQL = item.getEtcItem().getSkills();
		if (skillsSQL != null)
		{
			for (String skillInfo : skillsSQL)
			{
				String[] skill = skillInfo.split("-");
				if (skill != null && skill.length == 2)
				{
					skillId = Integer.parseInt(skill[0]);
					skillLvl = Integer.parseInt(skill[1]);
				}
			}
		}
		
		if (skillId == 0 || skillLvl == 0)
		{
			activeChar.sendMessage("Item is not found in data base.");
			return;
		}
		
		if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
			return;

		activeChar.disableAllSkills();
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
		MagicSkillUse msu = new MagicSkillUse(activeChar, activeChar, skillId, skillLvl, skill.getHitTime(), 0, false);
		activeChar.broadcastPacket(msu);
		SetupGauge sg = new SetupGauge(0, skill.getHitTime());
		activeChar.sendPacket(sg);

		SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(item);
		activeChar.sendPacket(sm);

		EscapeFinalizer ef = new EscapeFinalizer(activeChar, itemId);
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(ef, skill.getHitTime()));
		activeChar.forceIsCasting(GameTimeController.getGameTicks() + skill.getHitTime() / GameTimeController.MILLIS_IN_TICK);
	}

	static class EscapeFinalizer implements Runnable
	{
		private L2PcInstance _activeChar;
		private int _itemId;

		EscapeFinalizer(L2PcInstance activeChar, int itemId)
		{
			_activeChar = activeChar;
			_itemId = itemId;
		}

		public void run()
		{
			if (_activeChar.isDead())
				return;
			_activeChar.enableAllSkills();
			_activeChar.setIsCastingNow(false);
			_activeChar.setIsIn7sDungeon(false);
			if (_activeChar.getInstanceId()>0)
			{
				Instance inst=InstanceManager.getInstance().getInstance(_activeChar.getInstanceId());
				if (inst!=null)
				{
					inst.ejectPlayer(_activeChar.getObjectId());
					return;
				}
			}			
			_activeChar.setInstanceId(0);
			
			try
			{
				switch (_itemId)
				{
					case 1830:
					case 5859:
						if (_activeChar.getClan() != null && CastleManager.getInstance().getCastleByOwner(_activeChar.getClan()) != null)
							_activeChar.teleToLocation(TeleportWhereType.Castle);
						else 
							_activeChar.teleToLocation(TeleportWhereType.Town);
						break;
					case 1829:
					case 5858:
						if (_activeChar.getClan() != null && FortManager.getInstance().getFortByOwner(_activeChar.getClan()) != null)
							_activeChar.teleToLocation(TeleportWhereType.Fortress);
						else if (_activeChar.getClan() != null && ClanHallManager.getInstance().getClanHallByOwner(_activeChar.getClan()) != null)
							_activeChar.teleToLocation(TeleportWhereType.ClanHall);
						else 
							_activeChar.teleToLocation(TeleportWhereType.Town);
						break;
					case 7117:
					case 7554:
						_activeChar.teleToLocation(-84318, 244579, -3730, true); // Talking Island
						break;
					case 7118:
					case 7555:
						_activeChar.teleToLocation(46934, 51467, -2977, true); // Elven Village
						break;
					case 7119:
					case 7556:
						_activeChar.teleToLocation(9745, 15606, -4574, true); // Dark Elven Village
						break;
					case 7120:
					case 7557:
						_activeChar.teleToLocation(-44836, -112524, -235, true); // Orc Village
						break;
					case 7121:
					case 7558:
						_activeChar.teleToLocation(115113, -178212, -901, true); // Dwarven Village
						break;
					case 7122:
						_activeChar.teleToLocation(-80826, 149775, -3043, true); // Gludin Village
						break;
					case 7123:
						_activeChar.teleToLocation(-12678, 122776, -3116, true); // Gludio Castle Town
						break;
					case 7124:
						_activeChar.teleToLocation(15670, 142983, -2705, true); // Dion Castle Town
						break;
					case 7125:
						_activeChar.teleToLocation(17836, 170178, -3507, true); // Floran
						break;
					case 7126:
					case 7559:
						_activeChar.teleToLocation(83400, 147943, -3404, true); // Giran Castle Town
						break;
					case 7127:
						_activeChar.teleToLocation(105918, 109759, -3207, true); // Hardin's Private Academy
						break;
					case 7128:
						_activeChar.teleToLocation(111409, 219364, -3545, true); // Heine
						break;
					case 7129:
						_activeChar.teleToLocation(82956, 53162, -1495, true); // Oren Castle Town
						break;
					case 7130:
						_activeChar.teleToLocation(85348, 16142, -3699, true); // Ivory Tower
						break;
					case 7131:
						_activeChar.teleToLocation(116819, 76994, -2714, true); // Hunters Village
						break;
					case 7132:
						_activeChar.teleToLocation(146331, 25762, -2018, true); // Aden Castle Town
						break;
					case 7133:
						_activeChar.teleToLocation(147928, -55273, -2734, true); // Goddard Castle Town
						break;
					case 7134:
						_activeChar.teleToLocation(43799, -47727, -798, true); // Rune Castle Town
						break;
					case 7135:
						_activeChar.teleToLocation(87331, -142842, -1317, true); // Schuttgart Castle Town
						break;
					case 6663:
					case 7618:
						_activeChar.teleToLocation(149864, -81062, -5618, true); // Ketra Orc Village
						break;
					case 6664:
					case 7619:
						_activeChar.teleToLocation(108275, -53785, -2524, true); // Varka Silenos Village
						break;
					default:
						_activeChar.teleToLocation(TeleportWhereType.Town);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static boolean checkConditions(L2PcInstance actor)
	{
		return actor.isStunned() || actor.isSleeping() || actor.isParalyzed() || actor.isFakeDeath() || actor.isTeleporting()
		|| actor.isAlikeDead() || actor.isAllSkillsDisabled() || actor.isCastingNow() || actor.isRooted();
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}