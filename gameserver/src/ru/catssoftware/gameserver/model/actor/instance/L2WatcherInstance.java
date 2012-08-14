package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

public class L2WatcherInstance extends L2MonsterInstance
{
	public L2WatcherInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		if (getNpcId() == 18601)
			ThreadPoolManager.getInstance().scheduleGeneral(new Debuff(this),3000);
	}
	private class Debuff implements Runnable
	{
		private L2WatcherInstance _watcher;
		private int _skillsId[] =		{1064,1160,1170,1169,1164,1165,1167,1168}; 
		private int _skillsLvl[] =		{14,15,13,14,19,3,6,7};
		
		public Debuff(L2WatcherInstance par)
		{
			_watcher=par;
		}
    	public void run()
    	{
			for(L2Character ch : _watcher.getKnownList().getKnownCharactersInRadius(500))
			{
				if (ch instanceof L2PcInstance)
				{
					int skillRnd = Rnd.get(0,7);
					L2Skill skill = SkillTable.getInstance().getInfo(_skillsId[skillRnd],_skillsLvl[skillRnd]);
					if (skill != null)
						skill.getEffects(ch, ch);
				}
			}
    		ThreadPoolManager.getInstance().scheduleGeneral(new Debuff(_watcher),3000);
    	}		
	}
}