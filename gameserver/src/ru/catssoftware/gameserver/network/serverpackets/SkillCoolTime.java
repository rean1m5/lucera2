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
import ru.catssoftware.gameserver.skills.TimeStamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *
 * @author  KenM
 */
public class SkillCoolTime extends L2GameServerPacket
{
	public List<TimeStamp> _reuseTimeStamps = Collections.emptyList();

	public SkillCoolTime(L2PcInstance cha)
	{
		_reuseTimeStamps = new ArrayList<TimeStamp>();
		for(TimeStamp ts : cha.getDisableSkills().values())
			if (ts.getReuse() >= 1000 && ts.getRemaining() >= 1000)
				_reuseTimeStamps.add(ts);

	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[S] C7 SkillCoolTime";
	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xC1);
		writeD(_reuseTimeStamps.size()); // list size
		for (TimeStamp ts : _reuseTimeStamps)
		{
			writeD(ts.getSkillId());
			writeD(ts.getSkillLevel());
			writeD((int) (ts.getReuse() / 1000));
			writeD((int) (ts.getRemaining() / 1000));
		}
		_reuseTimeStamps.clear();
	}

}
