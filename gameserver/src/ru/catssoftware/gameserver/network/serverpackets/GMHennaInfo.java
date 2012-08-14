//TODO: remove
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

/**
 *
 * @author  KenM
 */
public class GMHennaInfo extends L2GameServerPacket
{
	private final static String	S_F0_GMHENNAINFO	= "[S] F0 GMHennaInfo";
	private final L2PcInstance	_activeChar;
	private final L2Henna[]		_hennas				= new L2Henna[3];
	private int					_count				= 0;

	public GMHennaInfo(L2PcInstance activeChar)
	{
		_activeChar = activeChar;

		for (int i = 0; i < 3; i++)
		{
			L2Henna h = _activeChar.getHenna(i + 1);
			if (h != null)
				_hennas[_count++] = h;
		}
	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return S_F0_GMHENNAINFO;
	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xEA);

		writeC(_activeChar.getHennaStatINT());
		writeC(_activeChar.getHennaStatSTR());
		writeC(_activeChar.getHennaStatCON());
		writeC(_activeChar.getHennaStatMEN());
		writeC(_activeChar.getHennaStatDEX());
		writeC(_activeChar.getHennaStatWIT());
		writeD(3); // slots?
		writeD(_count); //size
		for (int i = 0; i < _count; i++)
		{
			writeD(_hennas[i].getSymbolId());
			writeD(_hennas[i].getSymbolId());
		}
	}
}
