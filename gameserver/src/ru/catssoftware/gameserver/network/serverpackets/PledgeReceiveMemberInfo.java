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

import ru.catssoftware.gameserver.model.L2ClanMember;

/**
 *
 * @author  -Wooden-
 */
public class PledgeReceiveMemberInfo extends L2GameServerPacket
{
	private static final String	_S__FE_3D_PLEDGERECEIVEMEMBERINFO	= "[S] FE:3D PledgeReceiveMemberInfo";
	private L2ClanMember		_member;

	/**
	 * @param member
	 */
	public PledgeReceiveMemberInfo(L2ClanMember member)
	{
		_member = member;
	}

	/**
	 * @see ru.catssoftware.gameserver.network.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		//L2EMU_ADD
		if (_member == null)
			return;
		//L2EMU_ADD

		writeC(0xFE);
		writeH(0x3D);

		writeD(_member.getSubPledgeType());
		writeS(_member.getName());
		writeS(_member.getTitle()); // title
		writeD(_member.getPledgeRank()); // power

		//clan or subpledge name
		if (_member.getSubPledgeType() != 0)
		{
			writeS((_member.getClan().getSubPledge(_member.getSubPledgeType())).getName());
		}
		else
			writeS(_member.getClan().getName());

		writeS(_member.getApprenticeOrSponsorName()); // name of this member's apprentice/sponsor
	}

	/**
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_3D_PLEDGERECEIVEMEMBERINFO;
	}

}
