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

import java.util.Vector;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Skill;


/**
 * Format: (ch) d [dd]
 *
 * @author  -Wooden-
 */
public class PledgeSkillList extends L2GameServerPacket
{
	private static final String	_S__FE_39_PLEDGESKILLLIST	= "[S] FE:39 PledgeSkillList";
	private L2Clan				_clan;
	private Vector<Skill>		_skill;

	// Really strange place to put this code ??
	class Skill
	{
		public int	id;
		public int	level;

		Skill(int pId, int pLevel)
		{
			id = pId;
			level = pLevel;
		}

	}

	public PledgeSkillList(L2Clan clan)
	{
		_clan = clan;
		_skill = new Vector<Skill>();
	}

	public void addSkill(int id, int level)
	{
		_skill.add(new Skill(id, level));
	}

	/**
	 * @see ru.catssoftware.gameserver.network.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		L2Skill[] skills = _clan.getAllSkills();

		writeC(0xFE);
		writeH(0x39);
		writeD(skills.length);
		for (L2Skill sk : skills)
		{
			writeD(sk.getId());
			writeD(sk.getLevel());
		}
	}

	/**
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_39_PLEDGESKILLLIST;
	}
}