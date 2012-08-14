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

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

/**
 * format   SdSS dddddddd d (Sddddd)
 *
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class GMViewPledgeInfo extends L2GameServerPacket
{
	private static final String	_S__A9_GMVIEWPLEDGEINFO	= "[S] 90 GMViewPledgeInfo";
	private L2Clan				_clan;
	private L2PcInstance		_activeChar;

	public GMViewPledgeInfo(L2Clan clan, L2PcInstance activeChar)
	{
		_clan = clan;
		_activeChar = activeChar;
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0x90);
		writeS(_activeChar.getName());
		writeD(_clan.getClanId());
		writeD(0x00); // Ashitaka fix
		writeS(_clan.getName());
		writeS(_clan.getLeaderName());
		writeD(_clan.getCrestId()); // -> no, it's no longer used (nuocnam) fix by game
		writeD(_clan.getLevel());
		writeD(_clan.getHasCastle());
		writeD(_clan.getHasHideout());
		writeD(_clan.getRank()); // Ashitaka fix
		writeD(_clan.getReputationScore());//writeD(_activeChar.getLevel()); Ashitaka Fix
		writeD(0);
		writeD(0);

		writeD(_clan.getAllyId()); //c2
		writeS(_clan.getAllyName()); //c2
		writeD(_clan.getAllyCrestId()); //c2
		writeD(_clan.isAtWar() ? 1 : 0); //c3

		L2ClanMember[] members = _clan.getMembers();
		writeD(members.length);

		for (L2ClanMember element : members)
		{
			if (element == null)
				continue;

			writeS(element.getName());
			writeD(element.getLevel());
			writeD(element.getClassId());
			writeD(element.getSex());
			writeD(element.getRace());
			writeD(element.isOnline() ? element.getObjectId() : 0);
			writeD(element.getSponsor());
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__A9_GMVIEWPLEDGEINFO;
	}

}
