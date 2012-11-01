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

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager.SiegeSpawn;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import javolution.util.FastList;


public class L2FortCommanderInstance extends L2FortSiegeGuardInstance
{
	private boolean _canTalk;

	public L2FortCommanderInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		_canTalk = true;
	}

	/**
	 * Return True if a siege is in progress and the L2Character attacker isn't a Defender.<BR><BR>
	 *
	 * @param attacker The L2Character that the L2CommanderInstance try to attack
	 *
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if ( attacker == null || !(attacker.isPlayer()) )
			return false;

		boolean isFort = (getFort() != null && getFort().getFortId() > 0 
			&& getFort().getSiege().getIsInProgress() &&
				!getFort().getSiege().checkIsDefender(((L2PcInstance)attacker).getClan()));

		// Attackable during siege by all except defenders
		return isFort;
	}

	@Override
	public void addDamageHate(L2Character attacker, int damage, int aggro, L2Skill skill)
	{
		if (attacker == null)
			return;

		if (!(attacker instanceof L2FortCommanderInstance))
			super.addDamageHate(attacker, damage, aggro, skill);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		if (getFort().getSiege().getIsInProgress())
			getFort().getSiege().killedCommander(this);

		return true;
	}

	/**
	 * This method forces guard to return to home location previously set
	 */
	@Override
	public void returnHome()
	{
		if (!isInsideRadius(getSpawn().getLocx(), getSpawn().getLocy(), 200, false))
		{
			setisReturningToSpawnPoint(true);
			clearAggroList();
			if (hasAI())
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz(), 0));
		}
	}

	@Override
	public final void addDamage(L2Character attacker, int damage, L2Skill skill)
	{
		L2Spawn spawn = getSpawn();
		if (spawn != null && canTalk())
		{
			FastList<SiegeSpawn> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getFortId());
			for (SiegeSpawn spawn2 : commanders)
			{
				if (spawn2.getNpcId() == spawn.getNpcid())
				{
					String text = "";
					switch (spawn2.getId())
					{
					case 1:
						text = "Attacking the enemy's reinforcements is necesary. Time to Die!";
						break;
					case 2:
						if (attacker instanceof L2Summon)
							attacker = ((L2Summon) attacker).getOwner();
						text = "Everyone, concentrate your attacks on " + attacker.getName() + "! Show the enemy your resolve!";
						break;
					case 3:
						text = "Spirit of Fire, unleash your power! Burn the enemy!!";
						break;
					}
					if (!text.isEmpty())
					{
						broadcastPacket(new NpcSay(getObjectId(), 1, getNpcId(), text));
						setCanTalk(false);
						ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTalkTask(), 10000);
					}
				}
			}
		}
		super.addDamage(attacker, damage, skill);
	}

	private class ScheduleTalkTask implements Runnable
	{
		public void run()
		{
			setCanTalk(true);
		}
	}

	void setCanTalk(boolean val)
	{
		_canTalk = val;
	}

	private boolean canTalk()
	{
		return _canTalk;
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}