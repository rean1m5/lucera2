package ru.catssoftware.gameserver.skills.conditions;

/*
 * @author Ro0TT
 * @date 30.10.2012
 */

import ru.catssoftware.gameserver.skills.Env;

public class ConditionPlayerLevelMax extends Condition
{
	private final int	_level;

	public ConditionPlayerLevelMax(int level)
	{
		_level = level;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.player.getLevel() <= _level;
	}
}
