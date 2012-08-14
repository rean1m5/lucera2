package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.ChanceCondition;
import ru.catssoftware.gameserver.model.ChanceSkillList;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

final class EffectStaticChance extends L2Effect
{
	public EffectStaticChance(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BUFF;
	}

	@Override
	public boolean onStart()
	{
		L2Skill skill=getSkill();
		if (skill != null)
		{
			L2Character cha = getEffected();
			if (cha!=null)
			{
				ChanceSkillList chList=cha.getChanceSkills();
				if (chList!=null)
				{
					ChanceCondition cc =chList.get(skill);
					if (cc != null)
						cc.isReady = false;
				}
			}
		}
		return true;
	}

	@Override
	public void onExit()
	{
		L2Skill skill=getSkill();
		if (skill != null)
		{
			L2Character cha = getEffected();
			if (cha!=null)
			{
				ChanceSkillList chList=cha.getChanceSkills();
				if (chList!=null)
				{
					ChanceCondition cc =chList.get(skill);
					if (cc != null)
						cc.isReady = true;
				}
			}
		}
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}