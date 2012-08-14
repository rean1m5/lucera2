package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public final class EffectChanceSkillTrigger extends L2Effect
{
	public EffectChanceSkillTrigger(Env env, EffectTemplate template)
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
		L2Skill skill = SkillTable.getInstance().getInfo(_triggeredId,_triggeredLevel);
		if (skill != null)
			getEffected().addChanceEffect(skill);

		return super.onStart();
	}

	@Override
	public void onExit()
	{
		getEffected().onExitChanceEffect();
		getEffected().removeChanceEffect(_triggeredId);
		super.onExit();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}