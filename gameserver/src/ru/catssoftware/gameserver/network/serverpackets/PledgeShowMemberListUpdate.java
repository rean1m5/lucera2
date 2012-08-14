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

/**
 * @author -Wooden-
 *
 */
public class PledgeShowMemberListUpdate extends L2GameServerPacket
{
	private static final String	_S__54_PLEDGESHOWMEMBERLISTUPDATE	= "[S] 54 PledgeShowMemberListUpdate";
	private L2PcInstance		_activeChar;
	private int					_pledgeType;
	private int					_hasSponsor;
	private String				_name;
	private int					_level;
	private int					_classId;
	private int					_objectId;
	private boolean				_isOnline;
	private int					_race;
	private int					_sex;

	public PledgeShowMemberListUpdate(L2PcInstance player)
	{
		_activeChar = player;
		_pledgeType = _activeChar.getSubPledgeType();
		if (_pledgeType == L2Clan.SUBUNIT_ACADEMY)
			_hasSponsor = _activeChar.getSponsor() != 0 ? 1 : 0;
		else
			_hasSponsor = 0;
		_name = _activeChar.getName();
		_level = _activeChar.getLevel();
		_classId = _activeChar.getClassId().getId();
		_race = _activeChar.getRace().ordinal();
		_sex = _activeChar.getAppearance().getSex() ? 1 : 0;
		_objectId = _activeChar.getObjectId();
		_isOnline = _activeChar.isOnline() == 1;
	}

	public PledgeShowMemberListUpdate(L2ClanMember player)
	{
		_activeChar = player.getPlayerInstance();
		_name = player.getName();
		_level = player.getLevel();
		_classId = player.getClassId();
		_objectId = player.getObjectId();
		_isOnline = player.isOnline();
		_pledgeType = player.getSubPledgeType();
		_race = player.getRace();
		_sex = player.getSex();
		if (_pledgeType == L2Clan.SUBUNIT_ACADEMY)
			_hasSponsor = player.getSponsor() != 0 ? 1 : 0;
		else
			_hasSponsor = 0;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x54);
		writeS(_name);
		writeD(_level);
		writeD(_classId);
		writeD(_sex);
		writeD(_race);
		writeD(_isOnline ? _objectId : 0);
		writeD(_pledgeType);
		writeD(_hasSponsor);
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__54_PLEDGESHOWMEMBERLISTUPDATE;
	}
}
