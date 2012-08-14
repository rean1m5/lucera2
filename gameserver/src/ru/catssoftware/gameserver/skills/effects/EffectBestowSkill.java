package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

final class EffectBestowSkill extends L2Effect
{
	public EffectBestowSkill(Env env, EffectTemplate template)
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
		L2Skill tempSkill = SkillTable.getInstance().getInfo(getSkill().getTriggeredId(), getSkill().getTriggeredLevel());
		if (tempSkill != null)
		{
			getEffected().addSkill(tempSkill);
			return true;
		}
		return false;
	}

	@Override
	public void onExit()
	{
		getEffected().removeSkill(getSkill().getTriggeredId());
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}