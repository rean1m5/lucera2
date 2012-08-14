package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.skills.AbnormalEffect;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public final class EffectMedusa extends L2Effect
{
	public EffectMedusa(Env env, EffectTemplate template)
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
		getEffected().startAbnormalEffect(AbnormalEffect.HOLD_2);
		getEffected().startParalyze();
		getEffected().setIsPetrified(true);
		return true;
	}

	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(AbnormalEffect.HOLD_2);
		getEffected().stopParalyze(this);
		getEffected().setIsPetrified(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
