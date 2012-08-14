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

import org.apache.commons.lang.ArrayUtils;

/**
 * @author NB4L1
 */
abstract class ConditionLogic extends Condition
{
	private Condition[] _conditions = EMPTY_ARRAY;

	public final Condition[] getConditions()
	{
		return _conditions;
	}

	public final void add(Condition condition)
	{
		if (condition == null)
			throw new IllegalStateException("Tried to add a 'null' condition to an <"
				+ getClass().getSimpleName().replace("ConditionLogic", "").toLowerCase() + "> condition");

		_conditions = (Condition[])ArrayUtils.add(_conditions, condition);
	}

	public final Condition getCanonicalCondition()
	{
		if (_conditions.length == 0)
			throw new IllegalStateException("Empty <"
				+ getClass().getSimpleName().replace("ConditionLogic", "").toLowerCase() + "> condition");
		
		if (_conditions.length == 1)
			return _conditions[0];
		
		return this;
	}

	@Override
	final String getDefaultMessage()
	{
		for (Condition c : getConditions())
		{
			String message = c.getMessage();
			if (message != null)
				return message;
		}

		return super.getDefaultMessage();
	}

	@Override
	final int getDefaultMessageId()
	{
		for (Condition c : getConditions())
		{
			int messageId = c.getMessageId();
			if (messageId != 0)
				return messageId;
		}

		return super.getDefaultMessageId();
	}
}