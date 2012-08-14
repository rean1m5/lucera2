package ru.catssoftware.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.status.SummonStatus;
import ru.catssoftware.gameserver.network.serverpackets.SetSummonRemainTime;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;


public class L2SummonInstance extends L2Summon
{
	private float				_expPenalty																				= 0;
	private int					_itemConsumeId, _itemConsumeCount, _itemConsumeSteps, _totalLifeTime, _timeLostIdle;
	private int					_timeRemaining, _timeLostActive, _nextItemConsumeTime, lastShowntimeRemaining;
	private static final int	SUMMON_LIFETIME_INTERVAL																= 1200000;
	private Future<?>			_summonConsumeTask;
	private static int			_lifeTime																				= SUMMON_LIFETIME_INTERVAL;

	public L2SummonInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
	{
		super(objectId, template, owner);
		setShowSummonAnimation(true);

		if (owner.getPet() != null && owner.getPet().getTemplate().getNpcId() == template.getNpcId())
			return;

		// defaults
		_itemConsumeId = 0;
		_itemConsumeCount = 0;
		_itemConsumeSteps = 0;
		_totalLifeTime = 1200000; // 20 minutes
		_timeLostIdle = 1000;
		_timeLostActive = 1000;

		if (skill != null)
		{
			_itemConsumeId = skill.getItemConsumeIdOT();
			_itemConsumeCount = skill.getItemConsumeOT();
			_itemConsumeSteps = skill.getItemConsumeSteps();
			_totalLifeTime = skill.getTotalLifeTime();
			_timeLostIdle = skill.getTimeLostIdle();
			_timeLostActive = skill.getTimeLostActive();
		}

		_timeRemaining = _totalLifeTime;
		lastShowntimeRemaining = _totalLifeTime;

		if (_itemConsumeId == 0)
			_nextItemConsumeTime = -1; // do not consume
		else if (_itemConsumeSteps == 0)
			_nextItemConsumeTime = -1; // do not consume
		else
			_nextItemConsumeTime = _totalLifeTime - _totalLifeTime / (_itemConsumeSteps + 1);

		// When no item consume is defined task only need to check when summon life time has ended.
		// Otherwise have to destroy items from owner's inventory in order to let summon live.
		int delay = 1000;
		_summonConsumeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new SummonConsume(getOwner(), this), delay, delay);
		getOwner().sendPacket(new SetSummonRemainTime(getTotalLifeTime(), getTotalLifeTime()));
	}

	@Override
	public final int getLevel()
	{
		return (getTemplate() != null ? getTemplate().getLevel() : 0);
	}

	@Override
	public int getSummonType()
	{
		return 1;
	}

	public void setExpPenalty(float expPenalty)
	{
		float ratePenalty = Config.ALT_GAME_SUMMON_PENALTY_RATE;
		_expPenalty = (expPenalty * ratePenalty);
	}

	public float getExpPenalty()
	{
		return _expPenalty;
	}

	public int getItemConsumeCount()
	{
		return _itemConsumeCount;
	}

	public int getItemConsumeId()
	{
		return _itemConsumeId;
	}

	public int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}

	public int getNextItemConsumeTime()
	{
		return _nextItemConsumeTime;
	}

	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}

	public int getTimeLostIdle()
	{
		return _timeLostIdle;
	}

	public int getTimeLostActive()
	{
		return _timeLostActive;
	}

	public int getTimeRemaining()
	{
		return _timeRemaining;
	}

	public void setNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime = value;
	}

	public void decNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime -= value;
	}

	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}

	public void addExpAndSp(int addToExp, int addToSp)
	{
		getOwner().addExpAndSp(addToExp, addToSp);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		if (_summonConsumeTask != null)
		{
			_summonConsumeTask.cancel(true);
			_summonConsumeTask = null;
		}

		return true;
	}

	static class SummonConsume implements Runnable
	{
		private L2PcInstance		_activeChar;
		private L2SummonInstance	_summon;
		
		SummonConsume(L2PcInstance activeChar, L2SummonInstance newpet)
		{
			_activeChar = activeChar;
			_summon = newpet;
		}

		public void run()
		{
			try
			{
				double oldTimeRemaining = _summon.getTimeRemaining();
				int maxTime = _summon.getTotalLifeTime();
				double newTimeRemaining;

				// if pet is attacking
				if (_summon.isAttackingNow())
					_summon.decTimeRemaining(_summon.getTimeLostActive());
				else
					_summon.decTimeRemaining(_summon.getTimeLostIdle());

				newTimeRemaining = _summon.getTimeRemaining();
				// check if the summon's lifetime has ran out
				if (newTimeRemaining < 0)
					_summon.unSummon(_activeChar);
				// check if it is time to consume another item
				else if ((newTimeRemaining <= _summon.getNextItemConsumeTime()) && (oldTimeRemaining > _summon.getNextItemConsumeTime()))
				{
					_summon.decNextItemConsumeTime(maxTime / (_summon.getItemConsumeSteps() + 1));

					// check if owner has enought itemConsume, if requested
					if (_summon.getItemConsumeCount() > 0 && _summon.getItemConsumeId() != 0 && !_summon.isDead() && !_summon.destroyItemByItemId("Consume", _summon.getItemConsumeId(), _summon.getItemConsumeCount(), _activeChar, true))
						_summon.unSummon(_activeChar);
				}

				// prevent useless packet-sending when the difference isn't visible.
				if ((_summon.lastShowntimeRemaining - newTimeRemaining) > maxTime / 352)
				{
					_summon.getOwner().sendPacket(new SetSummonRemainTime(maxTime, (int) newTimeRemaining));
					_summon.lastShowntimeRemaining = (int) newTimeRemaining;
				}
			}
			catch (Exception e)
			{
				_log.error("Error on player ["+_activeChar.getName()+"] summon item consume task.", e);
			}
		}
	}

	@Override
	public int getCurrentFed()
	{
		return _lifeTime;
	}

	@Override
	public int getMaxFed()
	{
		return SUMMON_LIFETIME_INTERVAL;
	}

	@Override
	public void unSummon(L2PcInstance owner)
	{
		if (_summonConsumeTask != null)
		{
			_summonConsumeTask.cancel(true);
			_summonConsumeTask = null;
		}
		super.unSummon(owner);
	}

	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}

	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}

	@Override
	public final SummonStatus getStatus()
	{
		if (_status == null)
			_status = new SummonStatus(this);

		return (SummonStatus) _status;
	}
}