package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class ManaHeal implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.MANAHEAL, L2SkillType.MANARECHARGE, L2SkillType.MANAHEAL_PERCENT };

	public void useSkill(L2Character actChar, L2Skill skill, L2Character... targets)
	{
		SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF).useSkill(actChar, skill, targets);
		
		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			double mp = skill.getPower();
			if (skill.getSkillType() == L2SkillType.MANAHEAL_PERCENT)
				mp = target.getMaxMp() * mp / 100.0;
			else
				mp = (skill.getSkillType() == L2SkillType.MANARECHARGE) ? target.calcStat(Stats.RECHARGE_MP_RATE, mp, null, null) : mp;

			if (actChar.getLevel() != target.getLevel())
			{
				if (actChar.getLevel() + 3 >= target.getLevel())
					mp = mp * 1;
				else if (actChar.getLevel() + 5 <= target.getLevel())
					mp = mp * 0.6;
				else if (actChar.getLevel() + 7 <= target.getLevel())
					mp = mp * 0.4;
				else if (actChar.getLevel() + 9 <= target.getLevel())
					mp = mp * 0.3;
				else if (actChar.getLevel() + 10 <= target.getLevel())
					mp = mp * 0.1;
			}

			//from CT2 u will receive exact MP, u can't go over it, if u have full MP and u get MP buff, u will receive 0MP restored message
			if ((target.getStatus().getCurrentMp() + mp) >= target.getMaxMp())
				mp = target.getMaxMp() - target.getStatus().getCurrentMp();

			target.setLastHealAmount((int) mp);
			target.getStatus().setCurrentMp(mp + target.getStatus().getCurrentMp());
			StatusUpdate sump = new StatusUpdate(target.getObjectId());
			sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getStatus().getCurrentMp());
			target.sendPacket(sump);

			if (actChar.isPlayer() && actChar != target)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S2_MP_RESTORED_BY_S1);
				sm.addString(actChar.getName());
				sm.addNumber((int) mp);
				target.sendPacket(sm);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_MP_RESTORED);
				sm.addNumber((int) mp);
				target.sendPacket(sm);
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}