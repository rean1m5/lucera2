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

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2CabaleBufferInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2FestivalGuideInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;

public class NpcKnownList extends CharKnownList
{
	public NpcKnownList(L2NpcInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public L2NpcInstance getActiveChar()
	{
		return (L2NpcInstance)_activeChar;
	}

	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		return 2 * getDistanceToWatchObject(object);
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2FestivalGuideInstance)
			return 4000;

		if (object instanceof L2FolkInstance || !(object instanceof L2Character))
			return 0;

		if (object instanceof L2CabaleBufferInstance)
			return 900;

		if (object instanceof L2PlayableInstance)
			return 1500;

		return 500;
	}
}