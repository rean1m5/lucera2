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

import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public final class PartySmallWindowAll extends L2GameServerPacket
{
	private static final String	_S__4E_PARTYSMALLWINDOWALL	= "[S] 4e PartySmallWindowAll [ddd (dsddddddddddd)]";

	private final L2Party _party;
	private final L2PcInstance _leader;

	public PartySmallWindowAll(L2Party party)
	{
		_party = party;
		_leader = _party.getLeader();
	}

	@Override
	protected void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		writeC(0x4E);
		writeD(_leader.getObjectId()); // c3 party leader id
		writeD(_party.getLootDistribution());//c3 party loot type (0,1,2,....)
		writeD(_party.getPartyMembers().size()-1);

		for (L2PcInstance member : _party.getPartyMembers())
		{
			if (member != null && member != activeChar)
			{
				writeD(member.getObjectId());
				writeS(member.getName());

				writeD((int) member.getStatus().getCurrentCp()); //c4
				writeD(member.getMaxCp()); //c4

				writeD((int) member.getStatus().getCurrentHp());
				writeD(member.getMaxHp());
				writeD((int) member.getStatus().getCurrentMp());
				writeD(member.getMaxMp());
				writeD(member.getLevel());
				writeD(member.getClassId().getId());
				writeD(0x00);//writeD(0x01); ??
				writeD(member.getRace().ordinal());
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__4E_PARTYSMALLWINDOWALL;
	}
}