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
package ru.catssoftware.lang;

import java.util.Map;

import javolution.util.FastMap;

/**
 * @author NB4L1
 */
public final class L2Integer
{
	private static final Map<Integer, Integer> MAP = new FastMap<Integer, Integer>();

	private static final int MIN = -1000;
	private static final int MAX = 100000;
	private static final int TOTAL = MAX - MIN + 1;

	private static final Integer[] ARRAY = new Integer[TOTAL];

	static
	{
		for (int i = 0; i < TOTAL; i++)
			ARRAY[i] = MIN + i;
	}

	public static Integer valueOf(String s)
	{
		return valueOf(Integer.parseInt(s));
	}

	public static Integer valueOf(int intValue)
	{
		if (MIN <= intValue && intValue <= MAX)
			return ARRAY[intValue - MIN];

		Integer integerValue = intValue;

		synchronized (MAP)
		{
			Integer cachedInteger = MAP.get(integerValue);

			if (cachedInteger != null)
				return cachedInteger;

			MAP.put(integerValue, integerValue);
		}

		return integerValue;
	}
}