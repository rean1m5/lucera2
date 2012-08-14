package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.tools.random.Rnd;

public class Recall implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.RECALL };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (skill.getId() == 4216 || skill.getId() == 4217 || skill.getId() == 4222)
		{
			doZakenTeleport(targets);
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			if (activeChar instanceof L2MonsterInstance)
				((L2MonsterInstance) activeChar).clearAggroList();
			return;
		}

		if (activeChar instanceof L2PcInstance)
		{
			if (((L2PcInstance) activeChar).isInOlympiadMode())
			{
				activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
				return;
			}
		}

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			if (target instanceof L2PcInstance)
			{
				L2PcInstance targetChar = (L2PcInstance) target;
				if (targetChar.isInsideZone(L2Zone.FLAG_NOESCAPE))
				{
					targetChar.sendMessage(Message.getMessage(targetChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
					targetChar.sendPacket(ActionFailed.STATIC_PACKET);
					break;
				}
				if (targetChar.isFestivalParticipant())
				{
					targetChar.sendMessage(Message.getMessage(targetChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
					continue;
				}
				if (targetChar.getGameEvent()!=null && targetChar.getGameEvent().isRunning())
				{
					targetChar.sendMessage(Message.getMessage(targetChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
					continue;
				}
				if (targetChar.isInJail() || targetChar.isInsideZone(L2Zone.FLAG_JAIL))
				{
					targetChar.sendMessage(Message.getMessage(targetChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
					continue;
				}
				if (targetChar.isInDuel())
				{
					targetChar.sendMessage(Message.getMessage(targetChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
					continue;
				}
			}
			target.setInstanceId(0);
			target.teleToLocation(TeleportWhereType.Town);
		}
	}

	protected void doZakenTeleport(L2Character... targets)
	{
		final int loc[][] =
		{
			{ 54228, 220136, -3496 },
			{ 56315, 220127, -3496 },
			{ 56285, 218078, -3496 },
			{ 54238, 218066, -3496 },
			{ 55259, 219107, -3496 },
			{ 56295, 218078, -3224 },
			{ 56283, 220133, -3224 },
			{ 54241, 220127, -3224 },
			{ 54238, 218077, -3224 },
			{ 55268, 219090, -3224 },
			{ 56284, 218078, -2952 },
			{ 54252, 220135, -2952 },
			{ 54244, 218095, -2952 },
			{ 55270, 219086, -2952 }
		};

		int rndLoc = 0;
		int rndX = 0;
		int rndY = 0;

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			target.abortAttack();

			rndLoc = Rnd.get(14);
			rndX = Rnd.get(-400, 400);
			rndY = Rnd.get(-400, 400);

			target.teleToLocation(loc[rndLoc][0] + rndX, loc[rndLoc][1] + rndY, loc[rndLoc][2]);
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}