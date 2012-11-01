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

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Env;

/**
 * @author MrPoke
 */
final class ConditionPlayerHasFort extends Condition
{
	private final int _fort;

	public ConditionPlayerHasFort(int fort)
	{
		_fort = fort;
	}

	/**
	 * @see ru.catssoftware.gameserver.skills.conditions.Condition#testImpl(ru.catssoftware.gameserver.skills.Env)
	 */
	@Override
	public boolean testImpl(Env env)
	{
		if (!(env.player.isPlayer()))
			return false;

		L2Clan clan = ((L2PcInstance)env.player).getClan();
		if (clan == null)
			return _fort == 0;

		// Any fortress
		if (_fort == -1)
			return clan.getHasFort() > 0;

		return clan.getHasFort() == _fort;
	}
}