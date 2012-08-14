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

/**
 * @author  KenM
 */
public class ExDuelUpdateUserInfo extends L2GameServerPacket
{
	private static final String	_S__FE_50_EXDUELUPDATEUSERINFO	= "[S] FE:50 ExDuelUpdateUserInfo [sddddddddd]";
	private L2PcInstance		_activeChar;

	public ExDuelUpdateUserInfo(L2PcInstance cha)
	{
		_activeChar = cha;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x4f);
		writeS(_activeChar.getName());
		writeD(_activeChar.getObjectId());
		writeD(_activeChar.getClassId().getId());
		writeD(_activeChar.getLevel());
		writeD((int) _activeChar.getCurrentHp());
		writeD(_activeChar.getMaxHp());
		writeD((int) _activeChar.getCurrentMp());
		writeD(_activeChar.getMaxMp());
		writeD((int) _activeChar.getCurrentCp());
		writeD(_activeChar.getMaxCp());
	}

	@Override
	public String getType()
	{
		return _S__FE_50_EXDUELUPDATEUSERINFO;
	}

}
