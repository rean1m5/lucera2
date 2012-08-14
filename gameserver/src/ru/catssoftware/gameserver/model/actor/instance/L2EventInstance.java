package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2EventInstance extends L2MonsterInstance
{
	private L2PcInstance _owner;
	private int _level;
	
	public L2EventInstance(int objectId, L2NpcTemplate template,L2PcInstance owner)
	{
		super(objectId, template);
		_owner = owner;
	}
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	@Override
	public final int getLevel()
	{
		return _level;
	}
	public void setLevel(int par)
	{
		_level=par;
	}
}