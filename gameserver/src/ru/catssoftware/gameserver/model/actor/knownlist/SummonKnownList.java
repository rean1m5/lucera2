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
package ru.catssoftware.gameserver.model.actor.knownlist;

import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Summon;

public class SummonKnownList extends PlayableKnownList
{
	public SummonKnownList(L2Summon activeChar)
	{
		super(activeChar);
	}

	@Override
	public final L2Summon getActiveChar()
	{
		return (L2Summon)_activeChar;
	}

	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		if (object == getActiveChar().getOwner() || object == getActiveChar().getTarget())
			return 6000;
		return 3000;
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		return 1500;
	}
}