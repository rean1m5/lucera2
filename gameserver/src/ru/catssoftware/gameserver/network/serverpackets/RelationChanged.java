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
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;

/**
 * @author  Luca Baldi
 */
public final class RelationChanged extends L2GameServerPacket
{
	private static final String	_S__CE_RELATIONCHANGED	= "[S] CE RelationChanged";

	public static final int RELATION_PARTY1 = 0x00001; // party member
	public static final int RELATION_PARTY2 = 0x00002; // party member
	public static final int RELATION_PARTY3 = 0x00004; // party member
	public static final int RELATION_PARTY4 = 0x00008; // party member (for information, see L2PcInstance.getRelation())
	public static final int RELATION_PARTYLEADER = 0x00010; // true if is party leader
	public static final int RELATION_HAS_PARTY = 0x00020; // true if is in party
	public static final int RELATION_CLAN_MEMBER = 0x00040; // true if is in clan
	public static final int RELATION_LEADER = 0x00080; // leader
	public static final int RELATION_INSIEGE = 0x00200; // true if in siege
	public static final int RELATION_ATTACKER = 0x00400; // true when attacker
	public static final int RELATION_ALLY = 0x00800; // blue siege icon, cannot have if red
	public static final int RELATION_ENEMY = 0x01000; // true when red icon, doesn't matter with blue
	public static final int RELATION_MUTUAL_WAR = 0x08000; // double fist
	public static final int RELATION_1SIDED_WAR = 0x10000; // single fist

	public static void sendRelationChanged(L2PcInstance target, L2PcInstance attacker)
	{
		if (target == null || attacker == null || attacker.isOfflineTrade())
			return;

		int currentRelation = target.getRelation(attacker);
		
		attacker.sendPacket(new RelationChanged(target, currentRelation, attacker));
		if (target.getPet() != null)
			attacker.sendPacket(new RelationChanged(target.getPet(), currentRelation, attacker));
	}

	private final int _objId;
	private final int _relation;
	private final int _autoAttackable;
	private final int _karma;
	private final int _pvpFlag;

	public RelationChanged(L2PlayableInstance target, int relation, L2PcInstance attacker)
	{
		_objId = target.getObjectId();
		_relation = relation;
		_autoAttackable = target.isAutoAttackable(attacker) ? 1 : 0;
		_karma = target != null && target.isPlayer() ? target.getPlayer().getKarma() : 0;
		_pvpFlag = target != null && target.isPlayer() ? target.getPlayer().getPvpFlag() : 0;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xCE);
		writeD(_objId);
		writeD(_relation);
		writeD(_autoAttackable);
		writeD(_karma);
		writeD(_pvpFlag);
	}

	@Override
	public String getType()
	{
		return _S__CE_RELATIONCHANGED;
	}
}