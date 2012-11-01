package ru.catssoftware.gameserver.skills;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.funcs.Func;
import ru.catssoftware.gameserver.skills.funcs.FuncOwner;

public final class Calculator
{
	private Func[]				_functions;

	public Calculator()
	{
		_functions = Func.EMPTY_ARRAY;
	}

	public Calculator(Calculator c)
	{
		_functions = c._functions;
	}

	public static boolean equalsCals(Calculator c1, Calculator c2)
	{
		if (c1 == c2)
			return true;

		if (c1 == null || c2 == null)
			return false;

		Func[] funcs1 = c1._functions;
		Func[] funcs2 = c2._functions;

		if (funcs1 == funcs2)
			return true;

		if (funcs1.length != funcs2.length)
			return false;

		if (funcs1.length == 0)
			return true;

		for (int i = 0; i < funcs1.length; i++)
		{
			if (funcs1[i] != funcs2[i])
				return false;
		}
		return true;
	}

	public int size()
	{
		if (_functions != null)
			return _functions.length;
		else
			return 0;
	}

	public synchronized void addFunc(Func f)
	{
		Func[] funcs = _functions;
		Func[] tmp = new Func[funcs.length + 1];

		final int order = f.order;

		int i;
		for (i = 0; i < funcs.length && order >= funcs[i].order; i++)
			tmp[i] = funcs[i];

		tmp[i] = f;

		for (; i < funcs.length; i++)
			tmp[i + 1] = funcs[i];

		_functions = tmp;
	}

	private synchronized void removeFunc(Func f)
	{
		Func[] funcs = _functions;
		Func[] tmp = new Func[funcs.length - 1];

		int i;
		for (i = 0; i < funcs.length && f != funcs[i]; i++)
			tmp[i] = funcs[i];

		if (i == funcs.length)
			return;

		for (i++; i < funcs.length; i++)
			tmp[i - 1] = funcs[i];

		if (tmp.length == 0)
			_functions = Func.EMPTY_ARRAY;
		else
			_functions = tmp;
	}

	public synchronized void removeOwner(FuncOwner owner, L2Character cha)
	{
		for (Func element : _functions)
		{
			if (element.funcOwner == owner)
			{
				removeFunc(element);

				if (cha.isPlayer())
					((L2PcInstance) cha).onFuncRemoval(element);
			}
		}
	}

	public void calc(Env env)
	{
		for (Func element : _functions)
			element.calcIfAllowed(env);
	}
}