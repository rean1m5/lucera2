package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.skills.AbnormalEffect;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 * @author LBaldi
 */
public final class EffectVitalityFlame extends L2Effect
{

	public EffectVitalityFlame(Env env, EffectTemplate template)
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
		getEffected().startAbnormalEffect(AbnormalEffect.VITALITY);
		return true;
	}

	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(AbnormalEffect.VITALITY);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}