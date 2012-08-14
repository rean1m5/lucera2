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
package ru.catssoftware.gameserver.skills.l2skills;

import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.StatsSet;

/**
 * @author Nemesiss
 */
public class L2SkillCreateItem extends L2Skill
{
	private final int[]	_createItemId;
	private final int	_createItemCount;
	private final int	_randomCount;

	public L2SkillCreateItem(StatsSet set)
	{
		super(set);
		_createItemId = set.getIntegerArray("create_item_id");
		_createItemCount = set.getInteger("create_item_count", 0);
		_randomCount = set.getInteger("random_count", 1);
	}

	/**
	 * @see ru.catssoftware.gameserver.model.L2Skill#useSkill(ru.catssoftware.gameserver.model.L2Character,
	 *      L2Character...)
	 */
	@Override
	public void useSkill(L2Character activeChar, L2Character... targets)
	{
		if (activeChar.isAlikeDead())
			return;
		if (_createItemId == null || _createItemCount == 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
			sm.addSkillName(this);
			activeChar.sendPacket(sm);
			return;
		}
		L2PcInstance player = (L2PcInstance) activeChar;
		if (activeChar instanceof L2PcInstance)
		{
			int rnd = Rnd.nextInt(_randomCount) + 1;
			int count = _createItemCount * rnd;
			int rndid = Rnd.nextInt(_createItemId.length);
			giveItems(player, _createItemId[rndid], count);
		}
	}

	/**
	 * @param activeChar
	 * @param itemId
	 * @param count
	 */
	public void giveItems(L2PcInstance activeChar, int itemId, int count)
	{
		
		activeChar.addItem(getName(), itemId, count, activeChar, true);
	}
}