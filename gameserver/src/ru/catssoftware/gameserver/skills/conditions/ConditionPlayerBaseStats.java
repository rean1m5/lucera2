/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.skills.conditions;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Env;

/**
 * @author mkizub
 */
class ConditionPlayerBaseStats extends Condition
{
	private final BaseStat	_stat;
	private final int		_value;

	public ConditionPlayerBaseStats(L2Character player, BaseStat stat, int value)
	{
		super();
		_stat = stat;
		_value = value;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if (!(env.player instanceof L2PcInstance))
			return false;
		L2PcInstance player = (L2PcInstance) env.player;
		switch (_stat)
		{
		case Int:
			return player.getINT() >= _value;
		case Str:
			return player.getStat().getSTR() >= _value;
		case Con:
			return player.getStat().getCON() >= _value;
		case Dex:
			return player.getStat().getDEX() >= _value;
		case Men:
			return player.getStat().getMEN() >= _value;
		case Wit:
			return player.getStat().getWIT() >= _value;
		}
		return false;
	}
}

enum BaseStat
{
	Int,
	Str,
	Con,
	Dex,
	Men,
	Wit
}
