package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Skill.SkillTargetType;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.taskmanager.DecayTaskManager;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import javolution.util.FastList;


public class Resurrect implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.RESURRECT };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		L2PcInstance targetPlayer;
		FastList<L2Character> targetToRes = new FastList<L2Character>();

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			if(Math.abs(target.getZ()-activeChar.getZ())>300)
				continue;
	
			if (target.isPlayer())
			{

				if(target.getPlayer().isInOlympiadMode())
					continue;
				// Check for same party or for same clan, if target is for clan.
				if (skill.getTargetType() == SkillTargetType.TARGET_CORPSE_CLAN)
				{
					if (activeChar.getPlayer() != null && activeChar.getPlayer().getClanId() != target.getPlayer().getClanId())
						continue;
				}
				
			}

			targetToRes.add(target);
		}

		for (L2Character cha : targetToRes)
		{
			if (activeChar.isPlayer())
			{
				if (cha.isPlayer())
				{
					cha.getPlayer().reviveRequest(activeChar.getPlayer(), skill);
				}
				else if (cha instanceof L2PetInstance)
				{
					if (((L2PetInstance) cha).getOwner() == activeChar)
						cha.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getStat().getWIT()));
					else
						((L2PetInstance) cha).getOwner().revivePetRequest((L2PcInstance) activeChar, skill);
				}
				else
					cha.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getStat().getWIT()));
			}
			else
			{
				DecayTaskManager.getInstance().cancelDecayTask(cha);
				cha.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getStat().getWIT()));
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}