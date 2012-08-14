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
package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ai.CtrlEvent;
import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

public class L2PenaltyMonsterInstance extends L2MonsterInstance
{
	private L2PcInstance	_ptk;

	public L2PenaltyMonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public L2Character getMostHated()
	{
		return _ptk;
	}

	@Deprecated
	public void notifyPlayerDead()
	{
		// Monster kill player and can by deleted
		deleteMe();

		L2Spawn spawn = getSpawn();
		if (spawn != null)
		{
			spawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(spawn, false);
		}
	}

	public void setPlayerToKill(L2PcInstance ptk)
	{
		if (Rnd.nextInt(100) <= 80)
		{
			CreatureSay cs = new CreatureSay(getObjectId(), SystemChatChannelId.Chat_Normal, getName(), "Ммм, ваша приманка была вкусной!");
			broadcastPacket(cs);
		}
		_ptk = ptk;
		addDamageHate(ptk, 10, 10);
		getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, ptk);
		addAttackerToAttackByList(ptk);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		if (Rnd.nextInt(100) <= 75)
		{
			CreatureSay cs = new CreatureSay(getObjectId(), SystemChatChannelId.Chat_Normal, getName(), "Я скажу рыбам не принимать вашу приманку!");
			broadcastPacket(cs);
		}
		return true;
	}
}