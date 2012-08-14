package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Boss;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/*
 * @author m095
 */

public final class EffectBlessingOfEva extends L2Effect
{

	public EffectBlessingOfEva(Env env, EffectTemplate template)
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
		if (!(getEffected() instanceof L2Boss))
		{
			getEffected().getStatus().setCurrentHp(getEffected().getMaxHp());
			getEffected().getStatus().setCurrentCp(getEffected().getMaxCp());
			getEffected().getStatus().setCurrentMp(getEffected().getMaxMp());
		}
		else
		{
			getEffector().getStatus().setCurrentHp(getEffector().getMaxHp());
			getEffector().getStatus().setCurrentCp(getEffector().getMaxCp());
			getEffector().getStatus().setCurrentMp(getEffector().getMaxMp());
		}
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
