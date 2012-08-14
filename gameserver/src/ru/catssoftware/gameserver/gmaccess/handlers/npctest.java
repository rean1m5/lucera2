package ru.catssoftware.gameserver.gmaccess.handlers;

import javolution.util.FastList;
import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class npctest extends gmHandler {

	@Override
	public String[] getCommandList() {
		// TODO Auto-generated method stub
		return new String[] {"npctest"};
	}

	@Override
	public void runCommand(L2PcInstance admin, String... params) {
		FastList<Integer> npc = new FastList<Integer>();
		for(L2Spawn spawn : SpawnTable.getInstance().getSpawnTable().values())
			if(spawn.getLastSpawn()!=null && spawn.getLastSpawn() instanceof L2NpcInstance && !(spawn.getLastSpawn() instanceof L2Attackable)) 
				if(!npc.contains(spawn.getNpcid())) {
					spawn.getLastSpawn().showChatWindow(admin);
					npc.add(spawn.getNpcid());
				}

	}

}
