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

import ru.catssoftware.gameserver.skills.Env;

final class ConditionPlayerCp extends Condition
{
	private final int _cp;
	private final String _msg;

	public ConditionPlayerCp(int cp)
	{
		_cp = cp;
		_msg = "Can be only used when CP is " + _cp + "% or lower.".intern();
	}

	@Override
	String getDefaultMessage()
	{
		return _msg;
	}

	@Override
	boolean testImpl(Env env)
	{
		return env.player.getStatus().getCurrentCp() * 100 / env.player.getMaxCp() <= _cp;
	}
}