package ru.catssoftware.gameserver.skills;

import ru.catssoftware.gameserver.model.L2Skill;

/**
 * Simple class containing all neccessary information to maintain
 * valid timestamps and reuse for skills upon relog. Filter this
 * carefully as it becomes redundant to store reuse for small delays.
 * @author  Yesod
 */
public class TimeStamp
{
	private final int id;
	private final int level;
	private long reuse;
	private long endTime;

	public TimeStamp(L2Skill skill, long _reuse)
	{
		id = skill.getId();
		level = skill.getLevel();
		reuse = _reuse;
		endTime = System.currentTimeMillis() + reuse;
	}


	public TimeStamp(int id, int level, long reuse, long endTime)
	{
		this.id = id;
		this.level = level;
		this.reuse = reuse;
		this.endTime = endTime;
	}

	public long getEndTime()
	{
		return endTime;
	}

	public int getSkillId()
	{
		return id;
	}

	public int getSkillLevel()
	{
		return level;
	}

	public long getReuse()
	{
		return reuse;
	}

	public long getRemaining()
	{
		return Math.max(endTime - System.currentTimeMillis(), 0);
	}

	/* Check if the reuse delay has passed and
	 * if it has not then update the stored reuse time
	 * according to what is currently remaining on
	 * the delay. */
	public boolean hasNotPassed()
	{
		return System.currentTimeMillis() < endTime;
	}
}
