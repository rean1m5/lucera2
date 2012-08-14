package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Skill.SkillTargetType;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public final class EffectHellboundDam extends L2Effect
{
	public EffectHellboundDam(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.DMG_OVER_TIME;
	}

	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
			return false;

		if (getEffected().getFirstEffect(2341) != null)
			return true;

		boolean awake = !(getEffected() instanceof L2Attackable) && !(getSkill().getTargetType() == SkillTargetType.TARGET_SELF && getSkill().isToggle());
		getEffected().reduceCurrentHp(200, getEffector(), awake, true, getSkill());
		return true;
	}
}