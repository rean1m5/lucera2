package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.instancemanager.grandbosses.FrintezzaManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2FrintezzaBossInstance extends L2RaidBossInstance {

	public L2FrintezzaBossInstance(int objectId, L2NpcTemplate template) {
		super(objectId, template);
	}
	
	@Override
	public boolean doDie(L2Character killer) {
		if(!super.doDie(killer))
			return false;
		FrintezzaManager.getInstance().onKill(this);
		return true;
	}
	@Override
	public void addDamageHate(L2Character attacker, int damage, int aggro, L2Skill skill) {
		if(getNpcId()==29045)
			return;
		
		super.addDamageHate(attacker, damage, aggro, skill);
		if(getNpcId()==29046) {
			if(getStatus().getCurrentHp() < getMaxHp()/4) 
				FrintezzaManager.getInstance().respawnScarlet();
		}
		if(getNpcId()==29046 || getNpcId()==29047)
			FrintezzaManager.getInstance().callAssist(attacker);
		
	}

}
