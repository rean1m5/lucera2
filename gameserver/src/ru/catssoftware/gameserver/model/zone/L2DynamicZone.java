package ru.catssoftware.gameserver.model.zone;

import java.util.concurrent.ScheduledFuture;

import org.w3c.dom.Node;

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.conditions.Condition;
import ru.catssoftware.gameserver.skills.conditions.ConditionParser;


public class L2DynamicZone extends L2DefaultZone
{
	private ScheduledFuture<?> _task;
	private Condition _cond;

	public L2DynamicZone()
	{
		super();
	}

	private boolean checkCondition(L2Character character)
	{
		if (_cond == null)
			return true;

		// Works with ConditionPlayer* and ConditionTarget* and some other
		Env env = new Env();
		env.player = character;
		env.target = character;

		return _cond.test(env);
	}

	// Called on movement
	@Override
	public void revalidateInZone(L2Character character)
	{
		if (_enabled && checkCondition(character) && isCorrectType(character) && isInsideZone(character))
		{
			if (!_characterList.containsKey(character.getObjectId()))
			{
				_characterList.put(character.getObjectId(), character);
				onEnter(character);
				// Timer turns on if characters are in zone
				if (_task == null)
					startZoneTask(character);
			}
		}
		else
		{
			if (_characterList.containsKey(character.getObjectId()))
			{
				_characterList.remove(character.getObjectId());
				onExit(character);
				// Timer turns off if zone is empty
				if (_characterList.size() == 0)
					stopZoneTask(character);
			}
		}
	}

	// Called by timer
	protected boolean revalidateCondition(L2Character character)
	{
		if (checkCondition(character))
		{
			if (!_characterList.containsKey(character.getObjectId()))
			{
				_characterList.put(character.getObjectId(), character);
				onEnter(character);
				// Timer turns on if characters are in zone
				if (_task == null)
					startZoneTask(character);
			}

			return true;
		}

		if (_characterList.containsKey(character.getObjectId()))
		{
			_characterList.remove(character.getObjectId());
			onExit(character);
			// Timer turns off if zone is empty
			if (_characterList.size() == 0)
				stopZoneTask(character);
		}
		return false;
	}

	private synchronized void startZoneTask(L2Character character)
	{
		if (_task == null)
			_task = ThreadPoolManager.getInstance().scheduleAtFixedRate(new ZoneTask(), 0, 3300);
	}

	private synchronized void stopZoneTask(L2Character character)
	{
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
		}
	}

	private class ZoneTask implements Runnable
	{
		public void run()
		{
			for (L2Character character : getCharactersInside().values())
			{
				if (revalidateCondition(character))
				{
					checkForDamage(character);
					if (_buffRepeat)
						checkForEffects(character);
				}
			}
		}
	}

	protected void checkForDamage(L2Character character)
	{
	}

	protected void checkForEffects(L2Character character)
	{
		if(_applyEnter != null)
		{
			for (L2Skill sk : _applyEnter)
			{
				if (character.getFirstEffect(sk.getId()) == null)
					sk.getEffects(character, character);
			}
		}
	}

	@Override
	protected void parseCondition(Node n) throws Exception
	{
		Condition cond = ConditionParser.getDefaultInstance().parseExistingCondition(n, null);
		Condition old = _cond;
		
		if (old != null)
			_log.fatal("Replaced " + old + " condition with " + cond + " condition at zone: " + this);
		_cond = cond;
	}
}