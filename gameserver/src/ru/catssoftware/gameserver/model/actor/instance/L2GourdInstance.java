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

import java.util.concurrent.Future;

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.taskmanager.DecayTaskManager;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;


/**
 * @author Rus-Team
 */
public final class L2GourdInstance extends L2MonsterInstance
{
	private int	_lifeTime;

	class GourdLifeTime implements Runnable
	{
		public void run()
		{
			if (!_gourd.isDead())
				_lifeTime += 1000;
		}

		@SuppressWarnings("unused")
		private final L2PcInstance		_owner;
		private final L2GourdInstance	_gourd;

		GourdLifeTime(L2PcInstance owner, L2GourdInstance gourd)
		{
			super();
			_owner = owner;
			_gourd = gourd;
		}
	}

	@SuppressWarnings("unchecked")
	public L2GourdInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		@SuppressWarnings("unused")
		Future _gourdLifeTask = null;
		DecayTaskManager.getInstance().addDecayTask(this);
		_lifeTime = 0;
		_gourdLifeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new GourdLifeTime(getOwner(), this), 0L, 1000L);
	}

	public int getLifeTime()
	{
		return _lifeTime;
	}

	public void setLifeTime(int duration)
	{
		_lifeTime = duration;
	}

	public L2PcInstance getOwner()
	{
		return _owner;
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, L2Skill skill)
	{
		for (int npcIds : NPC_IDS)
		{
			if (npcIds == getNpcId())
			{
				if (attacker.getActiveWeaponInstance().getItemId() == 4202 || attacker.getActiveWeaponInstance().getItemId() == 5133
						|| attacker.getActiveWeaponInstance().getItemId() == 5817 || attacker.getActiveWeaponInstance().getItemId() == 7058)
					super.reduceCurrentHp(damage, attacker, awake, skill);
				else if (damage > 0.0D)
					damage = 0.0D;

				break;
			}
		}
		super.reduceCurrentHp(damage, attacker, awake, skill);
	}

	int[]					NPC_IDS			=
											{ 12778, 12779 };
	@SuppressWarnings("unused")
	private final int		LIVE_TIME		= 0x2bf20;
	@SuppressWarnings("unused")
	private final int		ADD_LIVE_TIME	= 1000;
	private L2PcInstance	_owner;
}