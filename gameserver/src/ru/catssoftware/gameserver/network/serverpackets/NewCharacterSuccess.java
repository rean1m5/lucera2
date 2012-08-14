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

import ru.catssoftware.gameserver.templates.chars.L2PcTemplate;

import javolution.util.FastList;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.7 $ $Date: 2005/03/27 15:29:39 $
 */
public class NewCharacterSuccess extends L2GameServerPacket
{
	// dddddddddddddddddddd
	private static final String	S_0D_CHARTEMPLATES	= "[S] 0d CharTemplates";
	private List<L2PcTemplate>	_chars				= new FastList<L2PcTemplate>();

	public NewCharacterSuccess()
	{
	}

	public void addChar(L2PcTemplate template)
	{
		_chars.add(template);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x17);
		writeD(_chars.size());

		for (L2PcTemplate temp : _chars)
		{
			if (temp == null)
				continue;

			writeD(temp.getRace().ordinal());
			writeD(temp.getClassId().getId());
			writeD(0x46);
			writeD(temp.getBaseSTR());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseDEX());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseCON());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseINT());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseWIT());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseMEN());
			writeD(0x0a);
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return S_0D_CHARTEMPLATES;
	}
}