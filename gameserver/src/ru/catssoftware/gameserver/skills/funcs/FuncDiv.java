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
package ru.catssoftware.gameserver.skills.funcs;

import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.skills.conditions.Condition;

public final class FuncDiv extends FuncLambda
{
	public FuncDiv(Stats pStat, int pOrder, FuncOwner pFuncOwner, String value, Condition pCondition)
	{
		super(pStat, pOrder, pFuncOwner, value, pCondition);
	}
	public FuncDiv(Stats pStat, int pOrder, FuncOwner pFuncOwner, double value, Condition pCondition)
	{
		super(pStat, pOrder, pFuncOwner, String.valueOf(value), pCondition);
	}

	@Override
	protected void calc(Env env)
	{
		env.value /= _lambda;
	}
}