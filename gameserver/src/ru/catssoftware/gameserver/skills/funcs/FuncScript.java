package ru.catssoftware.gameserver.skills.funcs;

import java.lang.reflect.Method;

import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.skills.conditions.Condition;


public class FuncScript extends Func {
	private Method _method = null;
	public FuncScript(Stats pStat, int pOrder, FuncOwner pFuncOwner, String value, Condition pCondition)
	{
		super(pStat, pOrder, pFuncOwner, pCondition);
		try {
			Class <?> clazz = Class.forName("ru.catssoftware.gameserver.skills.effects.calc."+value);
			_method = clazz.getDeclaredMethod(pStat.getValue(), Env.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	@Override
	protected void calc(Env env) {
		if(_method!=null) try {
			_method.invoke(null, env);
		} catch(Exception e) { }

	}

}
