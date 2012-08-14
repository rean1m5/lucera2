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
package ru.catssoftware.gameserver.model.actor.stat;

import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;

public class DoorStat extends CharStat
{
	public DoorStat(L2DoorInstance activeChar)
	{
		super(activeChar);

		setLevel((byte) 1);
	}

	@Override
	public L2DoorInstance getActiveChar()
	{
		return (L2DoorInstance) _activeChar;
	}

	@Override
	public final byte getLevel()
	{
		return 1;
	}
}