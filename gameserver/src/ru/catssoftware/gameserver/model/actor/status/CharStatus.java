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
package ru.catssoftware.gameserver.model.actor.status;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.DuelManager;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance.ConditionListenerDependency;
import ru.catssoftware.gameserver.model.actor.stat.CharStat;
import ru.catssoftware.gameserver.model.entity.Duel;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.model.olympiad.OlympiadGame;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.taskmanager.AbstractIterativePeriodicTaskManager;
import ru.catssoftware.tools.random.Rnd;


public class CharStatus
{
	protected static final Logger _log = Logger.getLogger(CharStatus.class);

	private static final byte REGEN_FLAG_HP = 1;
	private static final byte REGEN_FLAG_MP = 2;
	private static final byte REGEN_FLAG_CP = 4;

	protected final L2Character _activeChar;
	private final int _period;

	private byte _flagsRegenActive = 0;
	private double _currentHp = 0;
	private double _currentMp = 0;
	private double _currentCp = 0;

	public CharStatus(L2Character activeChar)
	{
		_activeChar = activeChar;
		_period = Formulas.getRegeneratePeriod(_activeChar);
	}
	
	protected L2Character getActiveChar()
	{
		return _activeChar;
	}

	public final double getCurrentHp()
	{
		return _currentHp;
	}

	public final double getCurrentMp()
	{
		return _currentMp;
	}

	public final double getCurrentCp()
	{
		return _currentCp;
	}

	public final void setCurrentHpMp(double newHp, double newMp)
	{
		setCurrentHp(newHp);
		setCurrentMp(newMp);
	}

	public final void setCurrentHp(double newHp)
	{
		if (getActiveChar().isDead())
			return;

		double maxHp = getActiveChar().getStat().getMaxHp();
		if (newHp < 0)
			newHp = 0;

		if (getActiveChar().getHealLimit()>0)
		{
			if (newHp>(maxHp/100*getActiveChar().getHealLimit()))
				newHp = maxHp/100*getActiveChar().getHealLimit();
		}

		synchronized (this)
		{
			if (newHp >= maxHp)
			{
				_currentHp = maxHp;
				_flagsRegenActive &= ~REGEN_FLAG_HP;
				
				if (_flagsRegenActive == 0)
					stopHpMpRegeneration();
			}
			else
			{
				_currentHp = newHp;
				_flagsRegenActive |= REGEN_FLAG_HP;
				
				startHpMpRegeneration();
			}
		}

		if (getActiveChar().isPlayer())
		{
			if (getCurrentHp() <= maxHp * 0.3)
			{
				QuestState qs = ((L2PcInstance)getActiveChar()).getQuestState("255_Tutorial");
				if (qs != null)
					qs.getQuest().notifyEvent("CE45", null, ((L2PcInstance) getActiveChar()));
			}
		}

		getActiveChar().broadcastStatusUpdate();

		if (getActiveChar().isPlayer())
			((L2PcInstance) getActiveChar()).refreshConditionListeners(ConditionListenerDependency.PLAYER_HP);
	}

	public final void setCurrentMp(double newMp)
	{
		if (getActiveChar().isDead())
			return;

		double maxMp = getActiveChar().getStat().getMaxMp();
		if (newMp < 0)
			newMp = 0;

		if (getActiveChar().getHealLimit()>0)
		{
			if (newMp>(maxMp/100*getActiveChar().getHealLimit()))
				newMp = maxMp/100*getActiveChar().getHealLimit();
		}
		
		synchronized (this)
		{
			if (newMp >= maxMp)
			{
				_currentMp = maxMp;
				_flagsRegenActive &= ~REGEN_FLAG_MP;

				if (_flagsRegenActive == 0)
					stopHpMpRegeneration();
			}
			else
			{
				_currentMp = newMp;
				_flagsRegenActive |= REGEN_FLAG_MP;

				startHpMpRegeneration();
			}
		}

		getActiveChar().broadcastStatusUpdate();
	}

	public final void setCurrentCp(double newCp)
	{
		if (getActiveChar().isDead())
			return;

		double maxCp = getActiveChar().getStat().getMaxCp();
		if (newCp < 0)
			newCp = 0;

		if (getActiveChar().getHealLimit()>0)
		{
			if (newCp>(maxCp/100*getActiveChar().getHealLimit()))
				newCp = maxCp/100*getActiveChar().getHealLimit();
		}
		
		synchronized (this)
		{
			if (newCp >= maxCp)
			{
				_currentCp = maxCp;
				_flagsRegenActive &= ~REGEN_FLAG_CP;

				if (_flagsRegenActive == 0)
					stopHpMpRegeneration();
			}
			else
			{
				_currentCp = newCp;
				_flagsRegenActive |= REGEN_FLAG_CP;

				startHpMpRegeneration();
			}
		}

		getActiveChar().broadcastStatusUpdate();
	}

	boolean canReduceHp(double value, L2Character attacker, boolean awake, boolean isDOT)
	{
		if (attacker == null || getActiveChar().isDead() || getActiveChar().isPetrified())
			return false;

		if (getActiveChar().isInvul())
			return false;

		if (Duel.isInvul(attacker, getActiveChar()))
			return false;

		if (getActiveChar() instanceof L2Attackable)
			return ((L2Attackable)getActiveChar()).canReduceHp(value,attacker);
		return true;
	}

	public final void increaseHp(double value)
	{
		setCurrentHp(getCurrentHp() + value);
	}

	public final void reduceHp(double value, L2Character attacker)
	{
		reduceHp(value, attacker, true);
	}

	public final void reduceHp(double value, L2Character attacker, boolean awake)
	{
		reduceHp(value, attacker, awake, false, false);
	}

	public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean directHp)
	{
		if (!canReduceHp(value, attacker, awake, isDOT))
			return;

		reduceHp0(value, attacker, awake, isDOT, directHp);
	}

	void reduceHp0(double value, L2Character attacker, boolean awake, boolean isDOT, boolean directHp)
	{
		if (!isDOT)
		{
			if (awake)
			{
				if (getActiveChar().isSleeping())
					getActiveChar().stopSleeping(null);
				if (getActiveChar().isImmobileUntilAttacked())
					getActiveChar().stopImmobileUntilAttacked(null);
			}

			if (getActiveChar().isStunned() && Rnd.get(10) == 0)
				getActiveChar().stopStunning(null);
		}
		else if (awake && getActiveChar().isPlayer())
		{
			if (getActiveChar().isSleeping())
				getActiveChar().stopSleeping(null);
		}

		final L2PcInstance player = getActiveChar().getPlayer();
		final L2PcInstance attackerPlayer = attacker.getPlayer();

		if (value > 0) // Reduce Hp if any
		{
			// add olympiad damage
			if ((getActiveChar().isPlayer() && attacker.isPlayer() || Config.ALT_OLY_INCLUDE_SUMMON_DAMAGE) && player != null && player.isInOlympiadMode() && attackerPlayer.getPlayer() != null && attackerPlayer.isInOlympiadMode())
				if (player.getOlympiadGameId() == attackerPlayer.getOlympiadGameId())
				{
					OlympiadGame game = Olympiad.getInstance().getOlympiadGames().get(player.getOlympiadGameId());
					game.addDamage(attackerPlayer, value);
				}

			// If we're dealing with an L2Attackable Instance and the attacker hit it with an over-hit enabled skill, set the over-hit values.
			// Anything else, clear the over-hit flag
			if (getActiveChar() instanceof L2Attackable)
			{
				if (((L2Attackable) getActiveChar()).isOverhit())
					((L2Attackable) getActiveChar()).setOverhitValues(attacker, value);
				else
					((L2Attackable) getActiveChar()).overhitEnabled(false);
			}
			value = getCurrentHp() - value; // Get diff of Hp vs value
			if (value <= 0)
			{
				// is the dying a duelist? if so, change his duel state to dead
				if (player != null && player.isInDuel() && getActiveChar().isPlayer()) // pets can die as usual
				{
					getActiveChar().disableAllSkills();
					stopHpMpRegeneration();
					attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					attacker.sendPacket(ActionFailed.STATIC_PACKET);
					
					// let the DuelManager know of his defeat
					DuelManager.getInstance().onPlayerDefeat(player);
					value = 1;
				}
				else
					value = 0; // Set value to 0 if Hp < 0
			}

			setCurrentHp(value); // Set Hp
		}
		else
		{
			// If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
			if (getActiveChar() instanceof L2Attackable)
				((L2Attackable) getActiveChar()).overhitEnabled(false);
		}

		if (getActiveChar().getStatus().getCurrentHp() < 1) // Die
		{
			if (player != null && player.isInOlympiadMode() && getActiveChar().isPlayer()) // pets can die as usual
			{
				stopHpMpRegeneration();
				player.setIsDead(true);
				player.setIsPendingRevive(true);
				if (player.getPet() != null)
					player.getPet().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
				return;
			}

			// Start the doDie process
			getActiveChar().doDie(attacker);

			if (player != null)
			{
				QuestState qs = player.getQuestState("255_Tutorial");
				if (qs != null)
					qs.getQuest().notifyEvent("CE30", null, player);
			}
		}
		else
		{
			// If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
			if (getActiveChar() instanceof L2Attackable)
				((L2Attackable) getActiveChar()).overhitEnabled(false);
		}

		return;
	}

	public void reduceMp(double value)
	{
		setCurrentMp(getCurrentMp() - value);
	}

	public final void reduceCp(int value)
	{
		setCurrentCp(getCurrentCp() - value);
	}

	private static final class RegenTaskManager extends AbstractIterativePeriodicTaskManager<CharStatus>
	{
		private static final RegenTaskManager _instance = new RegenTaskManager();
		
		private static RegenTaskManager getInstance()
		{
			return _instance;
		}
		
		private RegenTaskManager()
		{
			super(1000);
		}

		public boolean hasTask(CharStatus task)
		{
			return super.hasTask(task);
		}		
		@Override
		protected void callTask(CharStatus task)
		{
			task.regenTask();
		}
		
		@Override
		protected String getCalledMethodName()
		{
			return "regenTask()";
		}
	}

	private long _runTime = System.currentTimeMillis();

	public synchronized final void startHpMpRegeneration()
	{
		if (!getActiveChar().isDead() && !RegenTaskManager.getInstance().hasTask(this))
		{
			RegenTaskManager.getInstance().startTask(this);

			_runTime = System.currentTimeMillis();
		}
	}

	public synchronized final void stopHpMpRegeneration()
	{
		_flagsRegenActive = 0;

		RegenTaskManager.getInstance().stopTask(this);
	}

	public final void regenTask()
	{
		if (System.currentTimeMillis() < _runTime)
			return;
		
		_runTime += _period;
		
		CharStat cs = getActiveChar().getStat();
		
		if (getCurrentHp() == cs.getMaxHp() && getCurrentMp() == cs.getMaxMp() && getCurrentCp() == cs.getMaxCp())
		{
			stopHpMpRegeneration();
			return;
		}
		
		if (getCurrentHp() < cs.getMaxHp())
			setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()));
		
		if (getCurrentMp() < cs.getMaxMp())
			setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()));
		
		if (getCurrentCp() < cs.getMaxCp())
			setCurrentCp(getCurrentCp() + Formulas.calcCpRegen(getActiveChar()));
	}

}