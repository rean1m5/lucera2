package ru.catssoftware.gameserver.model.actor.instance;


import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.gameserver.network.serverpackets.Revive;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2SepulchersVictimInstance extends L2SepulcherMonsterInstance {

	public L2SepulchersVictimInstance(int objectID, L2NpcTemplate template) {
		super(objectID, template);
	}
	@Override
	public void onSpawn() {
		super.onSpawn();
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			public void run() {
				getKnownList().updateKnownObjects();
				for(L2Character ch : getKnownList().getKnownCharactersInRadius(1000) ) {
					if(ch instanceof L2SepulcherMonsterInstance)
						if(((L2SepulcherMonsterInstance)ch).getNpcId()==18170) {
							((L2SepulcherMonsterInstance)ch).addDamageHate(L2SepulchersVictimInstance.this, 1, 100);
							break;
						}
				}
			}
		},5000);
	}
	
	@Override 
	public void deleteMe() {
		if(!isDead()) {
			NpcSay say = new NpcSay(getObjectId(),1,getNpcId(),"Thanks you! Recive my blessing!");
			broadcastPacket(say);
			for(L2PcInstance pc : _mausoleum.getPlayersInside() ) {
				if(pc.isDead()) {
					pc.doRevive(100);
					pc.sendPacket(new Revive(pc));
				}
				pc.restoreHPMP();
				pc.resetSkillTime(true);
			}
		}
		super.deleteMe();
	}
	@Override
	public L2CharacterAI getAI() {
		if(_ai==null)
			_ai = new L2CharacterAI(new AIAccessor());
		return _ai;
	}
	

	@Override
	public boolean doDie(L2Character killer) {
		if(super.doDie(killer)) {
			_mausoleum.nextStage();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker) {
		if(attacker instanceof L2SepulcherMonsterInstance)
			if(((L2SepulcherMonsterInstance)attacker).getNpcId()==18170)
				return true;
		return false;
	}
	

}
