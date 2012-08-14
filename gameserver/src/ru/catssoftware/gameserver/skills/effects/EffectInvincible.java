package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public final class EffectInvincible extends L2Effect
{
	public EffectInvincible(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.INVINCIBLE;
	}

	@Override
	public boolean onStart()
	{
		getEffected().setIsInvul(true);
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		getEffected().setIsInvul(true);
		return false;
	}

	@Override
	public void onExit()
	{
		getEffected().setIsInvul(false);
	}
}
