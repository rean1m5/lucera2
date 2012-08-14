package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/*
 * Author: M-095
 */

public final class EffectSublime extends L2Effect
{
	public EffectSublime(Env env, EffectTemplate template)
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
		getEffector().reduceCurrentHp(getEffector().getMaxHp() + 1, getEffector());
		if (getEffected() != getEffector())
			getEffected().setIsInvul(true);

		return true;
	}

	@Override
	public void onExit()
	{
		getEffected().setIsInvul(false);
	}

	@Override
	public boolean onActionTime()
	{
		if (getEffected() != getEffector())
			getEffected().setIsInvul(true);

		return false;
	}
}