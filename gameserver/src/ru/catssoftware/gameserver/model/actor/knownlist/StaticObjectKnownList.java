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
import ru.catssoftware.gameserver.model.actor.instance.L2FortSiegeGuardInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SiegeGuardInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2StaticObjectInstance;

public class StaticObjectKnownList extends CharKnownList
{
	public StaticObjectKnownList(L2StaticObjectInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public final L2StaticObjectInstance getActiveChar()
	{
		return (L2StaticObjectInstance)_activeChar;
	}

	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		if (object instanceof L2SiegeGuardInstance || object instanceof L2FortSiegeGuardInstance)
			return 800;
		if (!(object instanceof L2PcInstance))
			return 0;
		return 4000;
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2SiegeGuardInstance || object instanceof L2FortSiegeGuardInstance)
			return 600;
		if (!(object instanceof L2PcInstance))
			return 0;
		return 2000;
	}
}