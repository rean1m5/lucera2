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

/**
 */
public class PetDelete extends L2GameServerPacket
{
	private static final String	_S__CF_PETDELETE	= "[S] b6 PetDelete";
	private int					_petType;
	private int					_petObjId;

	public PetDelete(int petType, int petObjId)
	{
		_petType = petType; // summonType
		_petObjId = petObjId; //objectId
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xB6);
		writeD(_petType);// dont really know what these two are since i never needed them
		writeD(_petObjId);//objectId
	}

	@Override
	public String getType()
	{
		return _S__CF_PETDELETE;
	}
}
