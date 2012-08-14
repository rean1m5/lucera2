package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

final class EffectUpSkillLevel extends L2Effect
{
	public EffectUpSkillLevel(Env env, EffectTemplate template)
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
		L2Skill tmpSkill = SkillTable.getInstance().getInfo(getSkill().getId(), getSkill().getLevel()+1);
		if (tmpSkill != null)
				getEffected().addSkill(tmpSkill);
		return true;
	}

	@Override
	public void onExit()
	{
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}