package ru.catssoftware.gameserver.ai;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Character.AIAccessor;
import ru.catssoftware.gameserver.model.actor.instance.L2BaiumAngelInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.catssoftware.tools.random.Rnd;

public class L2BaiumAngelAI extends L2AttackableAI {

	public L2BaiumAngelAI(AIAccessor accessor) {
		super(accessor);
	}
	@Override
	protected void thinkActive() {
		 
		if(Rnd.get(100)<30) {
			L2BaiumAngelInstance angel = (L2BaiumAngelInstance)_actor;
			L2GrandBossInstance boss = angel.getGrandBoss();
			if(boss!=null) {
					L2Skill sk = SkillTable.getInstance().getInfo(4201, 10);
					if(sk!=null) {
						if(_actor.getDistanceSq(boss)>sk.getCastRange() * sk.getCastRange()) {
							moveToPawn(boss, sk.getCastRange());
							return;
						}
						_actor.setTarget(boss);
						_actor.doCast(sk);
						return;
					}
				}
				
		}
		super.thinkActive();
	}

	@Override
	protected void thinkIdle() {
		thinkActive();
	}
}
