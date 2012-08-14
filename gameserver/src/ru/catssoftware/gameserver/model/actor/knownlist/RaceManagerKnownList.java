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

import ru.catssoftware.gameserver.MonsterRace;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2RaceManagerInstance;
import ru.catssoftware.gameserver.network.serverpackets.DeleteObject;

public class RaceManagerKnownList extends NpcKnownList
{
	public RaceManagerKnownList(L2RaceManagerInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
			return false;

		if (object instanceof L2PcInstance)
		{
			DeleteObject obj = null;
			for (int i = 0; i < 8; i++)
			{
				obj = new DeleteObject(MonsterRace.getInstance().getMonsters()[i]);
				((L2PcInstance) object).sendPacket(obj);
			}
		}

		return true;
	}

	@Override
	public L2RaceManagerInstance getActiveChar()
	{
		return (L2RaceManagerInstance)_activeChar;
	}
}