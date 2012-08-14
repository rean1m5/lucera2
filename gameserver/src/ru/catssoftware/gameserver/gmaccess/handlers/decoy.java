package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2Decoy;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class decoy extends gmHandler {

	@Override
	public String[] getCommandList() {
		// TODO Auto-generated method stub
		return new String[] {"decoy"};
	}

	@Override
	public void runCommand(L2PcInstance admin, String... params) {
     L2Decoy decoy = new L2Decoy(admin);
     decoy.spawnMe(admin.getX(),admin.getY(),admin.getZ());
		
	}

}
