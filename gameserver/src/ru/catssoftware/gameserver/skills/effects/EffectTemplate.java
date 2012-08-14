package ru.catssoftware.gameserver.skills.effects;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.conditions.Condition;
import ru.catssoftware.gameserver.skills.funcs.FuncTemplate;

public final class EffectTemplate
{
	private final Constructor<?> _constructor;
	private final Constructor<?> _stolenConstructor;
	private final Condition _attachCond;

	public final double lambda;
	public final int count;
	public final int period;
	public final int iconId;
	public final int abnormalEffect;
	public final String stackType;
	public final float stackOrder;
	public final boolean showIcon;
	public final int triggeredId;
    public final int triggeredLevel;

	public FuncTemplate[] funcTemplates;

	public EffectTemplate(Condition pAttachCond, String name, double pLambda, int pCount, int pPeriod, int pAbnormalEffect, String pStackType, float pStackOrder, boolean pShowIcon, int trigId, int trigLvl, int iconId)
	{
		_attachCond = pAttachCond;

		lambda = pLambda;
		count = pCount;
		period = pPeriod;
		abnormalEffect = pAbnormalEffect;
		stackType = pStackType.intern();
		stackOrder = pStackOrder;
		showIcon = pShowIcon;
		triggeredId = trigId;
	    triggeredLevel = trigLvl;
	    this.iconId = iconId;
	    
		try
		{
			final Class<?> clazz = Class.forName("ru.catssoftware.gameserver.skills.effects.Effect" + name);

			_constructor = clazz.getConstructor(Env.class, EffectTemplate.class);

			Constructor<?> stolenConstructor = null;
			try
			{
				stolenConstructor = clazz.getConstructor(Env.class, L2Effect.class);
			}
			catch (NoSuchMethodException e)
			{
			}
			_stolenConstructor = stolenConstructor;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public L2Effect getEffect(Env env)
	{
		try
		{
			if (_attachCond == null || _attachCond.test(env))
				return (L2Effect) _constructor.newInstance(env, this);
		}
		catch (Exception e)
		{
			return null;
		}
		return null;
	}

	public L2Effect getStolenEffect(Env env, L2Effect stolen)
	{
		try
		{
			if (_stolenConstructor != null)
				return (L2Effect) _stolenConstructor.newInstance(env, stolen);
		}
		catch (Exception e)
		{
			return null;
		}
		return null;
	}

	public void attach(FuncTemplate f)
	{
		if (funcTemplates == null)
			funcTemplates = new FuncTemplate[1];
		else
			funcTemplates = Arrays.copyOf(funcTemplates, funcTemplates.length + 1);

		funcTemplates[funcTemplates.length - 1] = f;
	}
}