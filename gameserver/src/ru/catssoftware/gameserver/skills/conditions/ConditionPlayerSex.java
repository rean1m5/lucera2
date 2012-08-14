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

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Env;

class ConditionPlayerSex extends Condition
{
	//male 0 female 1
	private final int	_sex;

	public ConditionPlayerSex(int sex)
	{
		_sex = sex;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return (((L2PcInstance) env.player).getAppearance().getSex() ? 1 : 0) == _sex;
	}
}