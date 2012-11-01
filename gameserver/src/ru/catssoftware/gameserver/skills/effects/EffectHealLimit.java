package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public final class EffectHealLimit extends L2Effect
{
	public EffectHealLimit(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.DEBUFF;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected().isPlayer())
			getEffected().setHealLimit((int)calc());
		return true;
	}
	
	@Override
	public void onExit()
	{
		getEffected().setHealLimit(0);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
