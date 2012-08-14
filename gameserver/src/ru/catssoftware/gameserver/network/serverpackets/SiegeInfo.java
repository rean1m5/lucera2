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

import java.util.Calendar;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.network.L2GameClient;

/**
 * Shows the Siege Info<BR>
 * <BR>
 * packet type id 0xc9<BR>
 * format: cdddSSdSdd<BR>
 * <BR>
 * c = c9<BR>
 * d = CastleID<BR>
 * d = Show Owner Controls (0x00 default || >=0x02(mask?) owner)<BR>
 * d = Owner ClanID<BR>
 * S = Owner ClanName<BR>
 * S = Owner Clan LeaderName<BR>
 * d = Owner AllyID<BR>
 * S = Owner AllyName<BR>
 * d = current time (seconds)<BR>
 * d = Siege time (seconds) (0 for selectable)<BR>
 * d = (UNKNOW) Siege Time Select Related?
 * @author KenM
 */
public class SiegeInfo extends L2GameServerPacket
{
	private static final String	_S__C9_SIEGEINFO	= "[S] c9 SiegeInfo";

	private Castle				_castle;
	private ClanHall			_clanHall;
	private Calendar			_siegeDate;

	public SiegeInfo(Castle castle,ClanHall clanHall,Calendar siegeDate)
	{
		_castle = castle;
		_clanHall = clanHall;
		_siegeDate = siegeDate;
	}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;
		if (_castle==null)
		{
			writeC(0xC9);
			writeD(_clanHall.getId());
			writeD(((_clanHall.getOwnerId() == activeChar.getClanId()) && (activeChar.isClanLeader())) ? 0x01 : 0x00);
			writeD(_clanHall.getOwnerId());
			if (_clanHall.getOwnerId() > 0)
			{
				L2Clan owner = ClanTable.getInstance().getClan(_clanHall.getOwnerId());
				if (owner != null)
				{
					writeS(owner.getName()); // Clan Name
					writeS(owner.getLeaderName()); // Clan Leader Name
					writeD(owner.getAllyId()); // Ally ID
					writeS(owner.getAllyName()); // Ally Name
				}
				else
					_log.warn("Null owner for castle: " + _castle.getName());
			}
			else
			{
				writeS("NPC"); // Clan Name
				writeS(""); // Clan Leader Name
				writeD(0); // Ally ID
				writeS(""); // Ally Name
			}

			writeD((int) (Calendar.getInstance().getTimeInMillis()/1000));
			writeD((int) (_siegeDate.getTimeInMillis() / 1000));
			writeD(0x00); //number of choices?
		}
		else
		{
			writeC(0xC9);
			writeD(_castle.getCastleId());
			writeD(((_castle.getOwnerId() == activeChar.getClanId()) && (activeChar.isClanLeader())) ? 0x01 : 0x00);
			writeD(_castle.getOwnerId());
			if (_castle.getOwnerId() > 0)
			{
				L2Clan owner = ClanTable.getInstance().getClan(_castle.getOwnerId());
				if (owner != null)
				{
					writeS(owner.getName()); // Clan Name
					writeS(owner.getLeaderName()); // Clan Leader Name
					writeD(owner.getAllyId()); // Ally ID
					writeS(owner.getAllyName()); // Ally Name
				}
				else
					_log.warn("Null owner for castle: " + _castle.getName());
			}
			else
			{
				writeS("NPC"); // Clan Name
				writeS(""); // Clan Leader Name
				writeD(0); // Ally ID
				writeS(""); // Ally Name
			}

			writeD((int) (Calendar.getInstance().getTimeInMillis()/1000));
			writeD((int) (_castle.getSiege().getSiegeDate().getTimeInMillis() / 1000));
			writeD(0x00); //number of choices?
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__C9_SIEGEINFO;
	}
}