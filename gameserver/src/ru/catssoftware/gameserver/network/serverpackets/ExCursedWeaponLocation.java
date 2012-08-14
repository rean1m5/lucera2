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
package ru.catssoftware.gameserver.network.serverpackets;

import java.util.List;

import ru.catssoftware.gameserver.model.Location;


/**
 * @author  -Wooden-
 */
public class ExCursedWeaponLocation extends L2GameServerPacket
{
	private static final String		_S__FE_47_EXCURSEDWEAPONLOCATION	= "[S] FE:47 ExCursedWeaponLocation [d(dd ddd)]";
	private List<CursedWeaponInfo>	_cursedWeaponInfo;

	public ExCursedWeaponLocation(List<CursedWeaponInfo> cursedWeaponInfo)
	{
		_cursedWeaponInfo = cursedWeaponInfo;
	}

	/**
	 * @see ru.catssoftware.gameserver.network.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x46); // confirmed

		if (!_cursedWeaponInfo.isEmpty())
		{
			writeD(_cursedWeaponInfo.size());
			for (CursedWeaponInfo w : _cursedWeaponInfo)
			{
				writeD(w.id);
				writeD(w.activated);

				writeD(w.loc.getX());
				writeD(w.loc.getY());
				writeD(w.loc.getZ());
			}
		}
		else
		{
			writeD(0);
			writeD(0);
		}
	}

	/**
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_47_EXCURSEDWEAPONLOCATION;
	}

	public static class CursedWeaponInfo
	{
		public final Location loc;
		public final int id;
		public final int activated; //0 - not activated ? 1 - activated

		public CursedWeaponInfo(Location pLoc, int ID, int status)
		{
			loc = pLoc;
			id = ID;
			activated = status;
		}
	}
}