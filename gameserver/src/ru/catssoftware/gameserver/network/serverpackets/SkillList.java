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

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;


public final class SkillList extends L2GameServerPacket
{
	private static final String _S__6D_SKILLLIST = "[S] 58 SkillList";

	private final List<L2Skill> _skills;

	public SkillList(List<L2Skill> list)
	{
		_skills = list;
	}

	@Override
	protected void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		writeC(0x58);
		writeD(_skills.size());

		for (L2Skill s : _skills)
		{
			writeD(s.isPassive() || s.isChance() ? 1 : 0);
			writeD(s.getLevel());
			writeD(s.getDisplayId());
			int grayed = 0;
			if(Config.DISABLE_SKILLS_ON_LEVEL_LOST) {
				if(s.getMagicLevel()-activeChar.getLevel() >= 5)
					grayed = 1;
			}
			writeC(grayed); // 1 = Disabled (gray) e.g. when transformed
		}
	}

	@Override
	public String getType()
	{
		return _S__6D_SKILLLIST;
	}
}