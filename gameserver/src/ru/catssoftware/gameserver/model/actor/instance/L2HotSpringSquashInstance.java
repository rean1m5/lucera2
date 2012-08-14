package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.instancemanager.clanhallsiege.RainbowSpringSiege;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2HotSpringSquashInstance extends L2MonsterInstance
{
	public L2HotSpringSquashInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId,template);
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		RainbowSpringSiege.getInstance().onDieSquash(this);
		return true;
	}
	
	@Override
	public boolean canReduceHp(double damage, L2Character attacker)
	{
		return false;
	}	
}