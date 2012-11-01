package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;

public class L2SepultureZone extends L2Zone {

	@Override
	public void onDieInside(L2Character character) {

	}

	@Override
	protected void onEnter(L2Character cha) {
		if(cha.isPlayer()) {
			L2PcInstance player = cha.getPlayer();
			if(player.getParty()==null && !player.isGM())
				player.teleToLocation(TeleportWhereType.Town);
			player._inSepulture = true;
		}
		
	}

	@Override
	protected void onExit(L2Character cha) {
		if(cha.isPlayer())
			((L2PcInstance)cha)._inSepulture = false;
	}

	@Override
	public void onReviveInside(L2Character character) {
		// TODO Auto-generated method stub

	}

}
