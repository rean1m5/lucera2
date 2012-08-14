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
//TODO: Удалить
import static ru.catssoftware.gameserver.instancemanager.PartyRoomManager.ENTRIES_PER_PAGE;

import java.util.List;

import ru.catssoftware.gameserver.instancemanager.PartyRoomManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * Format:(ch) d [sdd]
 * @author  Crion/kombat
 */

public class ExListPartyMatchingWaitingRoom extends L2GameServerPacket
{
	private static final String	_S__FE_36_EXLISTPARTYMATCHINGWAITINGROOM = "[S] FE:36 ExListPartyMatchingWaitingRoom";

	private final List<L2PcInstance>	_waiting;
	private final int					_offset;
	private final int					_last;

	public ExListPartyMatchingWaitingRoom(int minLevel, int maxLevel, int page)
	{
		_waiting = PartyRoomManager.getInstance().getWaitingList(minLevel, maxLevel);
		_offset = (page - 1) * ENTRIES_PER_PAGE;
		_last = _offset + Math.min(_waiting.size() - _offset, ENTRIES_PER_PAGE);
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x36);

		writeD(_waiting.size() / 64 + 1); // total pages
		writeD(_last - _offset); // players in this page
		for (int i = _offset; i < _last; i++)
		{
			L2PcInstance player = _waiting.get(i);
			writeS(player.getName());
			writeD(player.getClassId().getId());
			writeD(player.getLevel());
		}
	}

	@Override
	public String getType()
	{
		return _S__FE_36_EXLISTPARTYMATCHINGWAITINGROOM;
	}
}
