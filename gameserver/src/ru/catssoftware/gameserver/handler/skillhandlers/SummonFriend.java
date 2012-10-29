package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.model.restriction.AvailableRestriction;
import ru.catssoftware.gameserver.model.restriction.ObjectRestrictions;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ConfirmDlg;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.random.Rnd;

public class SummonFriend implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.SUMMON_FRIEND };

	public static boolean checkSummonerStatus(L2PcInstance summonerChar)
	{
		if (summonerChar == null)
			return false;

		if (summonerChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return false;
		}
		if (summonerChar.isFlying())
			return false;
		if (summonerChar.inObserverMode())
			return false;
		if (summonerChar.isInsideZone(L2Zone.FLAG_NOSUMMON))
		{
			summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
			return false;
		}
		if (summonerChar.getInstanceId() > 0)
		{
			Instance summonerInstance = InstanceManager.getInstance().getInstance(summonerChar.getInstanceId());
			if (summonerInstance != null && (!Config.ALLOW_SUMMON_TO_INSTANCE || !summonerInstance.isSummonAllowed()))
			{
				summonerChar.sendPacket(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
				return false;
			}
		}
		return true;
	}

	public static boolean checkTargetStatus(L2PcInstance targetChar, L2PcInstance summonerChar)
	{
		if (targetChar == null)
			return false;
		if (targetChar.isAlikeDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}
		if (targetChar.isInStoreMode())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}
		if (targetChar.isRooted() || targetChar.isInCombat())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}
		if (targetChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD);
			return false;
		}
		if (targetChar.inObserverMode())
		{
			summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
			return false;
		}
		if (targetChar.isFestivalParticipant())
		{
			summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
			return false;
		}
		if (summonerChar.isInsideZone(L2Zone.FLAG_NOSUMMON))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IN_SUMMON_BLOCKING_AREA);
			sm.addString(targetChar.getName());
			summonerChar.sendPacket(sm);
			return false;
		}
		
		if (ObjectRestrictions.getInstance().checkRestriction(targetChar, AvailableRestriction.PlayerSummonFriend))
		{
			summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
			return false;
		}
		if ((summonerChar.getGameEvent()!=null && summonerChar.getGameEvent().isRunning()) || (targetChar.getGameEvent()!=null && targetChar.getGameEvent().isRunning()))
		{
			summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
			return false;
		}
		if (summonerChar.isIn7sDungeon())
		{
			int targetCabal = SevenSigns.getInstance().getPlayerCabal(targetChar);
			if (SevenSigns.getInstance().isSealValidationPeriod())
			{
				if (targetCabal != SevenSigns.getInstance().getCabalHighestScore())
				{
					summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
					return false;
				}
			}
			else
			{
				if (targetCabal == SevenSigns.CABAL_NULL)
				{
					summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
					return false;
				}
			}
		}

		return true;
	}

	public static void teleToTarget(L2PcInstance targetChar, L2PcInstance summonerChar, L2Skill summonSkill)
	{
		if (targetChar == null || summonerChar == null || summonSkill == null)
			return;

		if (!checkSummonerStatus(summonerChar))
			return;
		if (!checkTargetStatus(targetChar, summonerChar))
			return;

		int itemConsumeId = summonSkill.getTargetConsumeId();
		int itemConsumeCount = summonSkill.getTargetConsume();
		if (itemConsumeId != 0 && itemConsumeCount != 0)
		{
			if (targetChar.getInventory().getInventoryItemCount(itemConsumeId, 0) < itemConsumeCount)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING);
				sm.addItemName(itemConsumeId);
				targetChar.sendPacket(sm);
				return;
			}
			targetChar.getInventory().destroyItemByItemId("Consume", itemConsumeId, itemConsumeCount, summonerChar, targetChar);
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
			sm.addItemName(itemConsumeId);
			targetChar.sendPacket(sm);
		}
		// set correct instance id
		targetChar.setInstanceId(summonerChar.getInstanceId());
		int x = summonerChar.getX();
		int y = summonerChar.getY();
		int n = 0;
		for(;;)  {
			x += Rnd.get((int)summonerChar.getTemplate().getdCollisionRadius()*-2, (int)summonerChar.getTemplate().getdCollisionRadius()*2);
			y += Rnd.get((int)summonerChar.getTemplate().getdCollisionRadius()*-2, (int)summonerChar.getTemplate().getdCollisionRadius()*2);
			if(++n > 5 || GeoData.getInstance().canMoveFromToTarget(summonerChar.getX(), summonerChar.getY(), summonerChar.getZ(), x, y, summonerChar.getZ(), summonerChar.getInstanceId()))
				break;
		}
		targetChar.teleToLocation(x, y, summonerChar.getZ(), false);
		
	}

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return; // currently not implemented for others

		L2PcInstance activePlayer = (L2PcInstance) activeChar;
		if (!checkSummonerStatus(activePlayer))
			return;

		for (L2Character element : targets)
		{
			if (!(element instanceof L2PcInstance))
				continue;

			if (activeChar == element)
				continue;

			L2PcInstance target = (L2PcInstance) element;

			if (!checkTargetStatus(target, activePlayer))
				continue;

			if (!Util.checkIfInRange(0, activeChar, target, false))
			{
				if (!target.teleportRequest(activePlayer, skill))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_SUMMONED);
					sm.addString(target.getName());
					activePlayer.sendPacket(sm);
					continue;
				}

				if (skill.getId() == 1403) //summon friend
				{
					// Send message
					ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
					confirm.addCharName(activeChar);
					confirm.addZoneName(activeChar.getX(), activeChar.getY(), activeChar.getZ());
					confirm.addTime(30000);
					confirm.addRequesterId(activePlayer.getObjectId());
					target.sendPacket(confirm);
				}
				else
				{
					teleToTarget(target, activePlayer, skill);
					target.teleportRequest(null, null);
				}
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
