package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ai.L2BaiumAngelAI;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2BaiumAngelInstance extends L2MonsterInstance {

	public L2BaiumAngelInstance(int objectId, L2NpcTemplate template) {
		super(objectId, template);
	}
	private L2GrandBossInstance _boss;

	public void setBoss(L2GrandBossInstance boss) {
		_boss = boss;
	}

	@Override
	public L2GrandBossInstance getGrandBoss() {
		return _boss;
	}

	@Override
	public L2CharacterAI getAI() {
		if(_ai==null)
			_ai = new L2BaiumAngelAI(new AIAccessor());
		return _ai;
	}
	@Override
	public void returnHome() {
		
	}

}
