package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.FlyToLocation;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.network.serverpackets.FlyToLocation.FlyType;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;

public class InstantJump implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.INSTANT_JUMP };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (targets.length == 0 || targets[0] == null)
			return;

		if (activeChar.isRooted())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
			activeChar.sendPacket(sm);
			return;
		}

		L2Character target = targets[0];

		int x = 0, y = 0, z = 0;

		int px = target.getX();
		int py = target.getY();
		double ph = Util.convertHeadingToDegree(target.getHeading());

		ph += 180;

		if (ph > 360)
			ph -= 360;

		ph = (Math.PI * ph) / 180;

		x = (int) (px + (25 * Math.cos(ph)));
		y = (int) (py + (25 * Math.sin(ph)));
		z = target.getZ();

		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		activeChar.broadcastPacket(new FlyToLocation(activeChar, x, y, z, FlyType.DUMMY));
		activeChar.abortAttack();
		activeChar.abortCast();

		activeChar.getPosition().setXYZ(x, y, z);
		activeChar.broadcastPacket(new ValidateLocation(activeChar));

		if (skill.hasEffects())
		{
			if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
			{
				activeChar.stopSkillEffects(skill.getId());
				skill.getEffects(target, activeChar);
			}
			else
			{
				// activate attacked effects, if any
				target.stopSkillEffects(skill.getId());
				if (Formulas.calcSkillSuccess(activeChar, target, skill, false, false, false))
					skill.getEffects(activeChar, target);
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
