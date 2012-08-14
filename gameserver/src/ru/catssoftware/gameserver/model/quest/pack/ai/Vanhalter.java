package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.instancemanager.grandbosses.VanHalterManager;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;

public class Vanhalter extends Quest
{
	private int ANDREAS_VAN_HALTER = 29062;
	private int ANDREAS_CAPTAIN    = 22188;
	private int TRIOLS[] = {32058,32059,32060,32061,32062,32063,32064,32065,32066};
	
	public Vanhalter()
	{
		super(-1,"vanhalter","ai");
		addAttackId(ANDREAS_VAN_HALTER);
		addKillId(ANDREAS_VAN_HALTER);
		addKillId(ANDREAS_CAPTAIN);
		for(int Triol : TRIOLS)
		  addKillId(Triol);
	}
	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
	    int npcId = npc.getNpcId();
	    if (npcId == ANDREAS_VAN_HALTER)
	    {
	      int maxHp = npc.getMaxHp();
	      double curHp = npc.getStatus().getCurrentHp();
	      if ((curHp / maxHp) * 100 <= 20)
	        VanHalterManager.getInstance().callRoyalGuardHelper();
	    }
	    return null;
	}
	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
	    int npcId = npc.getNpcId();
	    if (TRIOLS.equals(npcId))
	    {
	      VanHalterManager.getInstance().removeBleeding(npcId);
	      VanHalterManager.getInstance().checkTriolRevelationDestroy();
	    }
	    if (npcId == ANDREAS_CAPTAIN)
	      VanHalterManager.getInstance().checkRoyalGuardCaptainDestroy();
	    if (npcId == ANDREAS_VAN_HALTER)
	      VanHalterManager.getInstance().enterInterval();
	    return null;
	}
}