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
import ru.catssoftware.gameserver.network.L2GameClient;

public class PartySmallWindowAdd extends L2GameServerPacket
{
	private static final String	_S__4F_PARTYSMALLWINDOWADD	= "[S] 4f PartySmallWindowAdd [dddsdddddddddd]";

	private L2PcInstance		_member;

	public PartySmallWindowAdd(L2PcInstance member)
	{
		_member = member;
	}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;

		writeC(0x4F);
		writeD(activeChar.getObjectId()); // c3
		writeD(0x00); //c3
		writeD(_member.getObjectId());
		writeS(_member.getName());

		writeD((int) _member.getStatus().getCurrentCp()); //c4
		writeD(_member.getMaxCp()); //c4

		writeD((int) _member.getStatus().getCurrentHp());
		writeD(_member.getMaxHp());
		writeD((int) _member.getStatus().getCurrentMp());
		writeD(_member.getMaxMp());
		writeD(_member.getLevel());
		writeD(_member.getClassId().getId());
		writeD(0x00);
		writeD(0x00);
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__4F_PARTYSMALLWINDOWADD;
	}
}