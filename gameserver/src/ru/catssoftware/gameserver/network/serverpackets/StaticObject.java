//TODO: Check

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

import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2StaticObjectInstance;

public class StaticObject extends L2GameServerPacket
{
	private final static String	S_99_STATICOBJECT	= "[S] 99 StaticObject";

	private final int			_staticObjectId;
	private final int			_objectId;

	public StaticObject(L2StaticObjectInstance staticObject)
	{
		_staticObjectId = staticObject.getStaticObjectId();
		_objectId = staticObject.getObjectId();
	}

	public StaticObject(L2DoorInstance door)
	{
		_staticObjectId = door.getDoorId();
		_objectId = door.getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x99);
		writeD(_staticObjectId);
		writeD(_objectId);
	}

	/* (non-Javadoc)
	* @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	*/
	@Override
	public String getType()
	{
		return S_99_STATICOBJECT;
	}
}
