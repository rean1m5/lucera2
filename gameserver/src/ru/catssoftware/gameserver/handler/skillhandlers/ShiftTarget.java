package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class ShiftTarget implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	=
													{ L2SkillType.SHIFT_TARGET };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		L2Attackable attackerChar = null;
		L2NpcInstance attacker = null;
		L2PcInstance targetChar = null;

		boolean targetShifted = false;

		for (L2Object target : targets)
		{
			if (target.isPlayer())
			{
				targetChar = (L2PcInstance) target;
				break;
			}
		}

		for (L2Object nearby : activeChar.getKnownList().getKnownCharactersInRadius(skill.getSkillRadius()))
		{
			if (!targetShifted)
			{
				if (nearby instanceof L2Attackable)
				{
					attackerChar = (L2Attackable) nearby;
					targetShifted = true;
					break;
				}
			}
		}

		if (targetShifted && attackerChar != null && targetChar != null)
		{
			attacker = attackerChar;
			int aggro = attackerChar.getHating(activeChar);

			if (aggro == 0)
			{
				if (targetChar.isRunning())
					attacker.setRunning();
				attackerChar.addDamageHate(targetChar, 0, 1);
				attacker.setTarget(targetChar);
				attackerChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, targetChar);
			}
			else
			{
				attackerChar.stopHating(activeChar);
				if (targetChar.isRunning())
					attacker.setRunning();
				attackerChar.addDamageHate(targetChar, 0, aggro);
				attacker.setTarget(targetChar);
				attackerChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, targetChar);
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}