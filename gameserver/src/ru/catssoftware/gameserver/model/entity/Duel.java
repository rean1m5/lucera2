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
package ru.catssoftware.gameserver.model.entity;

import javolution.util.FastList;
import org.apache.log4j.Logger;
import ru.catssoftware.Message;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.DuelManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.model.base.Experience;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class Duel
{
	private final static Logger			_log					= Logger.getLogger(Duel.class.getName());

	public static final int				DUELSTATE_NODUEL		= 0;
	public static final int				DUELSTATE_DUELLING		= 1;
	public static final int				DUELSTATE_DEAD			= 2;
	public static final int				DUELSTATE_WINNER		= 3;
	public static final int				DUELSTATE_INTERRUPTED	= 4;

	private int							_duelId;
	private L2PcInstance				_playerA;
	private L2PcInstance				_playerB;
	private boolean						_partyDuel;
	private long						_duelEndTime;
	private int							_surrenderRequest		= 0;
	private int							_countdown				= 4;
	private boolean						_finished				= false;

	private ScheduledFuture<?>			_duelTask				= null;	
	private ScheduledFuture<?>			_duelEndTask			= null;
	
	private FastList<PlayerCondition>	_playerConditions;

	public static enum DuelResultEnum
	{
		Continue, Team1Win, Team2Win, Team1Surrender, Team2Surrender, Canceled, Timeout
	}

	public Duel(L2PcInstance playerA, L2PcInstance playerB, int partyDuel, int duelId)
	{
		_duelId = duelId;
		_playerA = playerA;
		_playerB = playerB;
		_partyDuel = partyDuel == 1;

		_duelEndTime = System.currentTimeMillis();
		if (_partyDuel)
			_duelEndTime += 300 * 1000;
		else
			_duelEndTime += 120 * 1000;

		_playerConditions = new FastList<PlayerCondition>();

		setFinished(false);

		if (_partyDuel)
		{
			// increase countdown so that start task can teleport players
			_countdown++;
			// inform players that they will be portet shortly
			SystemMessage sm = new SystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
		}
		// Schedule duel start
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartDuelTask(this), 3000);
	}

	public class PlayerCondition
	{
		private L2PcInstance		_player;
		private L2Summon			_summon;
		private double				_hp;
		private double				_mp;
		private double				_cp;
		private double				_hpS;
		private double				_mpS;
		private boolean				_paDuel;
		private int					_x, _y, _z;
		private Map<Integer,Integer>_summonEffects = new HashMap<Integer,Integer>();

		public PlayerCondition(L2PcInstance player, boolean partyDuel)
		{
			if (player == null)
				return;
			_player = player;
			_hp = _player.getStatus().getCurrentHp();
			_mp = _player.getStatus().getCurrentMp();
			_cp = _player.getStatus().getCurrentCp();
			_summon = player.getPet();
			if (_summon!=null)
			{
				_hpS = _summon.getStatus().getCurrentHp();
				_mpS = _summon.getStatus().getCurrentMp();
				for(L2Effect e : _summon.getAllEffects())
				{
					if (e!=null)
						_summonEffects.put(e.getSkill().getId(),e.getSkill().getLevel());						
				}
			}
			_player.store();
			_paDuel = partyDuel;

			if (_paDuel)
			{
				_x = _player.getX();
				_y = _player.getY();
				_z = _player.getZ();
			}
		}

		public void restoreCondition()
		{
			if (_player == null)
				return;
			_player.getStatus().setCurrentHpMp(_hp, _mp);
			_player.getStatus().setCurrentCp(_cp);

			L2Summon summon = _player.getPet(); 
			if (_summon!=summon)
			{
				if (_summon!=null)
				{
					if (summon==null)
					{
						if(!(_summon instanceof L2PetInstance))
						{
							L2SummonInstance newSummon = new L2SummonInstance(IdFactory.getInstance().getNextId(), _summon.getTemplate(), _player, null);
							newSummon.setName(_summon.getTemplate().getName());
							newSummon.setTitle(_player.getName());
							newSummon.setExpPenalty(((L2SummonInstance)_summon).getExpPenalty());
							if (newSummon.getLevel() >= Experience.LEVEL.length)
								newSummon.getStat().setExp(Experience.LEVEL[Experience.LEVEL.length - 1]);
							else
								newSummon.getStat().setExp(Experience.LEVEL[(newSummon.getLevel() % Experience.LEVEL.length)]);
							newSummon.getStatus().setCurrentHp(_hpS);
							newSummon.getStatus().setCurrentMp(_mpS);
							newSummon.setHeading(_player.getHeading());
							newSummon.setRunning();
							_player.setPet(newSummon);
							newSummon.stopAllEffects();
							if (_summonEffects != null)
							{
								for (int temp : _summonEffects.keySet())
								{
									L2Skill skill = SkillTable.getInstance().getInfo(temp, _summonEffects.get(temp));
									if (skill != null)
										skill.getEffects(newSummon, newSummon);
								}
							}
							L2World.getInstance().storeObject(newSummon);
							newSummon.spawnMe(_player.getX() + 50, _player.getY() + 100, _player.getZ());
						}
					}
				}
				else
				{
					if (summon instanceof L2PetInstance)
					{
						if (summon.isDead())
							summon.doRevive();
						summon.unSummon(_player);
					}
				}
				
			}
			else if (_summon!=null)
			{
				if (_summon.isDead() && _summon instanceof L2PetInstance)
				{
					_summon.doRevive();
					_summon.getStatus().setCurrentHpMp(_hpS, _mpS);
				}
				else
					_summon.getStatus().setCurrentHpMp(_hpS, _mpS);
				_summon.stopAllEffects();
				if (_summonEffects != null)
				{
					for (int temp : _summonEffects.keySet())
					{
						L2Skill skill = SkillTable.getInstance().getInfo(temp, _summonEffects.get(temp));
						if (skill != null)
							skill.getEffects(_summon, _summon);
					}
				}
			}
			
			if (_paDuel)
				teleportBack();
//			_player.stopAllEffects();
//			_player.restoreEffects();
			
		}

		public void teleportBack()
		{
			_player.teleToLocation(_x, _y, _z);
		}

		public L2PcInstance getPlayer()
		{
			return _player;
		}
	}

	public class ScheduleDuelTask implements Runnable
	{
		private Duel	_duel;

		public ScheduleDuelTask(Duel duel)
		{
			_duel = duel;
		}

		public void run()
		{
			try
			{
				DuelResultEnum status = _duel.checkEndDuelCondition();

				if (status == DuelResultEnum.Canceled)
				{
					// do not schedule duel end if it was interrupted
					setFinished(true);
					_duel.endDuel(status);
				}
				else if (status != DuelResultEnum.Continue)
				{
					setFinished(true);
					playKneelAnimation();
					_duelEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndDuelTask(_duel, status), 5000);
				}
				else
					_duelTask = ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public class ScheduleStartDuelTask implements Runnable
	{
		private Duel	_duel;

		public ScheduleStartDuelTask(Duel duel)
		{
			_duel = duel;
		}

		public void run()
		{
			try
			{
				// start/continue countdown
				int count = _duel.countdown();

				if (count == 4)
				{
					// players need to be teleportet first
					//TODO: stadia manager needs a function to return an unused stadium for duels currently only teleports to the same stadium
					_duel.teleportPlayers(149485, 46718, -3413);

					// give players 20 seconds to complete teleport and get ready (its ought to be 30 on offical..)
					ThreadPoolManager.getInstance().scheduleGeneral(this, 20000);
				}
				else if (count > 0) // duel not started yet - continue countdown
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
				else
					_duel.startDuel();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public class ScheduleEndDuelTask implements Runnable
	{
		private Duel			_duel;
		private DuelResultEnum	_result;

		public ScheduleEndDuelTask(Duel duel, DuelResultEnum result)
		{
			_duel = duel;
			_result = result;
		}

		public void run()
		{
			try
			{
				_duel.endDuel(_result);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stops all players from attacking.
	 * Used for duel timeout / interrupt.
	 */
	private void stopFighting()
	{
		if (_partyDuel)
		{
			for (L2PcInstance temp : _playerA.getParty().getPartyMembers())
				doFightStop(temp);
			for (L2PcInstance temp : _playerB.getParty().getPartyMembers())
				doFightStop(temp);
		}
		else
		{
			doFightStop(_playerA);
			doFightStop(_playerB);
		}
	}

	private void doFightStop(L2PcInstance temp)
	{
		if (temp.getPet()!=null)
		{
			temp.getPet().abortCast();
			temp.getPet().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
		}
		temp.abortCast();
		temp.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		temp.setTarget(null);
		temp.sendPacket(ActionFailed.STATIC_PACKET);
	}
	/**
	 * Check if a player engaged in pvp combat (only for 1on1 duels)
	 * @return returns true if a duelist is engaged in Pvp combat
	 */
	public boolean isDuelistInPvp(boolean sendMessage)
	{
		if (_partyDuel)
		{
			// Party duels take place in arenas - should be no other players there
			return false;
		}
		else if (_playerA.getPvpFlag() != 0 || _playerB.getPvpFlag() != 0)
		{
			if (sendMessage)
			{
				_playerA.sendMessage(Message.getMessage(_playerA, Message.MessageId.MSG_DUEL_CANCELED_DUE_PVP));
				_playerB.sendMessage(Message.getMessage(_playerB, Message.MessageId.MSG_DUEL_CANCELED_DUE_PVP));
			}
			return true;
		}
		return false;
	}

	/**
	 * Starts the duel
	 *
	 */
	public void startDuel()
	{
		// Save player Conditions
		savePlayerConditions();

		if (_playerA.isInDuel() || _playerB.isInDuel())
		{
			// clean up
			_playerConditions.clear();
			_playerConditions = null;
			DuelManager.getInstance().removeDuel(this);
			return;
		}

		if (_partyDuel)
		{
			// set isInDuel() state
			// cancel all active trades, just in case? xD
			for (L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(1);
				temp.broadcastUserInfo(true);
				broadcastToTeam2(new ExDuelUpdateUserInfo(temp));
			}
			for (L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(2);
				temp.broadcastUserInfo(true);
				broadcastToTeam1(new ExDuelUpdateUserInfo(temp));
			}

			// Send duel Start packets
			ExDuelReady ready = new ExDuelReady(1);
			ExDuelStart start = new ExDuelStart(1);

			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);
		}
		else
		{
			// set isInDuel() state
			_playerA.setIsInDuel(_duelId);
			_playerA.setTeam(1);
			_playerB.setIsInDuel(_duelId);
			_playerB.setTeam(2);

			// Send duel Start packets
			ExDuelReady ready = new ExDuelReady(0);
			ExDuelStart start = new ExDuelStart(0);

			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);
			
			broadcastToTeam1(new ExDuelUpdateUserInfo(_playerB));
			broadcastToTeam2(new ExDuelUpdateUserInfo(_playerA));
			_playerA.broadcastUserInfo(true);
			_playerB.broadcastUserInfo(true);
		}

		// play sound
		PlaySound ps = new PlaySound(1, "B04_S01", 0, 0, 0, 0, 0);
		broadcastToTeam1(ps);
		broadcastToTeam2(ps);

		// start duelling task
		_duelTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleDuelTask(this), 1000);
	}

	/**
	 * Save the current player condition: hp, mp, cp, location
	 *
	 */
	public void savePlayerConditions()
	{
		if (_partyDuel)
		{
			for (L2PcInstance temp : _playerA.getParty().getPartyMembers())
				_playerConditions.add(new PlayerCondition(temp, _partyDuel));
			for (L2PcInstance temp : _playerB.getParty().getPartyMembers())
				_playerConditions.add(new PlayerCondition(temp, _partyDuel));
		}
		else
		{
			_playerConditions.add(new PlayerCondition(_playerA, _partyDuel));
			_playerConditions.add(new PlayerCondition(_playerB, _partyDuel));
		}
	}

	/**
	 * Restore player conditions
	 * @param was the duel canceled?
	 */
	public void restorePlayerConditions(boolean abnormalDuelEnd)
	{
		// update isInDuel() state for all players
		if (_partyDuel)
		{
			for (L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(0);
				temp.broadcastUserInfo(true);
			}
			for (L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(0);
				temp.broadcastUserInfo(true);
			}
		}
		else
		{
			_playerA.setIsInDuel(0);
			_playerA.setTeam(0);
			_playerA.broadcastUserInfo(true);
			_playerB.setIsInDuel(0);
			_playerB.setTeam(0);
			_playerB.broadcastUserInfo(true);
		}

		// if it is an abnormal DuelEnd do not restore hp, mp, cp
		if (abnormalDuelEnd)
			return;

		// restore player conditions
		for (FastList.Node<PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;)
			e.getValue().restoreCondition();
	}

	public void restorePlayerConditions(L2PcInstance pl)
	{
		pl.setIsInDuel(0);
		pl.setTeam(0);
		pl.broadcastUserInfo(true);
		for (PlayerCondition e : _playerConditions)
			if (e.getPlayer()==pl)
			{
				e.restoreCondition();
				_playerConditions.remove(e);
			}
	}
	
	/**
	 * Get the duel id
	 * @return id
	 */
	public int getId()
	{
		return _duelId;
	}

	/**
	 * Returns the remaining time
	 * @return remaining time
	 */
	public int getRemainingTime()
	{
		return (int) (_duelEndTime - System.currentTimeMillis());
	}

	/**
	 * Get the player that requestet the duel
	 * @return duel requester
	 */
	public L2PcInstance getPlayerA()
	{
		return _playerA;
	}

	/**
	 * Get the player that was challenged
	 * @return challenged player
	 */
	public L2PcInstance getPlayerB()
	{
		return _playerB;
	}

	/**
	 * Returns whether this is a party duel or not
	 * @return is party duel
	 */
	public boolean isPartyDuel()
	{
		return _partyDuel;
	}

	public void setFinished(boolean mode)
	{
		_finished = mode;
	}

	public boolean getFinished()
	{
		return _finished;
	}

	/**
	 * teleport all players to the given coordinates
	 * @param x
	 * @param y
	 * @param z
	 */
	public void teleportPlayers(int x, int y, int z)
	{
		if (!_partyDuel)
			return;

		int offset = 0;

		for (L2PcInstance temp : _playerA.getParty().getPartyMembers())
		{
			temp.teleToLocation(x + offset - 180, y - 150, z);
			offset += 40;
		}
		offset = 0;
		for (L2PcInstance temp : _playerB.getParty().getPartyMembers())
		{
			temp.teleToLocation(x + offset - 180, y + 150, z);
			offset += 40;
		}
	}

	/**
	 * Broadcast a packet to the challanger team
	 *
	 */
	public void broadcastToTeam1(L2GameServerPacket packet)
	{
		if (_playerA == null)
			return;

		if (_partyDuel && _playerA.getParty() != null)
		{
			for (L2PcInstance temp : _playerA.getParty().getPartyMembers())
				temp.sendPacket(packet);
		}
		else
			_playerA.sendPacket(packet);
	}

	/**
	 * Broadcast a packet to the challenged team
	 *
	 */
	public void broadcastToTeam2(L2GameServerPacket packet)
	{
		if (_playerB == null)
			return;

		if (_partyDuel && _playerB.getParty() != null)
		{
			for (L2PcInstance temp : _playerB.getParty().getPartyMembers())
				temp.sendPacket(packet);
		}
		else
			_playerB.sendPacket(packet);
	}

	/**
	 * Get the duel winner
	 * @return winner
	 */
	public L2PcInstance getWinner()
	{
		if (!getFinished() || _playerA == null || _playerB == null)
			return null;
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
			return _playerA;
		if (_playerB.getDuelState() == DUELSTATE_WINNER)
			return _playerB;
		return null;
	}

	/**
	 * Get the duel looser
	 * @return looser
	 */
	public L2PcInstance getLooser()
	{
		if (!getFinished() || _playerA == null || _playerB == null)
			return null;
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
			return _playerB;
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
			return _playerA;
		return null;
	}

	/**
	 * Playback the bow animation for all loosers
	 *
	 */
	public void playKneelAnimation()
	{
		L2PcInstance looser = getLooser();

		if (looser == null)
			return;

		if (_partyDuel && looser.getParty() != null)
		{
			for (L2PcInstance temp : looser.getParty().getPartyMembers())
				temp.broadcastPacket(new SocialAction(temp.getObjectId(), 7));
		}
		else
			looser.broadcastPacket(new SocialAction(looser.getObjectId(), 7));
	}

	/**
	 * Do the countdown and send message to players if necessary
	 * @return current count
	 */
	public int countdown()
	{
		_countdown--;

		if (_countdown > 3)
			return _countdown;

		// Broadcast countdown to duelists
		SystemMessage sm = null;
		if (_countdown > 0)
		{
			sm = new SystemMessage(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS);
			sm.addNumber(_countdown);
		}
		else
			sm = new SystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN);

		broadcastToTeam1(sm);
		broadcastToTeam2(sm);

		return _countdown;
	}

	/**
	 * The duel has reached a state in which it can no longer continue
	 * @param duel result
	 */
	public void endDuel(DuelResultEnum result)
	{
		if (_playerA == null || _playerB == null)
		{
			//clean up
			_playerConditions.clear();
			_playerConditions = null;
			DuelManager.getInstance().removeDuel(this);
			if(_duelTask!=null)
				_duelTask.cancel(false);
			if(_duelEndTask!=null)
				_duelEndTask.cancel(false);
			return;
		}
		if(_duelTask!=null)
			_duelTask.cancel(false);
		if(_duelEndTask!=null)
			_duelEndTask.cancel(false);

		// inform players of the result
		SystemMessage sm = null;
		switch (result)
		{
		case Team1Win:
			restorePlayerConditions(false);
			// send SystemMessage
			if (_partyDuel)
				sm = new SystemMessage(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL);
			else
				sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL);
			sm.addString(_playerA.getName());

			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
			break;
		case Team2Win:
			restorePlayerConditions(false);
			// send SystemMessage
			if (_partyDuel)
				sm = new SystemMessage(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL);
			else
				sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL);
			sm.addString(_playerB.getName());

			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
			break;
		case Team1Surrender:
			restorePlayerConditions(false);
			// send SystemMessage
			if (_partyDuel)
				sm = new SystemMessage(SystemMessageId.SINCE_S1S_PARTY_WITHDREW_FROM_THE_DUEL_S1S_PARTY_HAS_WON);
			else
				sm = new SystemMessage(SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON);
			sm.addString(_playerA.getName());
			sm.addString(_playerB.getName());

			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
			break;
		case Team2Surrender:
			restorePlayerConditions(false);
			// send SystemMessage
			if (_partyDuel)
				sm = new SystemMessage(SystemMessageId.SINCE_S1S_PARTY_WITHDREW_FROM_THE_DUEL_S1S_PARTY_HAS_WON);
			else
				sm = new SystemMessage(SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON);
			sm.addString(_playerB.getName());
			sm.addString(_playerA.getName());

			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
			break;
		case Canceled:
			stopFighting();
			// dont restore hp, mp, cp
			restorePlayerConditions(true);
			// send SystemMessage
			sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);

			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
			break;
		case Timeout:
			stopFighting();
			// hp,mp,cp seem to be restored in a timeout too...
			restorePlayerConditions(false);
			// send SystemMessage
			sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);

			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
			break;
		}
		GameExtensionManager.getInstance().handleAction(this, Action.DUEL_FINISH, _playerA,_playerB,result,_partyDuel);
		// Send end duel packet
		ExDuelEnd duelEnd = null;
		if (_partyDuel)
			duelEnd = new ExDuelEnd(1);
		else
			duelEnd = new ExDuelEnd(0);

		broadcastToTeam1(duelEnd);
		broadcastToTeam2(duelEnd);

		//clean up
		_playerConditions.clear();
		_playerConditions = null;
		DuelManager.getInstance().removeDuel(this);
	}

	/**
	 * Did a situation occur in which the duel has to be ended?
	 * @return DuelResultEnum duel status
	 */
	public DuelResultEnum checkEndDuelCondition()
	{
		// one of the players might leave during duel
		if (_playerA == null || _playerB == null)
			return DuelResultEnum.Canceled;

		// got a duel surrender request?
		if (_surrenderRequest != 0)
		{
			if (_surrenderRequest == 1)
				return DuelResultEnum.Team1Surrender;

			return DuelResultEnum.Team2Surrender;
		}
		// duel timed out
		else if (getRemainingTime() <= 0)
			return DuelResultEnum.Timeout;
		// Has a player been declared winner yet?
		else if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResultEnum.Team1Win;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResultEnum.Team2Win;
		}

		// More end duel conditions for 1on1 duels
		else if (!_partyDuel)
		{
			// Duel was interrupted e.g.: player was attacked by mobs / other players
			if (_playerA.getDuelState() == DUELSTATE_INTERRUPTED || _playerB.getDuelState() == DUELSTATE_INTERRUPTED)
				return DuelResultEnum.Canceled;

			// Are the players too far apart?
			if (!_playerA.isInsideRadius(_playerB, 1600, false, false))
				return DuelResultEnum.Canceled;

			// Did one of the players engage in PvP combat?
			if (isDuelistInPvp(true))
				return DuelResultEnum.Canceled;

			// is one of the players in a Siege, Peace or PvP zone?
			SiegeManager tmpSM = SiegeManager.getInstance();
			if (_playerA.isInsideZone(L2Zone.FLAG_PEACE) || _playerB.isInsideZone(L2Zone.FLAG_PEACE) || tmpSM.checkIfInZone(_playerA) || tmpSM.checkIfInZone(_playerB) || _playerA.isInsideZone(L2Zone.FLAG_PVP) || _playerB.isInsideZone(L2Zone.FLAG_PVP))
				return DuelResultEnum.Canceled;
		}

		return DuelResultEnum.Continue;
	}

	/**
	 * Register a surrender request
	 * @param surrendering player
	 */
	public void doSurrender(L2PcInstance player)
	{
		// already recived a surrender request
		if (_surrenderRequest != 0)
			return;

		// stop the fight
		stopFighting();

		if (_partyDuel)
		{
			if (_playerA.getParty().getPartyMembers().contains(player))
			{
				_surrenderRequest = 1;
				for (L2PcInstance temp : _playerA.getParty().getPartyMembers())
					temp.setDuelState(DUELSTATE_DEAD);
				for (L2PcInstance temp : _playerB.getParty().getPartyMembers())
					temp.setDuelState(DUELSTATE_WINNER);
			}
			else if (_playerB.getParty().getPartyMembers().contains(player))
			{
				_surrenderRequest = 2;
				for (L2PcInstance temp : _playerB.getParty().getPartyMembers())
					temp.setDuelState(DUELSTATE_DEAD);
				for (L2PcInstance temp : _playerA.getParty().getPartyMembers())
					temp.setDuelState(DUELSTATE_WINNER);
			}
		}
		else
		{
			if (player == _playerA)
			{
				_surrenderRequest = 1;
				_playerA.setDuelState(DUELSTATE_DEAD);
				_playerB.setDuelState(DUELSTATE_WINNER);
			}
			else if (player == _playerB)
			{
				_surrenderRequest = 2;
				_playerB.setDuelState(DUELSTATE_DEAD);
				_playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}

	/**
	 * This function is called whenever a player was defeated in a duel
	 * @param dieing player
	 */
	public void onPlayerDefeat(L2PcInstance player)
	{
		// Set player as defeated
		player.setDuelState(DUELSTATE_DEAD);

		if (_partyDuel)
		{
			boolean teamdefeated = true;
			for (L2PcInstance temp : player.getParty().getPartyMembers())
			{
				if (temp.getDuelState() == DUELSTATE_DUELLING)
				{
					teamdefeated = false;
					break;
				}
			}

			if (teamdefeated)
			{
				L2PcInstance winner = _playerA;
				if (_playerA.getParty().getPartyMembers().contains(player))
					winner = _playerB;

				for (L2PcInstance temp : winner.getParty().getPartyMembers())
					temp.setDuelState(DUELSTATE_WINNER);
			}
		}
		else
		{
			if (player != _playerA && player != _playerB)
				_log.warn("Error in onPlayerDefeat(): player is not part of this 1vs1 duel");

			if (_playerA == player)
				_playerB.setDuelState(DUELSTATE_WINNER);
			else
				_playerA.setDuelState(DUELSTATE_WINNER);
		}
	}

	/**
	 * This function is called whenever a player leaves a party
	 * @param leaving player
	 */
	public void onRemoveFromParty(L2PcInstance player)
	{
		// if it isnt a party duel ignore this
		if (!_partyDuel)
			return;

		// this player is leaving his party during party duel
		// if hes either playerA or playerB cancel the duel and port the players back
		if (player == _playerA || player == _playerB)
		{
			for (FastList.Node<PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;)
			{
				restorePlayerConditions(player);				
			}

			_playerA = null;
			_playerB = null;
		}
		else
		// teleport the player back & delete his PlayerCondition record
		{
			for (FastList.Node<PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;)
			{
				if (e.getValue().getPlayer() == player)
				{
					restorePlayerConditions(player);
					break;
				}
			}
		}
	}

	public static boolean isInvul(L2Character targetChar, L2Character attackerChar)
	{
		final L2PcInstance attacker = attackerChar.getPlayer();
		final L2PcInstance target = targetChar.getPlayer();

		if (attacker == null && target == null)
			return false;

		final boolean attackerIsInDuel = attacker != null && attacker.isInDuel();
		final boolean targetIsInDuel = target != null && target.isInDuel();

		if (!attackerIsInDuel && !targetIsInDuel)
			return false;

		if (attackerIsInDuel)
		{
			if (attacker.getDuelState() == Duel.DUELSTATE_DEAD || attacker.getDuelState() == Duel.DUELSTATE_WINNER)
				return true;
		}

		if (targetIsInDuel)
		{
			if (target.getDuelState() == Duel.DUELSTATE_DEAD || target.getDuelState() == Duel.DUELSTATE_WINNER)
				return true;
		}

		if (attackerIsInDuel && targetIsInDuel && attacker.getDuelId() == target.getDuelId())
			return false;

		if (attackerIsInDuel)
			attacker.setDuelState(Duel.DUELSTATE_INTERRUPTED);

		if (targetIsInDuel)
			target.setDuelState(Duel.DUELSTATE_INTERRUPTED);

		return false;
	}
}
