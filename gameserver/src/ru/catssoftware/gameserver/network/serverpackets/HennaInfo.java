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

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.templates.item.L2Henna;

public final class HennaInfo extends L2GameServerPacket
{
	private static final String	_S__E4_HennaInfo	= "[S] E4 HennaInfo";

	private final L2PcInstance	_activeChar;
	private final L2Henna[]		_hennas				= new L2Henna[3];
	private int					_count				= 0;

	public HennaInfo(L2PcInstance player)
	{
		_activeChar = player;
		int slotCount = player.getLevel()<40?2:3;
		for (int i = 1; i <= slotCount; i++)
		{
			L2Henna h = _activeChar.getHenna(i);
			if (h != null)
				_hennas[_count++] = h;
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xe4);

		writeC(_activeChar.getHennaStatINT()); //equip INT
		writeC(_activeChar.getHennaStatSTR()); //equip STR
		writeC(_activeChar.getHennaStatCON()); //equip CON
		writeC(_activeChar.getHennaStatMEN()); //equip MEM
		writeC(_activeChar.getHennaStatDEX()); //equip DEX
		writeC(_activeChar.getHennaStatWIT()); //equip WIT

		if(getClient().getActiveChar().getLevel()<40)
			writeD(2);
		else
			writeD(3); // slots?

		writeD(_count); //size
		for (int i = 0; i < _count; i++)
		{
			writeD(_hennas[i].getSymbolId());
			writeD(_hennas[i].getSymbolId());
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__E4_HennaInfo;
	}
}
